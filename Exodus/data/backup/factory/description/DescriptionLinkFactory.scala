package utopia.exodus.database.factory.description

import java.time.Instant
import utopia.exodus.database.model.description.{DescriptionLinkModel, DescriptionLinkModelFactory}
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.description.DescriptionLinkDataOld
import utopia.metropolis.model.stored.description.{Description, DescriptionLinkOld}
import utopia.vault.model.immutable.{Storable, Table}
import utopia.vault.nosql.factory.row.linked.LinkedFactory
import utopia.vault.nosql.template.Deprecatable

import scala.util.{Success, Try}

/**
  * A common trait for factories of description links
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
trait DescriptionLinkFactory[+E] extends LinkedFactory[E, Description] with Deprecatable
{
	// ABSTRACT	----------------------------------
	
	/**
	  * @return Factory used in database model construction
	  */
	def modelFactory: DescriptionLinkModelFactory[_]
	
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
	
	override def apply(model: Model, child: Description) =
		table.requirementDeclaration.validate(model).toTry.flatMap { valid =>
			apply(valid("id").getInt, valid(modelFactory.targetIdAttName).getInt, child, valid("created").getInstant)
		}
}

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
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param table Targeted table
	  * @param targetIdAttName Name of the attribute that contains the link to the described item
	  * @return A new factory for reading description links
	  */
	def apply(table: Table, targetIdAttName: String): DescriptionLinkFactory[DescriptionLinkOld] =
		apply(DescriptionLinkModelFactory(table, targetIdAttName))
	
	/**
	  * Wraps a description link model factory into a description link factory
	  * @param modelFactory model to wrap
	  * @return A new link factory
	  */
	def apply(modelFactory: DescriptionLinkModelFactory[Storable]): DescriptionLinkFactory[DescriptionLinkOld] =
		DescriptionLinkFactoryImplementation(modelFactory)
	
	
	// NESTED	----------------------------------
	
	private case class DescriptionLinkFactoryImplementation(modelFactory: DescriptionLinkModelFactory[Storable])
		extends DescriptionLinkFactory[DescriptionLinkOld]
	{
		override protected def apply(id: Int, targetId: Int, description: Description, created: Instant) =
			Success(DescriptionLinkOld(id, DescriptionLinkDataOld(targetId, description, created)))
	}
}
