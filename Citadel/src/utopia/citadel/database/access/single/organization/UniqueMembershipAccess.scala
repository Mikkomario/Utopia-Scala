package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.citadel.database.model.organization.MembershipModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Memberships.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueMembershipAccess 
	extends SingleRowModelAccess[Membership] with DistinctModelAccess[Membership, Option[Membership], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the organization the referenced user is/was a member of. None if no instance (or value) was found.
	  */
	def organizationId(implicit connection: Connection) = pullColumn(model.organizationIdColumn).int
	/**
	  * Id of the user who is/was a member of the referenced organization. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	/**
	  * Id of the user who created/started this membership. None if no instance (or value) was found.
	  */
	def creatorId(implicit connection: Connection) = pullColumn(model.creatorIdColumn).int
	/**
	  * Time when this membership started. None if no instance (or value) was found.
	  */
	def started(implicit connection: Connection) = pullColumn(model.startedColumn).instant
	
	/**
	  * Time when this membership ended (if applicable). None if no instance (or value) was found.
	  */
	def ended(implicit connection: Connection) = pullColumn(model.endedColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MembershipModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MembershipFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Ends this membership
	  * @param connection Implicit DB Connection
	  * @return Whether this membership was affected
	  */
	def end()(implicit connection: Connection) = ended = Now
	
	/**
	  * Updates the creatorId of the targeted Membership instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any Membership instance was affected
	  */
	def creatorId_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	/**
	  * Updates the ended of the targeted Membership instance(s)
	  * @param newEnded A new ended to assign
	  * @return Whether any Membership instance was affected
	  */
	def ended_=(newEnded: Instant)(implicit connection: Connection) = putColumn(model.endedColumn, newEnded)
	/**
	  * Updates the organizationId of the targeted Membership instance(s)
	  * @param newOrganizationId A new organizationId to assign
	  * @return Whether any Membership instance was affected
	  */
	def organizationId_=(newOrganizationId: Int)(implicit connection: Connection) = 
		putColumn(model.organizationIdColumn, newOrganizationId)
	/**
	  * Updates the started of the targeted Membership instance(s)
	  * @param newStarted A new started to assign
	  * @return Whether any Membership instance was affected
	  */
	def started_=(newStarted: Instant)(implicit connection: Connection) = 
		putColumn(model.startedColumn, newStarted)
	/**
	  * Updates the userId of the targeted Membership instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any Membership instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

