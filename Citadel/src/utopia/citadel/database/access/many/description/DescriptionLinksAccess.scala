package utopia.citadel.database.access.many.description

import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.DescriptionLinkModelFactory
import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object DescriptionLinksAccess
{
	// OTHER    -----------------------------------
	
	/**
	  * @param factory Factory used for reading description links
	  * @param linkModel Factory used for constructing description link db interaction models
	  * @param condition Condition to apply to searches (optional)
	  * @return A new access point to those description links
	  */
	def apply(factory: DescriptionLinkFactory, linkModel: DescriptionLinkModelFactory,
	          condition: Option[Condition] = None): DescriptionLinksAccess =
		new SimpleDescriptionLinksAccess(factory, linkModel, condition)
	
	/**
	  * @param table A description link table
	  * @return An access point to description links in that table
	  */
	def apply(table: DescriptionLinkTable): DescriptionLinksAccess =
		apply(DescriptionLinkFactory(table), DescriptionLinkModelFactory(table))
	
	
	// NESTED   -----------------------------------
	
	private class SimpleDescriptionLinksAccess(override val factory: DescriptionLinkFactory,
	                                           override val linkModel: DescriptionLinkModelFactory,
	                                           override val accessCondition: Option[Condition])
		extends DescriptionLinksAccess
}

/**
  * A common trait for access points which return multiple description links at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
trait DescriptionLinksAccess
	extends ManyRowModelAccess[DescriptionLink] with Indexed with FilterableView[DescriptionLinksAccess]
{
	// ABSTRACT -----------------------------------
	
	override def factory: DescriptionLinkFactory
	
	/**
	  * @return Model used for interacting with description links
	  */
	protected def linkModel: DescriptionLinkModelFactory
	
	
	// COMPUTED -----------------------------------
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the accessible description links
	  */
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { _.int }
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the accessible description link targets
	  */
	def targetIds(implicit connection: Connection) =
		pullColumn(linkModel.targetIdColumn).flatMap { _.int }
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the accessible descriptions
	  */
	def descriptionIds(implicit connection: Connection) =
		pullColumn(linkModel.descriptionIdColumn).flatMap { _.int }
	
	
	// IMPLEMENTED  -------------------------------
	
	override protected def self = this
	
	override def apply(condition: Condition): DescriptionLinksAccess =
		DescriptionLinksAccess(factory, linkModel, Some(condition))
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param targetId Description target id
	  * @return An access point to description links connecting to that target
	  */
	def withTargetId(targetId: Int) = filter(linkModel.withTargetId(targetId).toCondition)
	
	/**
	  * @param targetIds Description target ids
	  * @return An access point to description links concerning those ids
	  */
	def withTargetIds(targetIds: Iterable[Int]) =
		filter(linkModel.targetIdColumn in targetIds)
	/**
	  * @param descriptionIds Description ids
	  * @return An access point to links from those descriptions
	  */
	def withDescriptionIds(descriptionIds: Iterable[Int]) =
		filter(linkModel.descriptionIdColumn in descriptionIds)
}
