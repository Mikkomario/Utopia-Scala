package utopia.vault.coder.controller.reader

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.datastructure.immutable.{Model, ModelValidationFailedException}
import utopia.flow.generic.DataTypeException
import utopia.flow.util.{UncertainBoolean, Version}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, CombinationData, DbPropertyOverrides, Enum, Instance, Name, NamingRules, ProjectData, Property}
import utopia.vault.coder.model.datatype.BasicPropertyType.IntNumber
import utopia.vault.coder.model.datatype.{CustomPropertyType, PropertyType}
import utopia.vault.coder.model.enumeration.CombinationType.{Combined, MultiCombined, PossiblyCombined}
import utopia.vault.coder.model.enumeration.IntSize.Default
import utopia.vault.coder.model.datatype.PropertyType.{ClassReference, EnumValue, Text}
import utopia.vault.coder.model.enumeration.{CombinationType, NamingConvention}
import utopia.vault.coder.model.scala.Package
import utopia.vault.coder.model.scala.code.CodePiece

import java.nio.file.Path
import scala.util.{Failure, Success}

/**
  * Used for reading class data from a .json file
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
object ClassReader
{
	// OTHER    -----------------------------------
	
	/**
	  * Reads class data from a .json file
	  * @param path Path to the file to read
	  * @return Read project data. Failure if file reading or class parsing failed.
	  */
	def apply(path: Path) = JsonBunny(path).flatMap { v =>
		val root = v.getModel
		
		// Parses custom data types (must succeed)
		root("data_types", "types").getModel.attributes.tryMap { c =>
			c.value.model
				.toTry { DataTypeException(
					s"Custom data types must be presented as json objects. '$v' is not a model.") }
				.flatMap(CustomPropertyType.apply)
				.map { c.name.toLowerCase -> _ }
		}.flatMap { customTypes =>
			val customTypesMap = customTypes.toMap
			val author = root("author").getString
			val basePackage = Package(root("base_package", "package").getString)
			val modelPackage = root("model_package", "package_model").string match {
				case Some(pack) => Package(pack)
				case None => basePackage / "model"
			}
			val dbPackage = root("db_package", "database_package", "package_db", "package_database").string match {
				case Some(pack) => Package(pack)
				case None => basePackage / "database"
			}
			val projectName = root("name", "project").stringOr {
				basePackage.parts.lastOption match {
					case Some(lastPart) => lastPart.capitalize
					case None =>
						dbPackage.parent.parts.lastOption
							.orElse { dbPackage.parts.lastOption }
							.getOrElse { "Project" }
							.capitalize
				}
			}
			
			implicit val namingRules: NamingRules = NamingRules(root("naming").getModel).value
			
			val databaseName = root("database_name", "database", "db_name", "db").string
				.map { rawDbName =>
					namingRules.database.convert(rawDbName,
						NamingConvention.of(rawDbName, namingRules.database))
				}
				.filter { _.nonEmpty }
			val enumPackage = modelPackage / "enumeration"
			val enumerations = root("enumerations", "enums").getModel.attributes.map { enumAtt =>
				Enum(enumAtt.name.capitalize, enumAtt.value.getVector.flatMap { _.string }.map { _.capitalize },
					enumPackage, author)
			}
			val referencedEnumerations = root("referenced_enums", "referenced_enumerations").getVector
				.flatMap { _.string }
				.map { enumPath =>
					val (packagePart, enumName) = enumPath.splitAtLast(".")
					Enum(enumName, Vector(), packagePart)
				}
			val allEnumerations = enumerations ++ referencedEnumerations
			
			val classes = root("classes", "class").getModel.attributes.tryMap { packageAtt =>
				packageAtt.value.model match {
					case Some(classModel) =>
						classFrom(classModel, packageAtt.name, allEnumerations, customTypesMap, author).map { Vector(_) }
					case None =>
						packageAtt.value.getVector.flatMap { _.model }
							.tryMap { classFrom(_, packageAtt.name, allEnumerations, customTypesMap, author) }
				}
			}.map { _.flatten }
			
			classes.map { classData =>
				val classes = classData.map { _._1 }
				// Processes the proposed combinations
				val combinations = classData.flatMap { case (parentClass, combos, _) =>
					combos.flatMap { combo =>
						// Finds the child class (child name match)
						classes.find { c => c.name.variants.exists { _ ~== combo.childName } }.map { childClass =>
							// Determines the combination type
							val combinationType = combo.comboTypeName.notEmpty.flatMap(CombinationType.interpret)
								.getOrElse {
									if (combo.childrenDefinedAsPlural)
										MultiCombined
									else if (!(combo.childName ~== childClass.name.singular) &&
										(combo.childName ~== childClass.name.plural))
										MultiCombined
									else if (combo.alwaysLinked.isTrue)
										Combined
									else
										PossiblyCombined
								}
							// Determines combination name
							val childAlias = combo.childAlias.notEmpty.map { alias =>
								if (combinationType.isOneToMany)
									Name(alias, alias, NamingConvention.of(alias, namingRules.className))
								else
									Name.interpret(alias, namingRules.className)
							}
							val parentAlias = combo.parentAlias.notEmpty
								.map { alias => Name.interpret(alias, namingRules.className) }
							val comboName = combo.name.notEmpty match {
								case Some(n) => Name.interpret(n, namingRules.className)
								case None =>
									val childPart = childAlias.getOrElse(childClass.name)
									val base = parentClass.name + "with"
									if (combinationType.isOneToMany)
										base + childPart.plural
									else
										base + childPart
							}
							val isAlwaysLinked = combo.alwaysLinked.getOrElse {
								combinationType match {
									case Combined => true
									case _ => false
								}
							}
							CombinationData(combinationType, comboName, parentClass, childClass, parentAlias,
								childAlias, combo.doc, isAlwaysLinked)
						}
					}
				}
				// Returns class instances, also
				val instances = classData.flatMap { _._3 }
				ProjectData(projectName, modelPackage, dbPackage, databaseName, enumerations, classes, combinations,
					instances, namingRules, root("version").string.map { Version(_) },
					!root("models_without_vault").getBoolean, root("prefix_columns").getBoolean)
			}
		}
	}
	
	private def classFrom(classModel: Model, packageName: String, enumerations: Iterable[Enum],
	                      customTypes: Map[String, PropertyType], defaultAuthor: String)
	                     (implicit naming: NamingRules) =
	{
		val rawClassName = classModel("name").string.filter { _.nonEmpty }.map { Name.interpret(_, naming.className) }
		val tableName = classModel("table_name", "table").string.filter { _.nonEmpty }
			.map { Name.interpret(_, naming.table) }
		
		if (rawClassName.isEmpty && tableName.isEmpty)
			Failure(new ModelValidationFailedException("'name', 'table_name' or 'table' is required in a class model"))
		else {
			// Determines class name
			val className = rawClassName.getOrElse { tableName.get }
			val fullName = classModel("name_plural", "plural_name").string match {
				case Some(plural) => className.copy(plural = plural)
				case None => className
			}
			
			// Reads properties
			val properties = classModel("properties", "props").getVector.flatMap { _.model }
				.map { propertyFrom(_, enumerations, fullName, customTypes) }
			val idName = classModel("id").string.map { Name.interpret(_, naming.classProp) }
			
			// Finds the combo indices
			// The indices in the document are given as property names, but here they are converted to column names
			val comboIndexColumnNames: Vector[Vector[String]] = classModel("index", "combo_index")
				.vector.map[Vector[Vector[Name]]] { v =>
				Vector(v.flatMap { _.string }.map { s => Name.interpret(s, naming.classProp) })
			}
				.orElse {
					classModel("indices", "combo_indices").vector
						.map { vectors =>
							vectors.map[Vector[Name]] { vector =>
								vector.getVector.flatMap { _.string }.map { s => Name.interpret(s, naming.classProp) }
							}
						}
				}
				.getOrElse(Vector())
				.map { combo =>
					combo.flatMap { propName =>
						properties.view.flatMap { _.dbProperties }.find { _.name ~== propName }.map { _.columnName }
					}
				}
				.filter { _.nonEmpty }
			
			// Checks whether descriptions are supported for this class
			val descriptionLinkColumnName: Option[Name] =
				classModel("description_link", "desc_link", "description_link_column")
					.string match {
					case Some(n) => Some(Name.interpret(n, naming.classProp))
					case None =>
						if (classModel("described", "is_described").getBoolean)
							Some(className + "id")
						else
							None
				}
			
			// Reads combination-related information
			val comboInfo = (classModel("combination", "combo").model match {
				case Some(comboModel) => Vector(comboModel)
				case None => classModel("combinations", "combos").getVector.flatMap { _.model }
			}).flatMap { comboModel =>
				comboModel("child", "children").string.map { childName =>
					RawCombinationData(childName, comboModel("parent_alias", "alias_parent").getString,
						comboModel("child_alias", "alias_child").getString, comboModel("type").getString,
						comboModel("name").getString.capitalize, comboModel("name_plural").getString.capitalize,
						comboModel("doc").getString, comboModel("always_linked", "is_always_linked").boolean,
						comboModel.containsNonEmpty("children"))
				}
			}
			
			val readClass = new Class(fullName, tableName.map { _.tableName }, idName.getOrElse(Class.defaultIdName),
				properties, packageName, comboIndexColumnNames, descriptionLinkColumnName,
				classModel("doc").getString, classModel("author").stringOr(defaultAuthor),
				classModel("use_long_id").getBoolean,
				// Writes generic access point if this class has combinations, or if explicitly specified
				classModel("has_combos", "generic_access", "tree_inheritance").booleanOr(comboInfo.nonEmpty))
			
			// Also parses class instances if they are present
			val instances = classModel("instance").model match {
				case Some(instanceModel) => Vector(instanceFrom(instanceModel, readClass))
				case None => classModel("instances").getVector.flatMap { _.model }.map { instanceFrom(_, readClass) }
			}
			
			Success((readClass, comboInfo, instances))
		}
	}
	
	private def propertyFrom(propModel: Model, enumerations: Iterable[Enum], className: Name,
	                         customTypes: Map[String, PropertyType])
	                        (implicit naming: NamingRules) =
	{
		val rawName = propModel("name").string.filter { _.nonEmpty }
		val rawColumnName = propModel("column_name", "column", "col").string.filter { _.nonEmpty }
		
		// val name = propModel("name").getString
		// val columnName = propModel("column_name").stringOr(NamingUtils.camelToUnderscore(name))
		val tableReference = propModel("references", "ref").string.map { ref =>
			val (tablePart, columnPart) = ref.splitAtFirst("(")
			tablePart -> columnPart.untilLast(")").notEmpty.map { Name.interpret(_, naming.column) }
		}
		val length = propModel("length", "len").int
		val baseDataType = propModel("type").string.flatMap { typeName =>
			val lowerTypeName = typeName.toLowerCase
			val innermostTypeName = lowerTypeName.afterLast("[").notEmpty match {
				case Some(afterBracket) => afterBracket.untilFirst("]")
				case None => lowerTypeName
			}
			// Checks for a custom data type
			customTypes.get(innermostTypeName) match {
				case Some(customType) =>
					Some(if (lowerTypeName.untilFirst("[").contains("option")) customType.optional else customType)
				case None =>
					// Checks for an enumeration reference
					val enumType = {
						if (lowerTypeName.contains("enum")) {
							val enumName = lowerTypeName.afterFirst("enum")
								.afterFirst("[").untilFirst("]")
							enumerations.find { _.name.toLowerCase == enumName }
						}
						else
							None
					}
					enumType match {
						// Case: Enumeration reference
						case Some(enumType) =>
							val baseType = EnumValue(enumType)
							Some(if (lowerTypeName.contains("option")) baseType.optional else baseType)
						// Case: Standard data type
						case None => PropertyType.interpret(typeName, length, rawName)
					}
			}
		}
		// Applies the possible table reference
		val actualDataType = tableReference match {
			// Case: Reference type
			case Some((tableName, columnName)) =>
				ClassReference(tableName, columnName.getOrElse(Class.defaultIdName), baseDataType.getOrElse(IntNumber()))
			// Case: Other type
			case None =>
				baseDataType.getOrElse {
					// The default type is string for properties with a specified maximum length and int for others
					length match {
						case Some(length) => Text(length)
						case None => IntNumber(Default)
					}
				}
		}
		
		val columnName = rawColumnName.map { Name.interpret(_, naming.column) }
		val name: Name = rawName.map { Name.interpret(_, naming.classProp) }
			.orElse { columnName }
			.getOrElse { actualDataType.defaultPropertyName }
		val fullName = propModel("name_plural", "plural_name").string match {
			case Some(pluralName) => name.copy(plural = pluralName)
			case None => name
		}
		
		val rawDoc = propModel("doc").string.filter { _.nonEmpty }
		val doc = rawDoc.getOrElse { actualDataType.writeDefaultDescription(className, fullName) }
		
		val default: CodePiece = propModel("default", "def")
		// A property either lists database-interaction information within itself (single-column use-case) or
		// within a special property named "parts" (multi-column use-case)
		val partModels = propModel("parts").getVector.flatMap { _.model }
		val dbPropertyOverrides = {
			if (partModels.isEmpty && actualDataType.isSingleColumn)
				Vector(dbPropertyOverridesFrom(propModel, default))
			else
				partModels.map { dbPropertyOverridesFrom(_, readName = true) }
		}
		
		Property(fullName, actualDataType, default, dbPropertyOverrides, doc)
	}
	
	private def dbPropertyOverridesFrom(model: Model, default: CodePiece = CodePiece.empty, readName: Boolean = false)
	                                   (implicit naming: NamingRules) =
	{
		// Reads the custom column name
		val rawColumnName = model("column_name", "column", "col").string
		val columnName = rawColumnName.map { Name.interpret(_, naming.column) }
		val finalColumnName = columnName match {
			case Some(name) => name.columnName
			case None => ""
		}
		
		// Name-processing may be skipped
		val name = {
			if (readName) {
				val rawName = model("name").string
				val name = rawName.map { Name.interpret(_, naming.classProp) }.orElse { columnName }
				name.map { name =>
					model("name_plural", "plural_name").string match {
						case Some(pluralName) => name.copy(plural = pluralName)
						case None => name
					}
				}
			}
			else
				None
		}
		
		// A custom default value may be carried over to sql under some circumstances
		val sqlDefault = model("sql_default", "sql_def").stringOr { default.toSql.getOrElse("") }
		
		val rawLimit = model("length_rule", "length_limit", "limit", "max_length", "length_max", "max")
		val limit = rawLimit.int match {
			case Some(max) => s"to $max"
			case None => rawLimit.getString
		}
		
		DbPropertyOverrides(name, finalColumnName, sqlDefault, limit, model("indexed", "index", "is_index").boolean)
	}
	
	private def instanceFrom(model: Model, parentClass: Class)(implicit naming: NamingRules) =
	{
		// Matches model properties against class properties
		val properties = model.attributesWithValue.flatMap { att =>
			val attName = Name.interpret(att.name, naming.classProp)
			parentClass.properties.find { _.name ~== attName }
				.map { _ -> att.value }
		}.toMap
		Instance(parentClass, properties, model("id"))
	}
	
	
	// NESTED   --------------------------------------
	
	private case class RawCombinationData(childName: String, parentAlias: String, childAlias: String,
	                                      comboTypeName: String, name: String, namePlural: String, doc: String,
	                                      alwaysLinked: UncertainBoolean,
	                                      childrenDefinedAsPlural: Boolean)
}
