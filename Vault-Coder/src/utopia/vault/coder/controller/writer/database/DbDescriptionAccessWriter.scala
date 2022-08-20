package utopia.vault.coder.controller.writer.database

import utopia.vault.coder.model.data.{Class, Name, NamingRules, ProjectSetup}
import utopia.vault.coder.model.enumeration.NamingConvention.CamelCase
import utopia.vault.coder.model.scala.DeclarationDate
import utopia.vault.coder.model.scala.datatype.Reference
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.declaration.{File, ObjectDeclaration}

import scala.io.Codec

/**
  * Used for writing item description-related access points
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.2
  */
object DbDescriptionAccessWriter
{
	private val accessPrefix = Name("Db", "Db", CamelCase.capitalized)
	
	/**
	  * Writes description-related database interaction objects
	  * @param descriptionLinkClass Description link class based on which the objects are generated
	  * @param baseClassName        Name of the class to which the descriptions belong
	  * @param linkModelsRef        Reference to description link model factory access object
	  * @param descriptionFactoriesRef     Reference to linked description factory access object
	  * @param setup                Implicit project setup
	  * @param codec                Implicit codec used when writing files
	  * @return Single description access point reference + many descriptions access point reference.
	  *         Failure if file writing failed at some point.
	  */
	def apply(descriptionLinkClass: Class, baseClassName: Name, linkModelsRef: Reference,
	          descriptionFactoriesRef: Reference)
	         (implicit setup: ProjectSetup, codec: Codec, naming: NamingRules) =
	{
		val factoryPropertyName = baseClassName.prop
		
		val baseAccessProperties = Vector(
			ComputedProperty("factory", Set(descriptionFactoriesRef), isOverridden = true)(
				s"${ descriptionFactoriesRef.target }.$factoryPropertyName"),
			ComputedProperty("linkModel", Set(linkModelsRef), isOverridden = true)(
				s"${ linkModelsRef.target }.$factoryPropertyName")
		)
		// Next writes the individual description access point
		File(setup.singleAccessPackage / descriptionLinkClass.packageName,
			ObjectDeclaration((accessPrefix +: descriptionLinkClass.name).className,
				Vector(Reference.linkedDescriptionAccess),
				properties = baseAccessProperties, author = descriptionLinkClass.author,
				description = s"Used for accessing individual $baseClassName descriptions",
				since = DeclarationDate.versionedToday
			)
		).write().flatMap { singleDescriptionAccessRef =>
			// Finally writes the multiple descriptions access point
			File(setup.manyAccessPackage / descriptionLinkClass.packageName,
				ObjectDeclaration((accessPrefix +: baseClassName).pluralClassName,
					Vector(Reference.linkedDescriptionsAccess), properties = baseAccessProperties,
					author = descriptionLinkClass.author,
					description = s"Used for accessing multiple $baseClassName descriptions at a time",
					since = DeclarationDate.versionedToday
				)
			).write().map { singleDescriptionAccessRef -> _ }
		}
	}
}
