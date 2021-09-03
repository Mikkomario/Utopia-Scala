package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration}
import utopia.citadel.coder.model.scala.{Extension, Parameter, Reference}
import utopia.flow.util.FileExtensions._
import utopia.flow.util.StringExtensions._

import scala.io.Codec

/**
  * Used for writing standard model (from DB) factory objects
  * @author Mikko Hilpinen
  * @since 1.9.2021, v0.1
  */
object FactoryWriter
{
	/**
	  * Writes a factory used for processing database object data
	  * @param classToWrite Class data based on which the factory is created
	  * @param tablesRef Reference to the tables object
	  * @param modelRef Reference to the read model class
	  * @param dataRef Reference to the partial model data class
	  * @param codec Implicit codec to use when writing the document
	  * @param setup Implicit project-specific setup
	  * @return Reference to the new written factory object. Failure if writing failed.
	  */
	def apply(classToWrite: Class, tablesRef: Reference, modelRef: Reference, dataRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val parentPackage = s"${setup.projectPackage}.database.factory.${classToWrite.packageName}"
		val objectName = s"${classToWrite.name}Factory"
		val baseTrait = Reference.fromValidatedRowModelFactory(modelRef)
		val extensions: Vector[Extension] =
		{
			if (classToWrite.recordsCreationTime)
				Vector(baseTrait, Reference.fromRowFactoryWithTimestamps(modelRef))
			else
				Vector(baseTrait)
		}
		val baseProperties = Vector(ComputedProperty("table", Set(tablesRef), isOverridden = true)(
			s"${tablesRef.target}.${classToWrite.name.uncapitalize}"))
		// TODO: Add deprecation support
		File(parentPackage,
			// Extends FromValidatedRowModelFactory[A]
			ObjectDeclaration(objectName, extensions,
				// Contains implemented table reference
				properties = classToWrite.creationTimeProperty match {
					case Some(createdProp) => baseProperties :+
						ComputedProperty("creationTimePropertyName", isOverridden = true)(createdProp.name.quoted)
					case None => baseProperties
				},
				// Contains fromValidatedModel implementation
				methods = Set(MethodDeclaration("fromValidatedModel", Set(modelRef, dataRef, Reference.valueUnwraps),
					isOverridden = true)(Parameter("model", Reference.model(Reference.constant)))(
					s"${modelRef.target}(model(${"\"id\""}), ${dataRef.target}(${
						classToWrite.properties.map { prop => s"model(${prop.name.quoted})" }.mkString(", ")}))")),
				description = s"Used for reading ${classToWrite.name} data from the DB"
			)
		).writeTo(setup.sourceRoot/"database/factory"/classToWrite.packageName/s"$objectName.scala")
			.map { _ => Reference(parentPackage, objectName) }
	}
}
