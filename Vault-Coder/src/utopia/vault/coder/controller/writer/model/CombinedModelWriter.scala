package utopia.vault.coder.controller.writer.model

import utopia.vault.coder.model.data.{CombinationData, CombinationReferences, NamingRules, ProjectSetup}
import utopia.vault.coder.model.scala.{DeclarationDate, Reference}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{ClassDeclaration, File}

import scala.io.Codec

/**
  * Used for writing combining models
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  */
object CombinedModelWriter
{
	/**
	  * Writes the combination of two model classes
	  * @param data Combination building instructions
	  * @param parentRef Reference to the combination parent part (stored model)
	  * @param parentDataRef Reference to the data model of the combination parent
	  * @param childRef Reference to the combination child part (stored model)
	  * @param setup Implicit project setup
	  * @param codec Implicit codec used when writing the file
	  * @return Combination related references. Failure if file writing failed.
	  */
	def apply(data: CombinationData, parentRef: Reference, parentDataRef: Reference, childRef: Reference)
	         (implicit setup: ProjectSetup, codec: Codec, naming: NamingRules) =
	{
		val constructorParams = data.combinationType.applyParamsWith(data.parentName, data.childName,
			parentRef, childRef)
		File(setup.combinedModelPackage/data.packageName,
			ClassDeclaration(data.name.className, constructionParams = constructorParams,
				// Provides implicit access to the data model (because that's where most of the properties are)
				extensions = Vector(Reference.extender(parentDataRef)),
				properties = Vector(
					// Provides direct access to parent.id
					ComputedProperty("id", description = s"Id of this ${data.parentName} in the database")(
						s"${constructorParams.head.name}.id"),
					ComputedProperty("wrapped", isOverridden = true)(s"${constructorParams.head.name}.data")
				), description = s"Combines ${data.parentName} with ${data.childName} data", author = data.author,
				since = DeclarationDate.versionedToday, isCaseClass = true
			)
		).write().map { comboRef => CombinationReferences(parentRef, childRef, comboRef) }
	}
}
