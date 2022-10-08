package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

object MembershipData extends FromModelFactoryWithSchema[MembershipData]
{
	override val schema = ModelDeclaration("organization_id" -> IntType, "user_id" -> IntType)
	
	override protected def fromValidatedModel(model: Model) = MembershipData(model("organization_id"),
		model("user_id"), model("creator_id"), model("started"), model("ended"))
}

/**
  * Lists organization members, including membership history
  * @param organizationId Id of the organization the referenced user is/was a member of
  * @param userId Id of the user who is/was a member of the referenced organization
  * @param creatorId Id of the user who created/started this membership
  * @param started Time when this membership started
  * @param ended Time when this membership ended (if applicable)
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class MembershipData(organizationId: Int, userId: Int, creatorId: Option[Int] = None, 
	started: Instant = Now, ended: Option[Instant] = None) 
	extends StyledModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this Membership has already been deprecated
	  */
	def isDeprecated = ended.isDefined
	
	/**
	  * Whether this Membership is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("organization_id" -> organizationId, "user_id" -> userId, "creator_id" -> creatorId, 
			"started" -> started, "ended" -> ended))
	
	// The simple model version expects organization and user ids to be available elsewhere and the membership
	// status to be always known also
	override def toSimpleModel = Model(Vector("inviter_id" -> creatorId, "started" -> started))
}

