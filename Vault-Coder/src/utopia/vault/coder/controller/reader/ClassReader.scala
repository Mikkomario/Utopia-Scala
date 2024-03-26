package utopia.vault.coder.controller.reader

import utopia.bunnymunch.jawn.JsonBunny
import utopia.coder.controller.parsing.file.InputFiles
import utopia.coder.model.data
import utopia.coder.model.data.{Name, NamingRules}
import utopia.flow.error.DataTypeException
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType.{ModelType, StringType, VectorType}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.util.{UncertainBoolean, Version}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, CombinationData, DbPropertyOverrides, Enum, EnumerationValue, Instance, ProjectData, Property}
import utopia.vault.coder.model.datatype.StandardPropertyType.BasicPropertyType.IntNumber
import utopia.vault.coder.model.datatype.{CustomPropertyType, PropertyType, StandardPropertyType}
import utopia.vault.coder.model.enumeration.CombinationType.{Combined, MultiCombined, PossiblyCombined}
import utopia.vault.coder.model.enumeration.IntSize.Default
import utopia.vault.coder.model.datatype.StandardPropertyType.{ClassReference, EnumValue, Text}
import utopia.coder.model.enumeration.NameContext.{ClassName, ClassPropName, ColumnName, DatabaseName, EnumName, EnumValueName, Header, TableName}
import utopia.coder.model.enumeration.NamingConvention
import utopia.coder.model.enumeration.NamingConvention.{CamelCase, UnderScore}
import utopia.vault.coder.model.enumeration.{CombinationType, IntSize}
import utopia.coder.model.scala.Package
import utopia.coder.model.scala.code.CodePiece

