package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, InstantType, IntType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

object MembershipData extends FromModelFactoryWithSchema[MembershipData]
{
	override val schema = ModelDeclaration("organization_id" -> IntType, "user_id" -> IntType,
		"started" -> InstantType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = MembershipData(model("organization_id"),
		model("user_id"), model("inviter_id"), model("started"), model("ended"))
}

/**
  * Contains basic data about an organization membership
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  * @param organizationId Id of the organization the user belongs to
  * @param userId Id of the user who belongs to the organization
  * @param creatorId Id of the user who created this membership
  * @param started Timestamp when the membership started (default = current time)
  * @param ended Timestamp when the membership ended. None if not ended (default).
  */
case class MembershipData(organizationId: Int, userId: Int, creatorId: Option[Int] = None,
						  started: Instant = Now, ended: Option[Instant] = None)
	extends StyledModelConvertible
{
	override def toModel = Model(Vector("organization_id" -> organizationId, "user_id" -> userId,
		"inviter_id" -> creatorId, "started" -> started, "ended" -> ended))
	
	// The simple model version expects organization and user ids to be available elsewhere and the membership
	// status to be always known also
	override def toSimpleModel = Model(Vector("inviter_id" -> creatorId, "started" -> started))
}
