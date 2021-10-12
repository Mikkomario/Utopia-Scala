package utopia.vault.coder.controller

import utopia.bunnymunch.jawn.JsonBunny
import utopia.vault.coder.model.enumeration.BasicPropertyType.{IntNumber, Text}
import utopia.vault.coder.model.enumeration.PropertyType.{ClassReference, EnumValue, Optional}
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
	  * @return Base package name, followed by the read enumerations, the read classes and the project author.
	  *         Failure if file reading or class parsing failed.
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
		
		classes.map { (basePackage, enumerations, _) }
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
			val className = rawClassName.getOrElse { NamingUtils.underscoreToCamel(rawTableName.get) }
			val tableName = rawTableName.getOrElse { NamingUtils.camelToUnderscore(className) }
			val fullName = Name(className,
				classModel("name_plural", "plural_name").string.map { _.capitalize }.getOrElse(className + "s"))
			val properties = classModel("properties", "props").getVector.flatMap { _.model }
				.map { propertyFrom(_, enumerations, fullName) }
			// Finds the combo indices
			// The indices in the document are given as property names, but here they are converted to column names
			val comboIndexColumnNames: Vector[Vector[String]] = classModel("index", "combo", "combo_index")
				.vector.map[Vector[Vector[String]]] { v => Vector(v.flatMap { _.string }) }
				.orElse {
					classModel("indices", "combos", "combo_indices").vector
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
			
			Success(Class(fullName, tableName, properties, packageName, comboIndexColumnNames,
				descriptionLinkColumnName, classModel("doc").getString, classModel("author").stringOr(defaultAuthor),
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
}