import java.nio.file.Path
import scala.util.{Failure, Success, Try}

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
	def apply(path: Path): Try[ProjectData] = JsonBunny(path).flatMap { v =>
		val root = v.getModel
		
		// Parses custom data types (must succeed)
		root("data_types", "types").getModel.properties.tryMap { c =>
			c.value.model
				.toTry { new DataTypeException(
					s"Custom data types must be presented as json objects. '$v' is not a model.") }
				.flatMap(CustomPropertyType.apply)
				.map { c.name.toLowerCase -> _ }
		}.map { customTypes =>
			implicit val namingRules: NamingRules = NamingRules(root("naming").getModel).value
			
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
			val projectName = Header.from(root, disableGeneric = true)
				.orElse { root("name", "project").string.map { Name.interpret(_, namingRules(Header)) } }
				.getOrElse {
					basePackage.parts.lastOption match {
						case Some(lastPart) => Name.interpret(lastPart, CamelCase.lower)
						case None =>
							dbPackage.parent.parts.lastOption
								.orElse { dbPackage.parts.lastOption }
								.map { Name.interpret(_, CamelCase.lower) }
								.getOrElse { data.Name("Project", CamelCase.capitalized) }
					}
				}
			
			val databaseName = DatabaseName.from(root, disableGeneric = true)
			val enumPackage = modelPackage / "enumeration"
			val enumerations = enumsFrom(root, enumPackage, author)
			val referencedEnumerations = root("referenced_enums", "referenced_enumerations").getVector
				.flatMap { _.string }
				.map { enumPath =>
					val (packagePart, enumName) = enumPath.splitAtLast(".").toTuple
					Enum(enumName, packagePart, Vector())
				}
			val allEnumerations = enumerations ++ referencedEnumerations
			
			// Expects the classes property to contain an object where keys are package names and values are
			// either references to other files or arrays containing class objects
			val classData = root("classes").getModel.properties.flatMap { packageAtt =>
				packageAtt.value.castTo(VectorType, StringType) match {
					// Case: Class object array input => Parses the objects
					case Left(vectorVal) =>
						vectorVal.getVector.flatMap { _.model }
							.map { classFrom(_, packageAtt.name, allEnumerations, customTypesMap, author) }
					// Case: Reference input => Identifies and parses the referenced file
					case Right(stringVal) =>
						// If the specified path is relative, assumes that it is referred from this file's parent
						val subPath: Path = stringVal.getString
						val targetPath = if (subPath.isAbsolute) subPath else path.parent/subPath
						InputFiles.versionedFileFrom(targetPath, "json") match {
							// Case: File found => Parses it
							case Some(referencedPath) =>
								JsonBunny(referencedPath) match {
									// Case: Parsing succeeded => Processes parsed content
									case Success(parsed) =>
										parsed.getVector.flatMap { _.model }
											.map { classFrom(_, packageAtt.name, allEnumerations, customTypesMap, author) }
									// Case: Parsing failed => Prints a warning
									case Failure(error) =>
										error.printStackTrace()
										println(s"Failed to parse the json file referred as package ${
											packageAtt.name}, pointing to file ${referencedPath.absolute}")
										Vector.empty
								}
							// Case: File not found => Prints a warning
							case None =>
								println(s"Warning: $subPath for package ${
									packageAtt.name} is not a valid reference from ${path.parent.absolute}")
								Vector.empty
						}
				}
			}
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
								else if ((combo.childName !~== childClass.name.singular) &&
									(combo.childName ~== childClass.name.plural))
									MultiCombined
								else if (combo.alwaysLinked.isCertainlyTrue)
									Combined
								else
									PossiblyCombined
							}
						def parseAlias(alias: String, pluralForm: String, pluralIsDefault: Boolean = false) = {
							// Finds the alias name property, if defined
							alias.notEmpty.map { alias =>
								val namingConvention = NamingConvention.of(alias, namingRules(ClassPropName))
								// Finds the plural form, if defined
								pluralForm.notEmpty match {
									// Case: Plural form is defined
									case Some(pluralAlias) => Name(alias, pluralAlias, namingConvention)
									// Case: Only one form is defined
									case None =>
										// Case: Expects the main alias to be pluralized => Uses plural as singular, also
										if (pluralIsDefault)
											Name(alias, alias, namingConvention)
										// Case: Expects the main alias to be singular => Auto-pluralizes
										else
											Name.interpret(alias, namingConvention)
								}
							}
						}
						// Determines combination name
						val childAlias = parseAlias(combo.childAlias, combo.pluralChildAlias,
							combinationType.isOneToMany)
						val parentAlias = parseAlias(combo.parentAlias, combo.pluralParentAlias)
						val comboName = combo.name.notEmpty match {
							// Case: Custom name has been given
							case Some(name) =>
								val namingConvention = NamingConvention.of(name, namingRules(ClassName))
								combo.pluralName.notEmpty match {
									// Case: Plural form has also been specified
									case Some(pluralName) => Name(name, pluralName, namingConvention)
									// Case: No plural form specified => Auto-pluralizes
									case None => Name.interpret(name, namingConvention)
								}
							// Case: No name specified => Generates a name
							case None =>
								val childPart = childAlias.getOrElse(childClass.name)
								val base = parentClass.name + data.Name("with", "with", CamelCase.lower)
								// Case: There are multiple children => Plural child name is used in all name forms
								// E.g. CarWithTires and CarsWithTires
								if (combinationType.isOneToMany)
									base + Name(childPart.plural, childPart.plural, childPart.style)
								// Case: There are 0-1 children => Both parent and child name follow same pluralization
								// E.g. CarWithMotor => CarsWithMotors
								else
									Name(s"${base.singular}${ childPart.singularIn(base.style) }",
										s"${ base.plural }${ childPart.pluralIn(base.style) }", base.style)
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
			ProjectData(projectName, modelPackage, dbPackage, databaseName,
				enumerations, classes, combinations, instances, namingRules, root("version").string.map { Version(_) },
				!root("models_without_vault").getBoolean, root("prefix_columns").getBoolean)
		}
	}
	
	private def enumsFrom(root: Model, enumPackage: Package, author: String)(implicit namingRules: NamingRules) = {
		root("enumerations", "enums").castTo(VectorType, ModelType) match {
			// Case: Array model -based enumeration syntax => parses each object into an enumeration
			case Left(enumsValue) =>
				enumsValue.getVector.flatMap { _.model }.map { model =>
					val name = EnumName.from(model).getOrElse { Name("UnnamedEnum") }
					val idName = Name.interpret(model("index_name", "index_property", "index_prop", "id_name", "id_property",
						"id_prop", "index", "id").stringOr("id"), ClassPropName.style)
					val underScoreIdName = idName.singularIn(UnderScore)
					val values = model("values", "vals").getVector.zipWithIndex.map { case(v, index) =>
						v.model match {
							case Some(model) => enumValueFrom(model, index, underScoreIdName)
							case None => EnumerationValue(v.getString, (index + 1).toString)
						}
					}
					val idType = model("index_type", "id_type", "type").string
						.flatMap { custom =>
							StandardPropertyType.interpret(custom, model("id_length", "id_max", "length").int,
								Some(name.singular))
						}
						// The default ID type is integer, with size enough to fit all values (i.e. tiny)
						.getOrElse { IntNumber(IntSize.fitting(values.size).getOrElse(Default)) }
					val default = model("default").string
						.flatMap { defaultName => values.find { _.name ~== defaultName } }
					val actualPackage = model("package").string match {
						case Some(pck) => Package(pck)
						case None => enumPackage
					}
					Enum(name, actualPackage, values, default, idName, idType, model("doc").getString, author)
				}
			// Case: Old-school enumeration syntax using an object
			case Right(enumsModelValue) =>
				enumsModelValue.getModel.properties.map { enumAtt =>
					Enum(enumAtt.name.capitalize, enumPackage,
						enumAtt.value.getVector.zipWithIndex.map { case (v, index) =>
							v.model match {
								case Some(model) => enumValueFrom(model, index)
								case None => EnumerationValue(v.getString, (index + 1).toString)
							}
						},
						author = author)
				}
		}
	}
	
	private def enumValueFrom(model: Model, index: Int, underScoreIdPropName: String = "id")
	                         (implicit naming: NamingRules) =
	{
		val customId: Option[CodePiece] = model(underScoreIdPropName, "key", "id")
		val id = customId.getOrElse(CodePiece((index + 1).toString))
		val name = EnumValueName.from(model)
			// Uses the id as a backup value name
			.getOrElse {
				Name.interpret(if (id.text.headOption.exists { _.isLetter }) id.text else s"_${id.text}",
					naming(EnumValueName))
			}
		EnumerationValue(name, id, model("doc", "description").getString)
	}
	
	private def classFrom(classModel: Model, packageName: String, enumerations: Iterable[Enum],
	                      customTypes: Map[String, PropertyType], defaultAuthor: String)
	                     (implicit naming: NamingRules) =
	{
		// Determines class name
		// Uses table name as a backup for class name
		val tableName = TableName.from(classModel, disableGeneric = true)
		val name = ClassName.from(classModel).orElse(tableName)
			.getOrElse { data.Name("Unnamed", naming(ClassName)) + packageName }
		
		// Reads properties
		val properties = classModel("properties", "props").getVector.flatMap { _.model }
			.map { propertyFrom(_, enumerations, name, customTypes) }
		val idName = classModel("id_name", "id").string.map { Name.interpret(_, ClassPropName.style) }
		
		// Finds the combo indices
		// The indices in the document are given as property names, but here they are converted to column names
		val comboIndexColumnNames: Vector[Vector[String]] = classModel("index", "combo_index").vector
			.map[Vector[Vector[Name]]] { v =>
				Vector(v.flatMap { _.string }.map { s => Name.interpret(s, ClassPropName.style) })
			}
			.orElse {
				classModel("indices", "combo_indices").vector
					.map { vectors =>
						vectors.map[Vector[Name]] { vector =>
							vector.getVector.flatMap { _.string }.map { s => Name.interpret(s, ClassPropName.style) }
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
				case Some(n) => Some(Name.interpret(n, ClassPropName.style))
				case None =>
					if (classModel("described", "is_described").getBoolean)
						Some(name + "id")
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
					comboModel("parent_alias_plural", "plural_parent_alias", "alias_parent_plural",
						"plural_alias_parent").getString,
					comboModel("child_alias", "alias_child").getString,
					comboModel("child_alias_plural", "plural_child_alias", "alias_child_plural", "plural_alias_child",
						"children_alias", "alias_children").getString,
					comboModel("type").getString,
					comboModel("name").getString.capitalize, comboModel("name_plural").getString.capitalize,
					comboModel("doc").getString, comboModel("always_linked", "is_always_linked").boolean,
					comboModel.containsNonEmpty("children"))
			}
		}
		
		val readClass = new Class(name, tableName.map { _.table }, idName.getOrElse(Class.defaultIdName),
			properties, packageName, classModel("access_package", "sub_package", "access").getString, comboIndexColumnNames,
			descriptionLinkColumnName, classModel("doc").getString, classModel("author").stringOr(defaultAuthor),
			classModel("use_long_id").getBoolean,
			// Writes generic access point if this class has combinations, or if explicitly specified
			classModel("has_combos", "generic_access", "tree_inheritance").booleanOr(comboInfo.nonEmpty))
		
		// Also parses class instances if they are present
		val instances = classModel("instance").model match {
			case Some(instanceModel) => Vector(instanceFrom(instanceModel, readClass))
			case None => classModel("instances").getVector.flatMap { _.model }.map { instanceFrom(_, readClass) }
		}
		
		(readClass, comboInfo, instances)
	}
	
	private def propertyFrom(propModel: Model, enumerations: Iterable[Enum], className: Name,
	                         customTypes: Map[String, PropertyType])
	                        (implicit naming: NamingRules) =
	{
		// May use column name as a backup name
		val specifiedName = ClassPropName.from(propModel).orElse { ColumnName.from(propModel, disableGeneric = true) }
		
		// val name = propModel("name").getString
		// val columnName = propModel("column_name").stringOr(NamingUtils.camelToUnderscore(name))
		val tableReference = propModel("references", "ref").string.map { ref =>
			val (tablePart, columnPart) = ref.splitAtFirst("(").toTuple
			tablePart -> columnPart.untilLast(")").notEmpty.map { Name.interpret(_, ColumnName.style) }
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
							enumerations.find { _.name ~== enumName }
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
						case None => StandardPropertyType.interpret(typeName, length, specifiedName)
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
		
		val name: Name = specifiedName.getOrElse { actualDataType.defaultPropertyName }
		
		val rawDoc = propModel("doc").string.filter { _.nonEmpty }
		val doc = rawDoc.getOrElse { actualDataType.writeDefaultDescription(className, name) }
		
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
		
		Property(name, actualDataType, default, dbPropertyOverrides, propModel("with").getString,
			propModel("in").getString, doc)
	}
	
	private def dbPropertyOverridesFrom(model: Model, default: CodePiece = CodePiece.empty, readName: Boolean = false)
	                                   (implicit naming: NamingRules) =
	{
		// Name-processing may be skipped
		val columnName = ColumnName.from(model, disableGeneric = true)
		val name = if (readName) ClassPropName.from(model).orElse(columnName) else None
		val finalColumnName = columnName match {
			case Some(name) => name.column
			case None => ""
		}
		
		// A custom default value may be carried over to sql under some circumstances
		val sqlDefault = model("sql_default", "sql_def").stringOr { default.toSql.getOrElse("") }
		
		val limit = {
			// Attempts to parse the limit from the "limit" property
			val raw = model("length_rule", "length_limit", "limit", "max_length", "length_max", "max")
			val base = raw.int
				// Uses type property upper limit as a backup
				.orElse { model("type").getString.afterFirst("(").untilFirst(")").afterFirst("-").int } match
			{
				case Some(max) => s"to $max"
				case None => raw.getString
			}
			// Adds cropping, if appropriate
			if (model("allow_crop", "crop").getBoolean) {
				if (base.isEmpty) "crop" else s"$base or crop"
			}
			else
				base
		}
		
		DbPropertyOverrides(name, finalColumnName, sqlDefault, limit, model("indexed", "index", "is_index").boolean)
	}
	
	private def instanceFrom(model: Model, parentClass: Class)(implicit naming: NamingRules) =
	{
		// Matches model properties against class properties
		val properties = ClassPropName.use { implicit c =>
			model.nonEmptyProperties.flatMap { att =>
				val attName = Name.contextual(att.name)
				parentClass.properties.find { _.name ~== attName }.map { _ -> att.value }
			}.toMap
		}
		Instance(parentClass, properties, model("id"))
	}
	
	
	// NESTED   --------------------------------------
	
	private case class RawCombinationData(childName: String, parentAlias: String, pluralParentAlias: String,
	                                      childAlias: String, pluralChildAlias: String, comboTypeName: String,
	                                      name: String, pluralName: String, doc: String,
	                                      alwaysLinked: UncertainBoolean, childrenDefinedAsPlural: Boolean)
}
