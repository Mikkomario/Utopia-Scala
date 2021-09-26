package utopia.vault.coder.controller

import utopia.bunnymunch.jawn.JsonBunny
import utopia.vault.coder.model.enumeration.BasicPropertyType.{IntNumber, Text}
import utopia.vault.coder.model.enumeration.PropertyType.{ClassReference, EnumValue}
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.StringType
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Enum, Name, Property}
import utopia.vault.coder.model.enumeration.{BasicPropertyType, PropertyType}
import utopia.vault.coder.util.NamingUtils

import java.nio.file.Path

/**
  * Used for reading class data from a .json file
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
object ClassReader
{
	// TODO: Remove schemas and add support for auto-naming
	private val classSchema = ModelDeclaration("name" -> StringType)
	private val propertySchema = ModelDeclaration("name" -> StringType)
	
	/**
	  * Reads class data from a .json file
	  * @param path Path to the file to read
	  * @return Base package name, followed by the read enumerations and the read classes.
	  *         Failure if file reading or class parsing failed.
	  */
	def apply(path: Path) = JsonBunny(path).flatMap { v =>
		val root = v.getModel
		val basePackage = root("base_package").getString
		val enumPackage = s"$basePackage.model.enumeration"
		val enumerations = root("enumerations").getModel.attributes.map { enumAtt =>
			Enum(enumAtt.name.capitalize, enumAtt.value.getVector.flatMap { _.string }.map { _.capitalize }, enumPackage)
		}
		val classes = root("classes").getModel.attributes.tryMap { packageAtt =>
			packageAtt.value.model match
			{
				case Some(classModel) =>
					classSchema.validate(classModel).toTry
						.flatMap { parseClassFrom(_, packageAtt.name, enumerations).map { Vector(_) } }
				case None =>
					packageAtt.value.getVector.flatMap { _.model }.tryMap { classSchema.validate(_).toTry }
						.flatMap { classModels => classModels.tryMap { parseClassFrom(_, packageAtt.name, enumerations) } }
			}
		}.map { _.flatten }
		
		classes.map { (basePackage, enumerations, _) }
	}
	
	private def parseClassFrom(classModel: Model[Constant], packageName: String, enumerations: Iterable[Enum]) =
	{
		classModel("properties").getVector.flatMap { _.model }.tryMap { propertySchema.validate(_).toTry }
			.map { propModels =>
				val className = classModel("name").getString.capitalize
				val tableName = classModel("table_name").stringOr(NamingUtils.camelToUnderscore(className))
				val properties = propModels.map { propModel =>
					val name = propModel("name").getString
					val columnName = propModel("column_name").stringOr(NamingUtils.camelToUnderscore(name))
					val referencedTableName = propModel("references").string
					val length = propModel("length").int
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
								case _ => None
							}.getOrElse(IntNumber))
						case None =>
							baseDataType.getOrElse {
								length match
								{
									case Some(length) => Text(length)
									case None => IntNumber
								}
							}
					}
					Property(Name(name, propModel("name_plural").stringOr(name + "s")), columnName, actualDataType,
						propModel("doc").getString, propModel("usage").getString, propModel("default").getString)
				}
				Class(Name(className, classModel("name_plural").string.map { _.capitalize }.getOrElse(className + "s")),
					tableName, properties, packageName, classModel("doc").getString,
					classModel("use_long_id").getBoolean)
			}
	}
}
