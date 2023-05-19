package utopia.vault.coder.controller.writer.database

import utopia.coder.model.data.NamingRules
import utopia.coder.model.scala.DeclarationDate
import utopia.coder.model.scala.datatype.{Extension, Reference}
import utopia.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.coder.model.scala.declaration.{File, ObjectDeclaration}
import utopia.vault.coder.model.data.{CombinationData, CombinationReferences, VaultProjectSetup}
import utopia.vault.coder.util.VaultReferences.Vault._

import scala.io.Codec

/**
  * Used for writing a factory for reading combined model data
  * @author Mikko Hilpinen
  * @since 14.10.2021, v1.2
  */
object CombinedFactoryWriter
{
	/**
	  * Writes a combined model factory object file
	  * @param data Combination instructions / data
	  * @param references Combination-related references
	  * @param parentFactoryRef Reference to the parent class factory
	  * @param childFactoryRef Reference to the child class factory
	  * @param setup Implicit project setup
	  * @param codec Implicit codec used when writing the file
	  * @return Reference to the factory object. Failure if file writing failed
	  */
	def apply(data: CombinationData, references: CombinationReferences,
	          parentFactoryRef: Reference, childFactoryRef: Reference)
	         (implicit setup: VaultProjectSetup, codec: Codec, naming: NamingRules) =
	{
		// Some factory implementations require the isAlwaysLinked -property
		val linkingProperty = {
			if (data.combinationType.shouldSpecifyWhetherAlwaysLinked)
				Some(ComputedProperty("isAlwaysLinked", isOverridden = true)(data.isAlwaysLinked.toString))
			else
				None
		}
		val parentDeprecates = data.parentClass.isDeprecatable
		val childDeprecates = data.childClass.isDeprecatable
		// If either parent or child type supports deprecation, so does this factory
		val deprecation = {
			if (parentDeprecates || childDeprecates) {
				val condition = {
					if (parentDeprecates) {
						if (childDeprecates)
							"parentFactory.nonDeprecatedCondition && childFactory.nonDeprecatedCondition"
						else
							"parentFactory.nonDeprecatedCondition"
					}
					else
						"childFactory.nonDeprecatedCondition"
				}
				Some(Extension(deprecatable) ->
					ComputedProperty("nonDeprecatedCondition", isOverridden = true)(condition))
			}
			else
				None
		}
		// If the parent type uses row time indexing, and this is not a one-to-many combination,
		// Utilizes the same indexing
		val creation = {
			if (data.combinationType.isOneToOne && data.parentClass.recordsIndexedCreationTime) {
				val extension: Extension = fromRowFactoryWithTimestamps(references.combined)
				val function = ComputedProperty("creationTimePropertyName", Set(parentFactoryRef),
					isOverridden = true)(s"${parentFactoryRef.target}.creationTimePropertyName")
				Some(extension -> function)
			}
			else
				None
		}
		
		File(setup.factoryPackage/data.packageName,
			ObjectDeclaration((data.name + FactoryWriter.classNameSuffix).className,
				Vector(data.combinationType.extensionWith(references)) ++
					creation.map { _._1 } ++ deprecation.map { _._1 },
				properties = Vector(
					ComputedProperty("parentFactory", Set(parentFactoryRef), isOverridden = true)(
						parentFactoryRef.target),
					ComputedProperty("childFactory", Set(childFactoryRef), isOverridden = true)(childFactoryRef.target)
				) ++ linkingProperty ++ creation.map { _._2 } ++ deprecation.map { _._2 },
				methods = Set(data.combinationType.factoryApplyMethodWith(data.parentName, data.childName, references)),
				description = s"Used for reading ${data.name.pluralDoc} from the database", author = data.author,
				since = DeclarationDate.versionedToday
			)
		).write()
	}
}
