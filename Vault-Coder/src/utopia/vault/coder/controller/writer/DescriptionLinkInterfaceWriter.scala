package utopia.vault.coder.controller.writer

import utopia.flow.util.StringExtensions._
import utopia.vault.coder.model.data.{Class, ProjectSetup}
import utopia.vault.coder.model.scala.Reference
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ImmutableValue
import utopia.vault.coder.model.scala.declaration.{File, ObjectDeclaration}

import scala.io.Codec
import scala.util.Success

/**
  * Writes objects that provide access to different description link model factories and description link factories
  * @author Mikko Hilpinen
  * @since 9.10.2021, v1.2
  */
object DescriptionLinkInterfaceWriter
{
	/**
	  * Writes description link model factory and description link factory access objects
	  * @param classes Project classes
	  * @param tablesRef Reference to the tables object
	  * @param setup Implicit project setup
	  * @param codec Implicit codec to use when writing files
	  * @return Reference to the link model factory + reference to the link factory. Failure if file writing failed.
	  *         None if there weren't any classes that used descriptions.
	  */
	def apply(classes: Vector[Class], tablesRef: Reference)(implicit setup: ProjectSetup, codec: Codec) =
	{
		val targets = classes.flatMap { c => c.descriptionLinkClass.map { dc => (c, dc, dc.properties.head) } }
			.sortBy { _._1.name.singular }
		if (targets.nonEmpty)
		{
			val projectPrefix = setup.projectPackage.parts.last.capitalize
			// Contains a property for each described class
			val modelProps = targets.map { case (parent, desc, linkProp) =>
				ImmutableValue(parent.name.singular.uncapitalize,
					Set(tablesRef, Reference.descriptionLinkModelFactory),
					description = s"Database interaction model factory for ${
						parent.name.singular} description links")(
					s"DescriptionLinkModelFactory(${tablesRef.target}.${desc.name.singular.uncapitalize}, ${
						linkProp.name.singular.quoted})")
			}
			File(setup.dbModelPackage,
				ObjectDeclaration(projectPrefix + "DescriptionLinkModel", properties = modelProps)
			).write().flatMap { modelsRef =>
				File(setup.factoryPackage,
					ObjectDeclaration(projectPrefix + "DescriptionLinkFactory",
						// Contains a property matching each link model factory property
						properties = modelProps.map { modelProp =>
							ImmutableValue(modelProp.name, Set(modelsRef, Reference.descriptionLinkFactory))(
								s"DescriptionLinkFactory(${modelsRef.target}.${modelProp.name})")
						}
					)
				).write().map { fRef => Some(modelsRef -> fRef) }
			}
		}
		else
			Success(None)
	}
}
