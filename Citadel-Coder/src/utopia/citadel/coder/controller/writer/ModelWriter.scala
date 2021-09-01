package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.Class
import utopia.citadel.coder.model.scala.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.{ClassDeclaration, File, Parameter, Reference, ScalaType}
import utopia.citadel.coder.util.NamingUtils
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

import java.nio.file.Path
import scala.io.Codec

/**
  * Used for writing model data from class data
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
object ModelWriter
{
	/**
	  * Writes stored and partial model classes for a class template
	  * @param targetDirectory Project source base directory
	  * @param basePackageName full path of the project base package
	  * @param classToWrite class being written
	  * @param codec Implicit codec used when writing files
	  * @return Reference to the stored version, followed by a reference to the data version. Failure if writing failed.
	  */
	def apply(targetDirectory: Path, basePackageName: String, classToWrite: Class)(implicit codec: Codec) =
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
		)).writeTo(baseDirectory/"partial"/classToWrite.packageName/s"$dataClassName.scala").flatMap { _ =>
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
			val storePackage = s"$basePackageName.model.stored.${classToWrite.packageName}"
			File(storePackage, Vector(storedClass))
				.writeTo(baseDirectory/"stored"/classToWrite.packageName/s"${classToWrite.name}.scala")
				.map { _ => Reference(storePackage, storedClass.name) -> dataClassRef }
		}
	}
}
