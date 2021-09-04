package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.declaration.{ClassDeclaration, File}
import utopia.citadel.coder.model.scala.{Parameter, Reference, declaration}
import utopia.citadel.coder.util.NamingUtils
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

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
	  * @param classToWrite class being written
	  * @param codec Implicit codec used when writing files (implicit)
	  * @param setup Target project -specific settings (implicit)
	  * @return Reference to the stored version, followed by a reference to the data version. Failure if writing failed.
	  */
	def apply(classToWrite: Class)(implicit codec: Codec, setup: ProjectSetup) =
	{
		val baseDirectory = setup.sourceRoot/"model"
		val dataClassName = classToWrite.name.singular + "Data"
		val dataClassPackage = s"${setup.projectPackage}.model.partial.${classToWrite.packageName}"
		
		// Writes the data model
		File(dataClassPackage, Vector(
			ClassDeclaration(dataClassName,
				// Accepts a copy of each property. Uses default values where possible.
				classToWrite.properties.map { prop => Parameter(prop.name.singular, prop.dataType.toScala,
					prop.customDefault.notEmpty.getOrElse(prop.dataType.baseDefault), description = prop.description) },
				// Extends ModelConvertible
				Vector(Reference.modelConvertible),
				// Implements the toModel -property
				properties = Vector(
					ComputedProperty("toModel", Set(Reference.model, Reference.valueConversions), isOverridden = true)(
						s"Model(Vector(${ classToWrite.properties.map { prop =>
							s"${ NamingUtils.camelToUnderscore(prop.name.singular).quoted } -> ${prop.name}" }
							.mkString(", ") }))")
				),
				description = classToWrite.description,
				isCaseClass = true)
		)).writeTo(baseDirectory/"partial"/classToWrite.packageName/s"$dataClassName.scala").flatMap { _ =>
			// Writes the stored model next
			val dataClassRef = Reference(dataClassPackage, dataClassName)
			val storedClass =
			{
				val idType = classToWrite.idType.toScala
				// Accepts id and data -parameters
				val constructionParams = Vector(
					Parameter("id", idType, description = s"id of this ${classToWrite.name} in the database"),
					Parameter("data", dataClassRef, description = s"Wrapped ${classToWrite.name} data")
				)
				val description = s"Represents a ${classToWrite.name} that has already been stored in the database"
				// ModelConvertible extension & implementation differs based on id type
				if (classToWrite.useLongId)
					declaration.ClassDeclaration(classToWrite.name.singular, constructionParams,
						Vector(Reference.stored(dataClassRef, idType)),
						properties = Vector(
							ComputedProperty("toModel", Set(Reference.valueConversions, Reference.constant),
								isOverridden = true)("Constant(\"id\", id) + data.toModel")
						), description = description, isCaseClass = true)
				else
					declaration.ClassDeclaration(classToWrite.name.singular, constructionParams,
						Vector(Reference.storedModelConvertible(dataClassRef)),
						description = description, isCaseClass = true)
			}
			val storePackage = s"${setup.projectPackage}.model.stored.${classToWrite.packageName}"
			File(storePackage, Vector(storedClass))
				.writeTo(baseDirectory/"stored"/classToWrite.packageName/s"${classToWrite.name}.scala")
				.map { _ => Reference(storePackage, storedClass.name) -> dataClassRef }
		}
	}
}
