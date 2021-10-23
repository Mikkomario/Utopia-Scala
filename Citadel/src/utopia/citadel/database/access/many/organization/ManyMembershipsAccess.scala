package utopia.citadel.database.access.many.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.citadel.database.model.organization.MembershipModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyMembershipsAccess
{
	// NESTED	--------------------
	
	private class ManyMembershipsSubView(override val parent: ManyRowModelAccess[Membership], 
		override val filterCondition: Condition) 
		extends ManyMembershipsAccess with SubView
}

/**
  * A common trait for access points which target multiple Memberships at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyMembershipsAccess extends ManyRowModelAccess[Membership] with Indexed
{
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
	
	override def factory = MembershipFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManyMembershipsAccess = 
		new ManyMembershipsAccess.ManyMembershipsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
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

