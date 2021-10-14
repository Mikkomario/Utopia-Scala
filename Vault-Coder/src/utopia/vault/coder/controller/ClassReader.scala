package utopia.vault.coder.controller

import utopia.bunnymunch.jawn.JsonBunny
import utopia.vault.coder.model.enumeration.BasicPropertyType.{IntNumber, Text}
import utopia.vault.coder.model.enumeration.PropertyType.{ClassReference, EnumValue, Optional}
import utopia.flow.datastructure.immutable.{Constant, Model, ModelValidationFailedException}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.UncertainBoolean
import utopia.vault.coder.model.data.{Class, CombinationData, Enum, Name, ProjectData, Property}
import utopia.vault.coder.model.enumeration.CombinationType.{Combined, MultiCombined, PossiblyCombined}
import utopia.vault.coder.model.enumeration.{BasicPropertyType, CombinationType, PropertyType}
import utopia.vault.coder.util.NamingUtils

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
		val author = root("author").getString
		val basePackage = root("base_package", "package").getString
		val enumPackage = s"$basePackage.model.enumeration"
		val enumerations = root("enumerations", "enums").getModel.attributes.map { enumAtt =>
			Enum(enumAtt.name.capitalize, enumAtt.value.getVector.flatMap { _.string }.map { _.capitalize },
				enumPackage, author)
		}
		val classes = root("classes", "class").getModel.attributes.tryMap { packageAtt =>
			packageAtt.value.model match
			{
				case Some(classModel) =>
					parseClassFrom(classModel, packageAtt.name, enumerations, author).map { Vector(_) }
				case None =>
					packageAtt.value.getVector.flatMap { _.model }
						.tryMap { parseClassFrom(_, packageAtt.name, enumerations, author) }
			}
		}.map { _.flatten }
		
		classes.map { classData =>
			val classes = classData.map { _._1 }
			// Processes the proposed combinations
			val combinations = classData.flatMap { case (parentClass, combos) =>
				combos.flatMap { combo =>
					// Finds the child class (child name match)
					classes.find { c => c.name.variants.exists { _ ~== combo.childName } }.map { childClass =>
						// Determines the combination type
						val combinationType = combo.comboTypeName.notEmpty.flatMap(CombinationType.interpret)
							.getOrElse {
								if (combo.childrenDefinedAsPlural)
									MultiCombined
								else if (combo.childName ~== childClass.name.plural)
									MultiCombined
								else if (combo.alwaysLinked.isTrue)
									Combined
								else
									PossiblyCombined
							}
						// Determines combination name
						val comboNameSingular = combo.name.notEmpty.getOrElse {
							s"${parentClass.name}With${combo.childAlias.notEmpty.getOrElse {
								combinationType match
								{
									case MultiCombined => childClass.name.plural
									case _ => childClass.name.singular
								}
							}}"
						}
						val comboName = Name(comboNameSingular,
							combo.namePlural.notEmpty.getOrElse(comboNameSingular + "s"))
						val isAlwaysLinked = combo.alwaysLinked.getOrElse {
							combinationType match {
								case Combined => true
								case _ => false
							}
						}
						CombinationData(combinationType, comboName, parentClass, childClass, combo.parentAlias,
							combo.childAlias, isAlwaysLinked)
					}
				}
			}
			ProjectData(basePackage, enumerations, classes, combinations)
		}
	}
	
	private def parseClassFrom(classModel: Model[Constant], packageName: String, enumerations: Iterable[Enum],
	                           defaultAuthor: String) =
	{
		val rawClassName = classModel("name").string.filter { _.nonEmpty }.map { _.capitalize }
		val rawTableName = classModel("table_name", "table").string.filter { _.nonEmpty }
		
		if (rawClassName.isEmpty && rawTableName.isEmpty )
			Failure(new ModelValidationFailedException("'name', 'table_name' or 'table' is required in a class model"))
		else
		{
			// Determines class name
			val className = rawClassName.getOrElse { NamingUtils.underscoreToCamel(rawTableName.get) }
			val tableName = rawTableName.getOrElse { NamingUtils.camelToUnderscore(className) }
			val fullName = Name(className,
				classModel("name_plural", "plural_name").string.map { _.capitalize }.getOrElse(className + "s"))
			
			// Reads properties
			val properties = classModel("properties", "props").getVector.flatMap { _.model }
				.map { propertyFrom(_, enumerations, fullName) }
			
			// Finds the combo indices
			// The indices in the document are given as property names, but here they are converted to column names
			val comboIndexColumnNames: Vector[Vector[String]] = classModel("index", "combo_index")
				.vector.map[Vector[Vector[String]]] { v => Vector(v.flatMap { _.string }) }
				.orElse {
					classModel("indices", "combo_indices").vector
						.map { vectors => vectors.map[Vector[String]] { vector => vector.getVector.flatMap { _.string } } }
				}
				.getOrElse(Vector())
				.map { combo => combo.flatMap { propName =>
					properties.find { _.name.variants.exists { _ ~== propName } }.map { _.columnName } } }
				.filter { _.nonEmpty }
			
			// Checks whether descriptions are supported for this class
			val descriptionLinkColumnName = classModel("description_link_column", "description_link", "desc_link")
				.stringOr {
					if (classModel("described", "is_described").getBoolean)
						tableName + "_id"
					else
						""
				}
			
			// Reads combination-related information
			val comboInfo = (classModel("combination", "combo").model match
			{
				case Some(comboModel) => Vector(comboModel)
				case None => classModel("combinations", "combos").getVector.flatMap { _.model }
			}).flatMap { comboModel =>
				comboModel("child", "children").string.map { childName =>
					RawCombinationData(childName, comboModel("parent_alias", "alias_parent").getString,
						comboModel("child_alias", "alias_child").getString, comboModel("type").getString,
						comboModel("name").getString, comboModel("name_plural").getString,
						comboModel("always_linked", "is_always_linked").boolean,
						comboModel.containsNonEmpty("children"))
				}
			}
			
			Success(Class(fullName, tableName, properties, packageName, comboIndexColumnNames,
				descriptionLinkColumnName, classModel("doc").getString, classModel("author").stringOr(defaultAuthor),
				classModel("use_long_id").getBoolean) -> comboInfo)
		}
	}
	
	private def propertyFrom(propModel: Model[Constant], enumerations: Iterable[Enum], className: Name) =
	{
		val rawName = propModel("name").string.filter { _.nonEmpty }
		val rawColumnName = propModel("column_name", "column", "col").string.filter { _.nonEmpty }
		
		// val name = propModel("name").getString
		// val columnName = propModel("column_name").stringOr(NamingUtils.camelToUnderscore(name))
		val referencedTableName = propModel("references", "ref").string
		val length = propModel("length", "len").int
		val baseDataType = propModel("type").string.flatMap { typeName =>
			val lowerTypeName = typeName.toLowerCase
			val enumType =
			{
				if (lowerTypeName.contains("enum"))
				{
					val enumName = lowerTypeName.afterFirst("enum")
						.afterFirst("[").untilFirst("]")
					enumerations.find { _.name.toLowerCase == enumName }
				}
				else
					None
			}
			enumType match
			{
				case Some(enumType) => Some(EnumValue(enumType, lowerTypeName.contains("option")))
				case None => PropertyType.interpret(typeName, length)
			}
		}
		val actualDataType = referencedTableName match
		{
			case Some(tableName) =>
				ClassReference(tableName, baseDataType.findMap {
					case b: BasicPropertyType => Some(b)
					case Optional(wrapped) => Some(wrapped)
					case _ => None
				}.getOrElse(IntNumber), isNullable = baseDataType.exists { _.isNullable })
			case None =>
				baseDataType.getOrElse {
					length match
					{
						case Some(length) => Text(length)
						case None => IntNumber
					}
				}
		}
		
		val name = rawName
			.orElse { rawColumnName.map(NamingUtils.underscoreToCamel) }
			.getOrElse { actualDataType.defaultPropertyName }
		val columnName = rawColumnName.getOrElse { NamingUtils.camelToUnderscore(name) }
		val fullName = Name(name, propModel("name_plural", "plural_name").stringOr(name + "s"))
		
		val rawDoc = propModel("doc").string.filter { _.nonEmpty }
		val doc = rawDoc.getOrElse { actualDataType.writeDefaultDescription(className, fullName) }
		
		Property(fullName, columnName, actualDataType, doc, propModel("usage").getString,
			propModel("default", "def").getString, propModel("indexed", "index", "is_index").boolean)
	}
	
	
	// NESTED   --------------------------------------
	
	private case class RawCombinationData(childName: String, parentAlias: String, childAlias: String,
	                                      comboTypeName: String, name: String, namePlural: String,
	                                      alwaysLinked: UncertainBoolean,
	                                      childrenDefinedAsPlural: Boolean)
}
