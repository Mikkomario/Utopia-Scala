package utopia.citadel.database.access.many.organization

import utopia.citadel.database.model.organization.OrganizationDeletionModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition
import utopia.vault.sql.SqlExtensions._

import java.time.Instant

object ManyOrganizationDeletionsAccessLike
{
	private class ManyOrganizationDeletionsSubView[A](override val parent: ManyModelAccess[A],
	                                                  override val filterCondition: Condition)
		extends ManyOrganizationDeletionsAccessLike[A] with SubView
	{
		override def factory = parent.factory
		
		override protected def defaultOrdering = parent.defaultOrdering
	}
}

/**
  * A common trait for access points which target multiple OrganizationDeletions or similar instances at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyOrganizationDeletionsAccessLike[+A] extends ManyModelAccess[A] with Indexed
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
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def filter(additionalCondition: Condition): ManyOrganizationDeletionsAccessLike[A] =
		new ManyOrganizationDeletionsAccessLike.ManyOrganizationDeletionsSubView[A](this, additionalCondition)
	
	
	// OTHER	--------------------
	
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

