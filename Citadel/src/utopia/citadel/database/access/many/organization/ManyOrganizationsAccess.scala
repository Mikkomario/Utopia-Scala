package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.description.{DbOrganizationDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.organization.OrganizationFactory
import utopia.citadel.database.model.organization.OrganizationModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.organization.DescribedOrganization
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyOrganizationsAccess extends ViewFactory[ManyOrganizationsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyOrganizationsAccess = 
		 new _ManyOrganizationsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyOrganizationsAccess(condition: Condition) extends ManyOrganizationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple Organizations at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyOrganizationsAccess 
	extends ManyRowModelAccess[Organization] with ManyDescribedAccess[Organization, DescribedOrganization] 
		with FilterableView[ManyOrganizationsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * creatorIds of the accessible Organizations
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible Organizations
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationFactory
	
	override protected def describedFactory = DescribedOrganization
	
	override protected def manyDescriptionsAccess = DbOrganizationDescriptions
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyOrganizationsAccess = ManyOrganizationsAccess(condition)
	
	override def idOf(item: Organization) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Organization instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Organization instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted Organization instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any Organization instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
}

