package utopia.citadel.coder.controller.writer

import utopia.citadel.coder.model.data.{Class, ProjectSetup}
import utopia.citadel.coder.model.scala.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.{File, MethodDeclaration, ObjectDeclaration, Parameter, Reference}
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
	def apply(classToWrite: Class, tablesRef: Reference, modelRef: Reference, dataRef: Reference)
	         (implicit codec: Codec, setup: ProjectSetup) =
	{
		val parentPackage = s"${setup.projectPackage}.database.factory.${classToWrite.packageName}"
		val objectName = s"${classToWrite.name}Factory"
		// TODO: Add deprecation support
		File(parentPackage, objects = Vector(
			// Extends FromValidatedRowModelFactory[A]
			ObjectDeclaration(objectName, Vector(Reference.fromValidatedRowModelFactory(modelRef)),
				// Contains implemented table reference
				properties = Vector(ComputedProperty("table", Set(tablesRef), isOverridden = true)(
					s"${tablesRef.target}.${classToWrite.name.uncapitalize}")),
				// Contains fromValidatedModel implementation
				methods = Set(MethodDeclaration("fromValidatedModel", Set(modelRef, dataRef, Reference.valueUnwraps),
					isOverridden = true)(Parameter("model", Reference.model(Reference.constant)))(
					s"${modelRef.target}(model(${"\"id\""}), ${dataRef.target}(${
						classToWrite.properties.map { prop => s"model(${prop.name.quoted})" }.mkString(", ")}))")))
		)).writeTo(setup.sourceRoot/"database/factory"/classToWrite.packageName/s"$objectName.scala")
			.map { _ => Reference(parentPackage, objectName) }
	}
}
