package utopia.vault.coder.controller.writer.model

import utopia.coder.model.data.NamingRules
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{CombinationData, CombinationReferences, VaultProjectSetup}
import utopia.coder.model.scala.{DeclarationDate, Parameter}
import utopia.coder.model.scala.datatype.{Extension, Reference, ScalaType}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.coder.model.scala.declaration.{ClassDeclaration, File, MethodDeclaration}
import utopia.vault.coder.util.VaultReferences._

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
	def apply(data: CombinationData, parentRef: Reference, parentDataRef: Reference, childRef: Reference,
	          parentFactoryRef: Reference)
	         (implicit setup: VaultProjectSetup, codec: Codec, naming: NamingRules) =
	{
		val combinedClassName = data.name.className
		val combinedClassType = ScalaType.basic(combinedClassName)
		
		val extender: Extension = Reference.flow.extender(parentDataRef)
		val factory: Extension = parentFactoryRef(combinedClassType)
		// Extends the HasId trait only if Vault references are enabled
		val parents = {
			if (setup.modelCanReferToDB)
				Vector[Extension](extender, vault.hasId, factory)
			else
				Vector(extender, factory)
		}
		
		val parentName = data.parentName
		val constructorParams = data.combinationType.applyParamsWith(parentName, data.childName,
			parentRef, childRef)
		
		// Generates the withX functions in order to conform to the parent factory trait
		val parentPropName = parentName.prop
		val withFunctions = data.parentClass.properties.map { prop =>
			val methodName = (ModelWriter.withPrefix + prop.name).function
			val paramName = prop.name.prop
			MethodDeclaration(methodName, isOverridden = true)(
				Parameter(paramName, prop.dataType.concrete.toScala))(
				s"copy($parentPropName = $parentPropName.$methodName($paramName))")
		}
		
		File(setup.combinedModelPackage/data.packageName,
			ClassDeclaration(combinedClassName,
				constructionParams = constructorParams,
				// Provides implicit access to the data model (because that's where most of the properties are)
				extensions = parents,
				properties = Vector(
					// Provides direct access to parent.id
					ComputedProperty("id", description = s"Id of this ${data.parentName} in the database")(
						s"${constructorParams.head.name}.id"),
					ComputedProperty("wrapped", isOverridden = true)(s"${constructorParams.head.name}.data")
				),
				methods = withFunctions.toSet,
				description = data.description.notEmpty
					.getOrElse(s"Combines ${data.parentName} with ${data.childName} data"),
				author = data.author, since = DeclarationDate.versionedToday, isCaseClass = true
			)
		).write().map { comboRef => CombinationReferences(parentRef, childRef, comboRef) }
	}
}
