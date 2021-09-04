package utopia.citadel.coder.controller

import utopia.bunnymunch.jawn.JsonBunny
import utopia.citadel.coder.model.data.{Class, Property}
import utopia.citadel.coder.model.enumeration.BasicPropertyType.{Integer, Text}
import utopia.citadel.coder.model.enumeration.{BasicPropertyType, PropertyType}
import utopia.citadel.coder.model.enumeration.PropertyType.ClassReference
import utopia.citadel.coder.util.NamingUtils
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.StringType
import utopia.flow.util.CollectionExtensions._

import java.nio.file.Path

/**
  * Used for reading class data from a .json file
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
object ClassReader
{
	val classSchema = ModelDeclaration("name" -> StringType)
	val propertySchema = ModelDeclaration("name" -> StringType)
	
	def apply(path: Path) = JsonBunny(path).flatMap { v =>
		val root = v.getModel
		val basePackage = root("base_package").getString
		// TODO: Add package reading
		root("classes").getVector.flatMap { _.model }.tryMap { classSchema.validate(_).toTry.flatMap { classModel =>
			parseClassFrom(classModel)
		} }
	}
	
	private def parseClassFrom(classModel: Model[Constant]) =
	{
		classModel("properties").getVector.flatMap { _.model }.tryMap { propertySchema.validate(_).toTry }
			.map { propModels =>
				val className = classModel("name").getString
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
							}.getOrElse(Integer))
						case None =>
							baseDataType.getOrElse {
								length match
								{
									case Some(length) => Text(length)
									case None => Integer
								}
							}
					}
					Property(name, columnName, actualDataType, propModel("doc").getString,
						propModel("usage").getString, propModel("default").getString)
				}
				Class(className, tableName, properties, classModel("package").getString,
					classModel("doc").getString, classModel("use_long_id").getBoolean)
			}
	}
}
