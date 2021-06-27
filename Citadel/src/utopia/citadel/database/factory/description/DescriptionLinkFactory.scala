package utopia.citadel.database.factory.description

import java.time.Instant

import utopia.citadel.database.model.description.{DescriptionLinkModel, DescriptionLinkModelFactory}
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.description.DescriptionLinkData
import utopia.metropolis.model.stored.description.{Description, DescriptionLink}
import utopia.vault.model.immutable.{Storable, Table}
import utopia.vault.nosql.factory.{Deprecatable, LinkedFactory}

import scala.util.{Success, Try}

object DescriptionLinkFactory
{
	// ATTRIBUTES	------------------------------
	
	/**
	  * Description role description factory
	  */
	lazy val descriptionRole = apply(DescriptionLinkModel.descriptionRole)
	/**
	  * Device description factory
	  */
	lazy val device = apply(DescriptionLinkModel.device)
	/**
	  * Organization description factory
	  */
	lazy val organization = apply(DescriptionLinkModel.organization)
	/**
	  * Role description factory
	  */
	lazy val userRole = apply(DescriptionLinkModel.userRole)
	/**
	  * Task description factory
	  */
	lazy val task = apply(DescriptionLinkModel.task)
	/**
	  * Language description factory
	  */
	lazy val language = apply(DescriptionLinkModel.language)
	/**
	  * Language familiarity description factory
	  */
	lazy val languageFamiliarity = apply(DescriptionLinkModel.languageFamiliarity)
	
	
	// COMPUTED ----------------------------------
	
	/**
	  * @return All description link factory implementations in this module
	  */
	def defaultImplementations = Vector(
		descriptionRole, device, organization, userRole, task, language, languageFamiliarity
	)
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param table Targeted table
	  * @param targetIdAttName Name of the attribute that contains the link to the described item
	  * @return A new factory for reading description links
	  */
	def apply(table: Table, targetIdAttName: String): DescriptionLinkFactory[DescriptionLink] =
		apply(DescriptionLinkModelFactory(table, targetIdAttName))
	
	/**
	  * Wraps a description link model factory into a description link factory
	  * @param modelFactory model to wrap
	  * @return A new link factory
	  */
	def apply(modelFactory: DescriptionLinkModelFactory[Storable]): DescriptionLinkFactory[DescriptionLink] =
		DescriptionLinkFactoryImplementation(modelFactory)
	
	
	// NESTED	----------------------------------
	
	private case class DescriptionLinkFactoryImplementation(modelFactory: DescriptionLinkModelFactory[Storable])
		extends DescriptionLinkFactory[DescriptionLink]
	{
		override protected def apply(id: Int, targetId: Int, description: Description, created: Instant) =
			Success(DescriptionLink(id, DescriptionLinkData(targetId, description, created)))
	}
}

/**
  * A common trait for factories of description links
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.0
  */
trait DescriptionLinkFactory[+E] extends LinkedFactory[E, Description] with Deprecatable
{
	// ABSTRACT	----------------------------------
	
	/**
	  * @return Factory used in database model construction
	  */
	def modelFactory: DescriptionLinkModelFactory[Storable]
	
	/**
	  * Creates a new existing model based on specified data
	  * @param id Link id
	  * @param targetId Description target id
	  * @param description Description from DB
	  * @param created Creation time of the link
	  * @return A new existing desription link model
	  */
	protected def apply(id: Int, targetId: Int, description: Description, created: Instant): Try[E]
	
	
	// IMPLEMENTED	------------------------------
	
	override def table = modelFactory.table
	
	override def nonDeprecatedCondition = table("deprecatedAfter").isNull
	
	override def childFactory = DescriptionFactory
	
	override def apply(model: Model[Constant], child: Description) =
		table.requirementDeclaration.validate(model).toTry.flatMap { valid =>
			apply(valid("id").getInt, valid(modelFactory.targetIdAttName).getInt, child, valid("created").getInstant)
		}
}
