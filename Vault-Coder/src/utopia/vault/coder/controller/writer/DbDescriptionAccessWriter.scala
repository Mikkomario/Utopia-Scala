package utopia.vault.coder.controller.writer

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, ProjectSetup}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.vault.coder.model.scala.{Parameter, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration}

import scala.io.Codec

/**
  * Used for writing item description-related access points
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.2
  */
// TODO: Update this class to match changes in Citadel
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
		val linkProperty = descriptionLinkClass.properties.head
		val linkPropertyName = linkProperty.name.singular
		val factoryPropertyName = baseClassName.singular.uncapitalize
		
		val baseAccessProperties = Vector(
			ComputedProperty("model", Set(linkModelsRef),
				description = s"Model factory used when interacting with $baseClassName description links")(
				s"${linkModelsRef.target}.$factoryPropertyName"),
			ComputedProperty("factory", Set(linkFactoriesRef),
				description = s"Factory used when reading $baseClassName description links")(
				s"${linkFactoriesRef.target}.$factoryPropertyName")
		)
		val linkInputDataType = linkProperty.dataType.notNull.toScala
		// Next writes the individual description access point
		File(setup.singleAccessPackage/descriptionLinkClass.packageName,
			ObjectDeclaration(s"Db$linkClassName", properties = baseAccessProperties,
				methods = Set(
					MethodDeclaration("apply", Set(Reference.descriptionOfSingle),
						returnDescription = s"Access point to the targeted $baseClassName's individual descriptions")(
						Parameter(linkPropertyName, linkInputDataType,
							description = s"Id of the targeted $baseClassName"))(
						Reference.descriptionOfSingle.target + s"($linkPropertyName, factory, model)")
				)
			)
		).write().flatMap { singleDescriptionAccessRef =>
			val pluralLinkPropertyName = linkProperty.name.plural
			// Finally writes the multiple descriptions access point
			File(setup.manyAccessPackage/descriptionLinkClass.packageName,
				ObjectDeclaration(s"Db${descriptionLinkClass.name.plural}",
					properties = baseAccessProperties :+
						ImmutableValue("all", Set(Reference.descriptionsOfAll),
							description = s"An access point to the descriptions of all ${
								baseClassName.plural} at once")(
							Reference.descriptionsOfAll.target + "(factory, model)"),
					methods = Set(
						MethodDeclaration("apply", Set(Reference.descriptionsOfSingle),
							returnDescription = s"An access point to that $baseClassName's descriptions")(
							Parameter(linkPropertyName, linkInputDataType,
								description = s"Id of the targeted $baseClassName"))(
							Reference.descriptionsOfSingle.target + s"($linkPropertyName, factory, model)"),
						MethodDeclaration("apply", Set(Reference.descriptionsOfMany),
							returnDescription = s"An access point to descriptions of the targeted ${
								baseClassName.plural}")(
							Parameter(pluralLinkPropertyName, ScalaType.set(linkInputDataType),
								description = s"Ids of the ${baseClassName.plural} to target"))(
							Reference.descriptionsOfMany.target + s"($pluralLinkPropertyName, factory, model)")
					)
				)
			).write().map { singleDescriptionAccessRef -> _ }
		}
	}
}
