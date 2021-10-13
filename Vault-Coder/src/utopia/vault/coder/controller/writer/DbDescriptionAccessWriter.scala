package utopia.vault.coder.controller.writer

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, ProjectSetup}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.vault.coder.model.scala.Reference
import utopia.vault.coder.model.scala.declaration.{File, ObjectDeclaration}

import scala.io.Codec

/**
  * Used for writing item description-related access points
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.2
  */
object DbDescriptionAccessWriter
{
	/**
	  * Writes description-related database interaction objects
	  * @param descriptionLinkClass Description link class based on which the objects are generated
	  * @param baseClassName Name of the class to which the descriptions belong
	  * @param linkModelsRef Reference to description link model factory access object
	  * @param linkFactoriesRef Reference to description link factory access object
	  * @param setup Implicit project setup
	  * @param codec Implicit codec used when writing files
	  * @return Single description access point reference + many descriptions access point reference.
	  *         Failure if file writing failed at some point.
	  */
	def apply(descriptionLinkClass: Class, baseClassName: Name, linkModelsRef: Reference, linkFactoriesRef: Reference)
	         (implicit setup: ProjectSetup, codec: Codec) =
	{
		val linkClassName = descriptionLinkClass.name.singular
		val factoryPropertyName = baseClassName.singular.uncapitalize
		
		val baseAccessProperties = Vector(
			ComputedProperty("factory", Set(linkFactoriesRef), isOverridden = true)(
				s"${linkFactoriesRef.target}.$factoryPropertyName")
		)
		// Next writes the individual description access point
		File(setup.singleAccessPackage/descriptionLinkClass.packageName,
			ObjectDeclaration(s"Db$linkClassName", Vector(Reference.descriptionLinkAccess),
				properties = baseAccessProperties
			)
		).write().flatMap { singleDescriptionAccessRef =>
			// Finally writes the multiple descriptions access point
			File(setup.manyAccessPackage/descriptionLinkClass.packageName,
				ObjectDeclaration(s"Db${descriptionLinkClass.name.plural}", Vector(Reference.descriptionLinksAccess))
			).write().map { singleDescriptionAccessRef -> _ }
		}
	}
}
