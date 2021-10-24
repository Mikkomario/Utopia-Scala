package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.citadel.database.model.organization.MembershipModel
import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual Memberships
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMembership extends SingleRowModelAccess[Membership] with NonDeprecatedView[Membership] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = MembershipModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = MembershipFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Membership instance
	  * @return An access point to that Membership
	  */
	def apply(id: Int) = DbSingleMembership(id)
	
	/**
	  * Starts a new organization membership. Please make sure the user is not a member of the specified organization
	  * before calling this method.
	  * @param organizationId Id of the organization where the memberhip is starting
	  * @param userId Id of the user who becomes a member
	  * @param startingRoleId Id of the role the user will have in that organization
	  * @param creatorId Id of the user who is adding the specified user to that organization
	  * @param connection Implicit DB Connection
	  * @return Newly started membership
	  */
	def start(organizationId: Int, userId: Int, startingRoleId: Int, creatorId: Int)(implicit connection: Connection) =
	{
		val membership = model.insert(MembershipData(organizationId, userId, Some(creatorId)))
		val role = apply(membership.id).assignRoleWithId(startingRoleId, creatorId)
		MembershipWithRoles(membership, Set(role))
	}
}

