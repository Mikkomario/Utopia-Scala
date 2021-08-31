package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.Class
import utopia.citadel.coder.model.scala.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.{ClassDeclaration, File, Parameter, Reference, ScalaType}
import utopia.citadel.coder.util.NamingUtils
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

import java.nio.file.Path

/**
  * Used for writing model data from class data
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
object ModelWriter
{
	def apply(targetDirectory: Path, basePackageName: String, classToWrite: Class) =
	{
		val baseDirectory = targetDirectory/"model"
		val dataClassName = classToWrite.name + "Data"
		val dataClassPackage = s"$basePackageName.model.partial.${classToWrite.packageName}"
		
		// Writes the data model
		File(dataClassPackage, Vector(
			ClassDeclaration(dataClassName,
				classToWrite.properties.map { prop => Parameter(prop.name, prop.dataType.toScala,
					prop.customDefault.notEmpty.getOrElse(prop.dataType.baseDefault)) },
				Vector(Reference.modelConvertible),
				properties = Vector(
					ComputedProperty("toModel", Set(Reference.model, Reference.valueConversions), isOverridden = true)(
						s"Model(Vector(${ classToWrite.properties.map { prop =>
							s"${ NamingUtils.camelToUnderscore(prop.name).quoted } -> ${prop.name}" } }))")
				), isCaseClass = true)
		)).writeTo(baseDirectory/"partial"/classToWrite.packageName).flatMap { _ =>
			// Writes the stored model next
			val dataClassRef = Reference(dataClassPackage, dataClassName)
			val storedClass =
			{
				val idType = if (classToWrite.useLongId) ScalaType.long else ScalaType.int
				val constructionParams = Vector(Parameter("id", idType), Parameter("data", dataClassRef))
				if (classToWrite.useLongId)
					ClassDeclaration(classToWrite.name, constructionParams,
						Vector(Reference.stored(dataClassRef, idType)),
						properties = Vector(
							ComputedProperty("toModel", Set(Reference.valueConversions, Reference.constant),
								isOverridden = true)("Constant(\"id\", id) + data.toModel")
						), isCaseClass = true)
				else
					ClassDeclaration(classToWrite.name, constructionParams,
						Vector(Reference.storedModelConvertible(dataClassRef)), isCaseClass = true)
			}
			File(s"$basePackageName.model.stored.${classToWrite.packageName}", Vector(storedClass))
				.writeTo(baseDirectory/"stored"/classToWrite.packageName)
		}
	}
}
