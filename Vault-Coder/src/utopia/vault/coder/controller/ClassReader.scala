package utopia.vault.coder.controller

import utopia.bunnymunch.jawn.JsonBunny
import utopia.vault.coder.model.enumeration.BasicPropertyType.{IntNumber, Text}
import utopia.vault.coder.model.enumeration.PropertyType.{ClassReference, EnumValue}
import utopia.flow.datastructure.immutable.{Constant, Model, ModelValidationFailedException}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Enum, Name, Property}
import utopia.vault.coder.model.enumeration.{BasicPropertyType, PropertyType}
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
	/**
	  * Reads class data from a .json file
	  * @param path Path to the file to read
	  * @return Base package name, followed by the read enumerations and the read classes.
	  *         Failure if file reading or class parsing failed.
	  */
	def apply(path: Path) = JsonBunny(path).flatMap { v =>
		val root = v.getModel
		val basePackage = root("base_package", "package").getString
		val enumPackage = s"$basePackage.model.enumeration"
		val enumerations = root("enumerations", "enums").getModel.attributes.map { enumAtt =>
			Enum(enumAtt.name.capitalize, enumAtt.value.getVector.flatMap { _.string }.map { _.capitalize },
				enumPackage)
		}
		val classes = root("classes", "class").getModel.attributes.tryMap { packageAtt =>
			packageAtt.value.model match
			{
				case Some(classModel) => parseClassFrom(classModel, packageAtt.name, enumerations).map { Vector(_) }
				case None =>
					packageAtt.value.getVector.flatMap { _.model }
						.tryMap { parseClassFrom(_, packageAtt.name, enumerations) }
			}
		}.map { _.flatten }
		
		classes.map { (basePackage, enumerations, _) }
	}
	
	private def parseClassFrom(classModel: Model[Constant], packageName: String, enumerations: Iterable[Enum]) =
	{
		val rawClassName = classModel("name").string.filter { _.nonEmpty }.map { _.capitalize }
		val rawTableName = classModel("table_name", "table").string.filter { _.nonEmpty }
		
		if (rawClassName.isEmpty && rawTableName.isEmpty )
			Failure(new ModelValidationFailedException("'name', 'table_name' or 'table' is required in a class model"))
		else
		{
			val className = rawClassName.getOrElse { NamingUtils.underscoreToCamel(rawTableName.get) }
			val tableName = rawTableName.getOrElse { NamingUtils.camelToUnderscore(className) }
			val fullName = Name(className,
				classModel("name_plural", "plural_name").string.map { _.capitalize }.getOrElse(className + "s"))
			val properties = classModel("properties", "props").getVector.flatMap { _.model }
				.map { propertyFrom(_, enumerations, fullName) }
			
			Success(Class(fullName, tableName, properties, packageName, classModel("doc").getString,
				classModel("use_long_id").getBoolean))
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
		
		val name = rawName
			.orElse { rawColumnName.map(NamingUtils.underscoreToCamel) }
			.getOrElse { actualDataType.defaultPropertyName }
		val columnName = rawColumnName.getOrElse { NamingUtils.camelToUnderscore(name) }
		val fullName = Name(name, propModel("name_plural", "plural_name").stringOr(name + "s"))
		
		val rawDoc = propModel("doc").string.filter { _.nonEmpty }
		val doc = rawDoc.getOrElse { actualDataType.writeDefaultDescription(className, fullName) }
		
		Property(fullName, columnName, actualDataType, doc, propModel("usage").getString,
			propModel("default", "def").getString)
	}
}
