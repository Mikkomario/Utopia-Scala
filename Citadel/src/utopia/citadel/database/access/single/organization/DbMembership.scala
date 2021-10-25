package utopia.citadel.database.access.single.organization

import utopia.citadel.database.access.many.organization.DbMemberships
import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.citadel.database.model.organization.MembershipModel
import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

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
	  * @param organizationId Id of the linked organization
	  * @param userId Id of the linked user
	  * @return An access point to the link (membership) between these two entities
	  */
	def betweenOrganizationAndUser(organizationId: Int, userId: Int) =
		new DbSingleMembershipLink(organizationId, userId)
	
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
	
	
	// NESTED   -------------------------------
	
	class DbSingleMembershipLink(organizationId: Int, userId: Int) extends UniqueMembershipAccess with SubView
	{
		// COMPUTED ---------------------------
		
		/**
		  * @return An access point to this link + any possible historical links between this organization and user
		  */
		def withHistory =
			DbMemberships.withHistory.betweenOrganizationAndUser(organizationId, userId)
		
		
		// IMPLEMENTED  -----------------------
		
		override protected def parent = DbMembership
		override protected def defaultOrdering = None
		
		override def filterCondition = model.withOrganizationId(organizationId).withUserId(userId).toCondition
		
		
		// OTHER    ---------------------------
		
		/**
		  * Starts a new membership between this organization and user
		  * @param startingRoleId Id of the user role assigned for the joining user
		  * @param creatorId Id of the user who created / authorized this link
		  * @param connection Implicit DB Connection
		  * @return New membership, including role data
		  */
		def start(startingRoleId: Int, creatorId: Int)(implicit connection: Connection) =
			DbMembership.start(organizationId, userId, startingRoleId, creatorId)
	}
}

