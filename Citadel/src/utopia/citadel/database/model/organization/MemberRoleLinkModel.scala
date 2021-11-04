package utopia.citadel.database.model.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.MemberRoleLinkFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.MemberRoleLinkData
import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter

/**
  * Used for constructing MemberRoleModel instances and for inserting MemberRoles to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object MemberRoleLinkModel
	extends DataInserter[MemberRoleLinkModel, MemberRoleLink, MemberRoleLinkData] with DeprecatableAfter[MemberRoleLinkModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains MemberRole membershipId
	  */
	val membershipIdAttName = "membershipId"
	
	/**
	  * Name of the property that contains MemberRole roleId
	  */
	val roleIdAttName = "roleId"
	
	/**
	  * Name of the property that contains MemberRole creatorId
	  */
	val creatorIdAttName = "creatorId"
	
	/**
	  * Name of the property that contains MemberRole created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains MemberRole deprecatedAfter
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains MemberRole membershipId
	  */
	def membershipIdColumn = table(membershipIdAttName)
	
	/**
	  * Column that contains MemberRole roleId
	  */
	def roleIdColumn = table(roleIdAttName)
	
	/**
	  * Column that contains MemberRole creatorId
	  */
	def creatorIdColumn = table(creatorIdAttName)
	
	/**
	  * Column that contains MemberRole created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains MemberRole deprecatedAfter
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = MemberRoleLinkFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: MemberRoleLinkData) =
		apply(None, Some(data.membershipId), Some(data.roleId), data.creatorId, Some(data.created),
			data.deprecatedAfter)
	
	override def complete(id: Value, data: MemberRoleLinkData) = MemberRoleLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this role was added for the organization member
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param creatorId Id of the user who added this role to the membership, if known
	  * @return A model containing only the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = apply(creatorId = Some(creatorId))
	
	/**
	  * @param deprecatedAfter Time when this MemberRole became deprecated. None while this MemberRole is still valid.
	  * @return A model containing only the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param id A MemberRole id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param membershipId Id of the membership / member that has the referenced role
	  * @return A model containing only the specified membershipId
	  */
	def withMembershipId(membershipId: Int) = apply(membershipId = Some(membershipId))
	
	/**
	  * @param roleId Id of role the referenced member has
	  * @return A model containing only the specified roleId
	  */
	def withRoleId(roleId: Int) = apply(roleId = Some(roleId))
}

/**
  * Used for interacting with MemberRoles in the database
  * @param id MemberRole database id
  * @param membershipId Id of the membership / member that has the referenced role
  * @param roleId Id of role the referenced member has
  * @param creatorId Id of the user who added this role to the membership, if known
  * @param created Time when this role was added for the organization member
  * @param deprecatedAfter Time when this MemberRole became deprecated. None while this MemberRole is still valid.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class MemberRoleLinkModel(id: Option[Int] = None, membershipId: Option[Int] = None,
                               roleId: Option[Int] = None, creatorId: Option[Int] = None, created: Option[Instant] = None,
                               deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[MemberRoleLink]
{
	// IMPLEMENTED	--------------------
	
	override def factory = MemberRoleLinkModel.factory
	
	override def valueProperties =
	{
		import MemberRoleLinkModel._
		Vector("id" -> id, membershipIdAttName -> membershipId, roleIdAttName -> roleId, 
			creatorIdAttName -> creatorId, createdAttName -> created, 
			deprecatedAfterAttName -> deprecatedAfter)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param creatorId A new creatorId
	  * @return A new copy of this model with the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = copy(creatorId = Some(creatorId))
	
	/**
	  * @param deprecatedAfter A new deprecatedAfter
	  * @return A new copy of this model with the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param membershipId A new membershipId
	  * @return A new copy of this model with the specified membershipId
	  */
	def withMembershipId(membershipId: Int) = copy(membershipId = Some(membershipId))
	
	/**
	  * @param roleId A new roleId
	  * @return A new copy of this model with the specified roleId
	  */
	def withRoleId(roleId: Int) = copy(roleId = Some(roleId))
}

