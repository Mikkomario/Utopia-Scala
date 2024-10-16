package utopia.citadel.database.access.many.organization

import utopia.citadel.database.model.organization.{OrganizationDeletionCancellationModel, OrganizationDeletionModel}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.partial.organization.OrganizationDeletionCancellationData
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

import java.time.Instant

/**
  * 
	A common trait for access points which target multiple OrganizationDeletions or similar instances at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyOrganizationDeletionsAccessLike[+A, +Repr <: ManyModelAccess[A]] 
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
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
	  * An access point to deletions that are currently ready to actualize
	  */
	def readyToActualize = actualizingBefore(Now)
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the actualization of the targeted OrganizationDeletion instance(s)
	  * @param newActualization A new actualization to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def actualizations_=(newActualization: Instant)(implicit connection: Connection) = 
		putColumn(model.actualizationColumn, newActualization)
	
	/**
	  * @param threshold A time threshold
	  * @return An access point to these deletions that are scheduled to actualize before the specified time
	  */
	def actualizingBefore(threshold: Instant) = filter(model.actualizationColumn < threshold)
	
	/**
	  * Cancels all accessible organization deletions
	  * @param creatorId Id of the user who's cancelling these deletions (optional)
	  * @param connection Implicit DB Connection
	  * @return Inserted deletion cancellations
	  */
	def cancel(creatorId: Option[Int] = None)(implicit connection: Connection) = {
		// Reads deletion ids and creates a new cancellation for each of them
		OrganizationDeletionCancellationModel.insert(
			ids.map { deletionId => OrganizationDeletionCancellationData(deletionId, creatorId) })
	}
	
	/**
	  * Cancels all accessible organization deletions
	  * @param userId Id of the user who's cancelling these deletions
	  * @param connection Implicit DB Connection
	  * @return Inserted deletion cancellations
	  */
	def cancelBy(userId: Int)(implicit connection: Connection) = cancel(Some(userId))
	
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
	  * @param organizationIds Ids of the targeted organizations
	  * @return An access point to deletions that target any of the specified organizations
	  */
	def forAnyOfOrganizations(organizationIds: Iterable[Int]) = 
		filter(model.organizationIdColumn in organizationIds)
	
	/**
	  * Updates the organizationId of the targeted OrganizationDeletion instance(s)
	  * @param newOrganizationId A new organizationId to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def organizationIds_=(newOrganizationId: Int)(implicit connection: Connection) = 
		putColumn(model.organizationIdColumn, newOrganizationId)
	
	/**
	  * @param organizationId Id of the targeted organization
	  * @return An access point to deletions concerning that organization
	  */
	def withOrganizationId(organizationId: Int) = filter(model.withOrganizationId(organizationId).toCondition)
}

