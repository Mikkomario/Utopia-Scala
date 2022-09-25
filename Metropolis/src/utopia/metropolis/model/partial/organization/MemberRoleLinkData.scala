package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

object MemberRoleLinkData extends FromModelFactoryWithSchema[MemberRoleLinkData]
{
	override val schema = ModelDeclaration("membership_id" -> IntType, "role_id" -> IntType)
	
	override protected def fromValidatedModel(model: Model) =
		apply(model("membership_id"), model("role_id"), model("creator_id"), model("created"),
			model("deprecated_after"))
}

/**
  * Links an organization membership to the roles that member has within that organization
  * @param membershipId Id of the membership / member that has the referenced role
  * @param roleId Id of role the referenced member has
  * @param creatorId Id of the user who added this role to the membership, if known
  * @param created Time when this role was added for the organization member
  * @param deprecatedAfter Time when this MemberRole became deprecated. None while this MemberRole is still valid.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class MemberRoleLinkData(membershipId: Int, roleId: Int, creatorId: Option[Int] = None,
                              created: Instant = Now, deprecatedAfter: Option[Instant] = None)
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this MemberRole has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	/**
	  * Whether this MemberRole is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("membership_id" -> membershipId, "role_id" -> roleId, "creator_id" -> creatorId, 
			"created" -> created, "deprecated_after" -> deprecatedAfter))
}

