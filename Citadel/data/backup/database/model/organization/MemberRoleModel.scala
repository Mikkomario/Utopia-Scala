package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.Tables
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.UserRole
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable

object MemberRoleModel extends DeprecatableAfter[MemberRoleLinkModel]
{
	// ATTRIBUTES	------------------------
	
	/**
	  * Name of the attribute that contains linked role's id
	  */
	val roleIdAttName = "roleId"
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return Column that contains the associated role id
	  */
	def roleIdColumn = table(roleIdAttName)
	
	
	// IMPLEMENTED	------------------------
	
	override def table = Tables.organizationMemberRole
	
	override def withDeprecatedAfter(deprecation: Instant) =
		apply(deprecatedAfter = Some(deprecation))
	
	
	// OTHER	----------------------------
	
	/**
	  * @param membershipId Id of targeted membership
	  * @return A model with only membership id set
	  */
	def withMembershipId(membershipId: Int) = apply(membershipId = Some(membershipId))
	
	/**
	  * @param role a user role
	  * @return A model with only role set
	  */
	@deprecated("Please use .withRoleId(Int) instead")
	def withRole(role: UserRole) = withRoleId(role.id)
	
	/**
	  * @param roleId Id of targeted user role
	  * @return A model with only role id set
	  */
	def withRoleId(roleId: Int) = apply(roleId = Some(roleId))
	
	/**
	  * Inserts a new membership-role -connection to the DB
	  * @param membershipId Id of associated organization membership
	  * @param roleId Id of the role assigned to the user in the organization
	  * @param creatorId Id of the user who created this link
	  * @param connection DB Connection (implicit)
	  * @return Id of the newly inserted link
	  */
	def insert(membershipId: Int, roleId: Int, creatorId: Int)(implicit connection: Connection) =
		apply(None, Some(membershipId), Some(roleId), Some(creatorId)).insert().getInt
}

/**
  * Links organization memberships with their roles
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.0
  */
case class MemberRoleModel(id: Option[Int] = None, membershipId: Option[Int] = None,
						   roleId: Option[Int] = None, creatorId: Option[Int] = None,
						   deprecatedAfter: Option[Instant] = None) extends Storable
{
	import MemberRoleLinkModel._
	
	override def table = MemberRoleLinkModel.table
	
	override def valueProperties = Vector("id" -> id, "membershipId" -> membershipId, roleIdAttName -> roleId,
		"creatorId" -> creatorId, "deprecatedAfter" -> deprecatedAfter)
}
