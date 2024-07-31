package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.OrganizationDeletionCancellationFactory
import utopia.citadel.database.model.organization.OrganizationDeletionCancellationModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.organization.OrganizationDeletionCancellation
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyOrganizationDeletionCancellationsAccess 
	extends ViewFactory[ManyOrganizationDeletionCancellationsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyOrganizationDeletionCancellationsAccess = 
		new _ManyOrganizationDeletionCancellationsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyOrganizationDeletionCancellationsAccess(condition: Condition) 
		extends ManyOrganizationDeletionCancellationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple OrganizationDeletionCancellations at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyOrganizationDeletionCancellationsAccess 
	extends ManyRowModelAccess[OrganizationDeletionCancellation] with Indexed 
		with FilterableView[ManyOrganizationDeletionCancellationsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * deletionIds of the accessible OrganizationDeletionCancellations
	  */
	def deletionIds(implicit connection: Connection) = 
		pullColumn(model.deletionIdColumn).flatMap { value => value.int }
	
	/**
	  * creatorIds of the accessible OrganizationDeletionCancellations
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible OrganizationDeletionCancellations
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionCancellationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionCancellationFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyOrganizationDeletionCancellationsAccess = 
		ManyOrganizationDeletionCancellationsAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	
	/**
	  * Updates the deletionId of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newDeletionId A new deletionId to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def deletionIds_=(newDeletionId: Int)(implicit connection: Connection) = 
		putColumn(model.deletionIdColumn, newDeletionId)
}

