package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.MemberRoleLinkFactory
import utopia.citadel.database.model.organization.MemberRoleLinkModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct MemberRoles.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueMemberRoleLinkAccess
	extends SingleRowModelAccess[MemberRoleLink] with DistinctModelAccess[MemberRoleLink, Option[MemberRoleLink], Value]
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the membership / member that has the referenced role. None if no instance (or value) was found.
	  */
	def membershipId(implicit connection: Connection) = pullColumn(model.membershipIdColumn).int
	
	/**
	  * Id of role the referenced member has. None if no instance (or value) was found.
	  */
	def roleId(implicit connection: Connection) = pullColumn(model.roleIdColumn).int
	
	/**
	  * Id of the user who added this role to the membership, 
		if known. None if no instance (or value) was found.
	  */
	def creatorId(implicit connection: Connection) = pullColumn(model.creatorIdColumn).int
	
	/**
	  * Time when this role was added for the organization member. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Time when this MemberRole became deprecated. None while this MemberRole is still valid.. None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MemberRoleLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted MemberRole instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted MemberRole instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def creatorId_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	
	/**
	  * Updates the deprecatedAfter of the targeted MemberRole instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the membershipId of the targeted MemberRole instance(s)
	  * @param newMembershipId A new membershipId to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def membershipId_=(newMembershipId: Int)(implicit connection: Connection) = 
		putColumn(model.membershipIdColumn, newMembershipId)
	
	/**
	  * Updates the roleId of the targeted MemberRole instance(s)
	  * @param newRoleId A new roleId to assign
	  * @return Whether any MemberRole instance was affected
	  */
	def roleId_=(newRoleId: Int)(implicit connection: Connection) = putColumn(model.roleIdColumn, newRoleId)
}

