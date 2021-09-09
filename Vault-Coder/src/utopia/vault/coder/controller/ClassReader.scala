package utopia.vault.coder.controller

import utopia.bunnymunch.jawn.JsonBunny
import utopia.vault.coder.model.enumeration.BasicPropertyType.{IntNumber, Text}
import utopia.vault.coder.model.enumeration.PropertyType.ClassReference
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.StringType
import utopia.flow.util.CollectionExtensions._
import utopia.vault.coder.model.data.{Class, Name, Property}
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
	private val classSchema = ModelDeclaration("name" -> StringType)
	private val propertySchema = ModelDeclaration("name" -> StringType)
	
	/**
	  * Reads class data from a .json file
	  * @param path Path to the file to read
	  * @return Base package name, followed by the classes read. Failure if file reading or class parsing failed.
	  */
	def apply(path: Path) = JsonBunny(path).flatMap { v =>
		val root = v.getModel
		val basePackage = root("base_package").getString
		val classes = root("classes").getModel.attributes.tryMap { packageAtt =>
			packageAtt.value.model match
			{
				case Some(classModel) =>
					classSchema.validate(classModel).toTry
						.flatMap { parseClassFrom(_, packageAtt.name).map { Vector(_) } }
				case None =>
					packageAtt.value.getVector.flatMap { _.model }.tryMap { classSchema.validate(_).toTry }
						.flatMap { classModels => classModels.tryMap { parseClassFrom(_, packageAtt.name) } }
			}
		}.map { _.flatten }
		
		classes.map { basePackage -> _ }
	}
	
	private def parseClassFrom(classModel: Model[Constant], packageName: String) =
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
					val baseDataType = propModel("type").string.flatMap { PropertyType.interpret(_, length) }
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
