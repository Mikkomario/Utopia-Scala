package utopia.vault.coder.controller.writer

import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.Visibility.Protected
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.{Parameter, Parameters, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration}

import scala.io.Codec

/**
  * Writes described model classes (combine a base model with descriptions)
  * @author Mikko Hilpinen
  * @since 9.10.2021, v
  */
object DescribedModelWriter
{
	/**
	  * Writes a described model class for the specified class
	  * @param classToWrite Class based on which the model class is generated
	  * @param modelRef Reference to the stored model class version
	  * @param setup Implicit project setup
	  * @param codec Implicit codec to use when writing files
	  * @return Reference to the written file. Failure if file writing failed.
	  */
	def apply(classToWrite: Class, modelRef: Reference)(implicit setup: ProjectSetup, codec: Codec) =
	{
		val className = s"Described${classToWrite.name}"
		
		File(setup.modelPackage/"combined"/classToWrite.packageName,
			// Companion object is used for parsing from model data
			ObjectDeclaration(className, Vector(Reference.describedFromModelFactory(ScalaType.basic(className))),
				properties = Vector(
					ComputedProperty("undescribedFactory", Set(modelRef), isOverridden = true)(modelRef.target)
				),
				description = s"Used for parsing described copies of ${classToWrite.name.plural} from model data"
			),
			// Class combines the model with its descriptions
			ClassDeclaration(className,
				Parameters(Parameter("wrapped", modelRef),
					Parameter("descriptions", ScalaType.set(Reference.descriptionLink))),
				Vector(Reference.describedWrapper(modelRef), Reference.simplyDescribed),
				methods = Set(MethodDeclaration("simpleBaseModel", visibility = Protected, isOverridden = true)(
					Parameter("roles", ScalaType.iterable(Reference.descriptionRole)))("wrapped.toModel")),
				description = s"Combines ${classToWrite.name} with the linked descriptions",
				isCaseClass = true
			)
		).write()
	}
}
