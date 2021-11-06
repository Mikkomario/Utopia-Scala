package utopia.vault.coder.controller.writer.model

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.Visibility.Protected
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration, ObjectDeclaration}
import utopia.vault.coder.model.scala.{DeclarationDate, Parameter, Parameters, Reference, ScalaType}

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
	  * @param modelRef     Reference to the stored model class version
	  * @param setup        Implicit project setup
	  * @param codec        Implicit codec to use when writing files
	  * @return Reference to the written file. Failure if file writing failed.
	  */
	def apply(classToWrite: Class, modelRef: Reference)
	         (implicit setup: ProjectSetup, codec: Codec) =
	{
		val className = s"Described${ classToWrite.name }"
		val modelParamName = classToWrite.name.singular.uncapitalize
		
		File(setup.combinedModelPackage/classToWrite.packageName,
			ObjectDeclaration(className, Vector(Reference.describedFactory(modelRef, ScalaType.basic(className)))),
			// (not present in this version,
			// because implementation requires data and stored models to have fromModel parsing)
			/*
			ObjectDeclaration(className,
				Vector(Reference.describedFromModelFactory(ScalaType.basic(className), modelRef)),
				properties = Vector(
					//  which it doesn't do at the moment
					ComputedProperty("undescribedFactory", Set(modelRef), isOverridden = true)(modelRef.target)
				),
				description = s"Used for parsing described copies of ${classToWrite.name.plural} from model data"
			),*/
			// Class combines the model with its descriptions
			ClassDeclaration(className,
				Parameters(Parameter(modelParamName, modelRef, description = s"${classToWrite.name} to wrap"),
					Parameter("descriptions", ScalaType.set(Reference.linkedDescription),
						description = s"Descriptions concerning the wrapped ${classToWrite.name}")),
				Vector(Reference.describedWrapper(modelRef), Reference.simplyDescribed),
				properties = Vector(ComputedProperty("wrapped", isOverridden = true)(modelParamName)),
				methods = Set(MethodDeclaration("simpleBaseModel", visibility = Protected, isOverridden = true,
					isLowMergePriority = true)(Parameter("roles", ScalaType.iterable(Reference.descriptionRole)))(
					"wrapped.toModel")),
				description = s"Combines ${ classToWrite.name } with the linked descriptions",
				author = classToWrite.author, since = DeclarationDate.versionedToday, isCaseClass = true
			)
		).write()
	}
}
