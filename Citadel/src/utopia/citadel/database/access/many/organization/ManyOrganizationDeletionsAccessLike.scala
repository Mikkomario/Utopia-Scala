package utopia.citadel.database.access.many.organization

import utopia.citadel.database.model.organization.OrganizationDeletionModel
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

/**
  * A common trait for access points which target multiple OrganizationDeletions or similar instances at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyOrganizationDeletionsAccessLike[+A, +Repr <: ManyModelAccess[A]] extends ManyModelAccess[A] with Indexed
{
	// ABSTRACT --------------------
	
	protected def _filter(condition: Condition): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * organizationIds of the accessible OrganizationDeletions
	  */
	def organizationIds(implicit connection: Connection) = 
		pullColumn(model.organizationIdColumn).flatMap { value => value.int }
	/**
	  * actualizations of the accessible OrganizationDeletions
	  */
	def actualizations(implicit connection: Connection) = 
		pullColumn(model.actualizationColumn).flatMap { value => value.instant }
	/**
	  * creatorIds of the accessible OrganizationDeletions
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	/**
	  * creationTimes of the accessible OrganizationDeletions
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionModel
	
	/**
	  * @return An access point to deletions that are currently ready to actualize
	  */
	def readyToActualize = actualizingBefore(Now)
	
	
	// IMPLEMENTED	--------------------
	
	override def filter(additionalCondition: Condition) = _filter(additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param organizationId Id of the targeted organization
	  * @return An access point to deletions concerning that organization
	  */
	def withOrganizationId(organizationId: Int) =
		filter(model.withOrganizationId(organizationId).toCondition)
	/**
	  * @param threshold A time threshold
	  * @return An access point to these deletions that are scheduled to actualize before the specified time
	  */
	def actualizingBefore(threshold: Instant) =
		filter(model.actualizationColumn < threshold)
	
	/**
	  * Updates the actualization of the targeted OrganizationDeletion instance(s)
	  * @param newActualization A new actualization to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def actualizations_=(newActualization: Instant)(implicit connection: Connection) = 
		putColumn(model.actualizationColumn, newActualization)
	/**
	  * Updates the created of the targeted OrganizationDeletion instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the creatorId of the targeted OrganizationDeletion instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	/**
	  * Updates the organizationId of the targeted OrganizationDeletion instance(s)
	  * @param newOrganizationId A new organizationId to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def organizationIds_=(newOrganizationId: Int)(implicit connection: Connection) = 
		putColumn(model.organizationIdColumn, newOrganizationId)
}

