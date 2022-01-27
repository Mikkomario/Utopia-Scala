package utopia.vault.coder.controller.writer.database

import utopia.vault.coder.model.data.{CombinationData, CombinationReferences, ProjectSetup}
import utopia.vault.coder.model.scala.{DeclarationDate, Extension, Reference}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{File, ObjectDeclaration}

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
	         (implicit setup: ProjectSetup, codec: Codec) =
	{
		// Some factory implementations require the isAlwaysLinked -property
		val linkingProperty =
		{
			if (data.combinationType.shouldSpecifyWhetherAlwaysLinked)
				Some(ComputedProperty("isAlwaysLinked", isOverridden = true)(data.isAlwaysLinked.toString))
			else
				None
		}
		val parentDeprecates = data.parentClass.isDeprecatable
		val childDeprecates = data.childClass.isDeprecatable
		// If either parent or child type supports deprecation, so does this factory
		val deprecation =
		{
			if (parentDeprecates || childDeprecates)
			{
				val condition =
				{
					if (parentDeprecates)
					{
						if (childDeprecates)
							"parentFactory.nonDeprecatedCondition && childFactory.nonDeprecatedCondition"
						else
							"parentFactory.nonDeprecatedCondition"
					}
					else
						"childFactory.nonDeprecatedCondition"
				}
				Some(Extension(Reference.deprecatable) ->
					ComputedProperty("nonDeprecatedCondition", isOverridden = true)(condition))
			}
			else
				None
		}
		
		File(setup.factoryPackage/data.packageName,
			ObjectDeclaration(s"${data.name.singular}Factory",
				Vector(data.combinationType.extensionWith(references)) ++ deprecation.map { _._1 },
				properties = Vector(
					ComputedProperty("parentFactory", Set(parentFactoryRef), isOverridden = true)(
						parentFactoryRef.target),
					ComputedProperty("childFactory", Set(childFactoryRef), isOverridden = true)(childFactoryRef.target)
				) ++ linkingProperty ++ deprecation.map { _._2 },
				methods = Set(data.combinationType.factoryApplyMethodWith(data.parentName, data.childName, references)),
				description = s"Used for reading ${data.name.plural} from the database", author = data.author,
				since = DeclarationDate.versionedToday
			)
		).write()
	}
}
