package utopia.vault.coder.controller.writer

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, Name, ProjectSetup}
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.{ComputedProperty, ImmutableValue}
import utopia.vault.coder.model.scala.{Extension, Parameter, Reference, ScalaType}
import utopia.vault.coder.model.scala.declaration.{File, MethodDeclaration, ObjectDeclaration}

import scala.io.Codec

/**
  * Used for writing item description-related access points
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.2
  */
object DbDescriptionInteractionsWriter
{
	/**
	  * Writes description-related database interaction objects
	  * @param descriptionLinkClass Description link class based on which the objects are generated
	  * @param tablesRef Reference to the project tables
	  * @param baseClassName Name of the class to which the descriptions belong
	  * @param setup Implicit project setup
	  * @param codec Implicit codec used when writing files
	  * @return Description link model (factory) reference + description link factory reference +
	  *         single description access point reference + many descriptions access point reference.
	  *         Failure if file writing failed at some point.
	  */
	def apply(descriptionLinkClass: Class, tablesRef: Reference, baseClassName: Name)
	         (implicit setup: ProjectSetup, codec: Codec) =
	{
		val linkClassName = descriptionLinkClass.name.singular
		val linkProperty = descriptionLinkClass.properties.head
		val linkPropertyName = linkProperty.name.singular
		
		// FIXME: The model and factory can't be objects but must be values
		
		// Writes the link model factory object (edit) first
		File(setup.dbModelPackage/descriptionLinkClass.packageName,
			ObjectDeclaration(linkClassName + "LinkModel",
				Vector(Extension(Reference.descriptionLinkModelFactory)
					.withConstructor(CodePiece(s"${tablesRef.target}.${linkClassName.uncapitalize}",
						Set(tablesRef)), linkPropertyName.quoted))
			)
		).write().flatMap { linkModelFactoryRef =>
			// Next writes the link factory object (pull)
			File(setup.factoryPackage/descriptionLinkClass.packageName,
				ObjectDeclaration(linkClassName + "LinkFactory",
					Vector(Extension(Reference.descriptionLinkFactory)
						.withConstructor(CodePiece(linkModelFactoryRef.target, Set(linkModelFactoryRef))))
				)
			).write().flatMap { linkFactoryRef =>
				val baseAccessProperties = Vector(
					ComputedProperty("model", Set(linkModelFactoryRef),
						description = s"Model factory used when interacting with $baseClassName description links")(
						linkModelFactoryRef.target),
					ComputedProperty("factory", Set(linkFactoryRef),
						description = s"Factory used when reading $baseClassName description links")(
						linkFactoryRef.target)
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
					).write().map { manyDescriptionsAccessRef =>
						(linkModelFactoryRef, linkFactoryRef, singleDescriptionAccessRef, manyDescriptionsAccessRef)
					}
				}
			}
		}
	}
}
