package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.combined.description.DescribedSimpleModelConvertible
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.user.UserSettings

object DetailedInvitation extends FromModelFactory[DetailedInvitation]
{
	override def apply(model: template.Model[Property]) =
		InvitationWithResponse(model).map { base =>
			val organization = model("organization").model.flatMap { DescribedOrganization(_).toOption }
				.getOrElse { DescribedOrganization(base.organizationId, Set()) }
			DetailedInvitation(base, organization, model("sender").model.flatMap { UserSettings(_).toOption })
		}
}

/**
  * Attaches contextual information to an invitation, suitable for describing the invitation more specifically
  * to the recipient.
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  */
case class DetailedInvitation(invitation: InvitationWithResponse, organization: DescribedOrganization,
                              senderData: Option[UserSettings])
	extends Extender[InvitationData] with ModelConvertible with DescribedSimpleModelConvertible
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Id of this invitation
	  */
	def id = invitation.id
	
	/**
	  * @return Response attached to this invitation (None if contains no response)
	  */
	def response = invitation.response
	
	
	// IMPLEMENTED  --------------------------
	
	override def wrapped = invitation.invitation.data
	
	override def toModel = invitation.toModel ++ Model(Vector(
		"organization" -> organization.toModel,
		"sender" -> senderData.map { _.toModel }))
	
	
	// OTHER    ------------------------------
	
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		invitation.toSimpleModel.without("organization_id", "sender_id") ++
			Vector(Constant("organization", organization.toSimpleModelUsing(descriptionRoles)),
				Constant("sender", senderData.map { _.toModel }))
}
