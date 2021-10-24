package utopia.citadel.database.access.many.organization

import utopia.citadel.database.model.organization.MembershipModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

import java.time.Instant

/**
  * A common trait for access points which target multiple Memberships or like instances at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyMembershipsAccessLike[+A, +Repr <: ManyModelAccess[A]]
	extends ManyModelAccess[A] with Indexed
{
	// ABSTRACT --------------------
	
	protected def _filter(condition: Condition): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * organizationIds of the accessible Memberships
	  */
	def organizationIds(implicit connection: Connection) = 
		pullColumn(model.organizationIdColumn).flatMap { value => value.int }
	/**
	  * userIds of the accessible Memberships
	  */
	def userIds(implicit connection: Connection) = pullColumn(model.userIdColumn)
		.flatMap { value => value.int }
	/**
	  * creatorIds of the accessible Memberships
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	/**
	  * startTimes of the accessible Memberships
	  */
	def startTimes(implicit connection: Connection) = 
		pullColumn(model.startedColumn).flatMap { value => value.instant }
	/**
	  * endTimes of the accessible Memberships
	  */
	def endTimes(implicit connection: Connection) = 
		pullColumn(model.endedColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MembershipModel
	
	
	// IMPLEMENTED	--------------------
	
	override def filter(additionalCondition: Condition) = _filter(additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param organizationId Id of the targeted organization
	  * @return An access point to memberships within that organization
	  */
	def inOrganizationWithId(organizationId: Int) =
		filter(model.withOrganizationId(organizationId).toCondition)
	
	/**
	  * Updates the creatorId of the targeted Membership instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any Membership instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	/**
	  * Updates the ended of the targeted Membership instance(s)
	  * @param newEnded A new ended to assign
	  * @return Whether any Membership instance was affected
	  */
	def endTimes_=(newEnded: Instant)(implicit connection: Connection) = putColumn(model.endedColumn, 
		newEnded)
	/**
	  * Updates the organizationId of the targeted Membership instance(s)
	  * @param newOrganizationId A new organizationId to assign
	  * @return Whether any Membership instance was affected
	  */
	def organizationIds_=(newOrganizationId: Int)(implicit connection: Connection) = 
		putColumn(model.organizationIdColumn, newOrganizationId)
	/**
	  * Updates the started of the targeted Membership instance(s)
	  * @param newStarted A new started to assign
	  * @return Whether any Membership instance was affected
	  */
	def startTimes_=(newStarted: Instant)(implicit connection: Connection) = 
		putColumn(model.startedColumn, newStarted)
	/**
	  * Updates the userId of the targeted Membership instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any Membership instance was affected
	  */
	def userIds_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

