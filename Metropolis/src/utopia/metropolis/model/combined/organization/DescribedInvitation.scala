package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.combined.description.{DescribedSimpleModelConvertible, SimplyDescribed}
import utopia.metropolis.model.stored.description.{DescriptionLink, DescriptionRole}
import utopia.metropolis.model.stored.organization.Invitation
import utopia.metropolis.model.stored.user.UserSettings

object DescribedInvitation extends FromModelFactory[DescribedInvitation]
{
	override def apply(model: template.Model[Property]) = Invitation(model).map { base =>
		DescribedInvitation(base,
			model("organization_descriptions").getVector.flatMap { _.model }.flatMap { DescriptionLink(_).toOption }.toSet,
			model("sender").model.flatMap { UserSettings(_).toOption })
	}
}

/**
  * Attaches contextual information to an invitation, suitable for describing the invitation more specifically
  * to the recipient.
  * @author Mikko Hilpinen
  * @since 11.7.2020, v1
  */
case class DescribedInvitation(wrapped: Invitation, organizationDescriptions: Set[DescriptionLink],
							   senderData: Option[UserSettings])
	extends Extender[Invitation] with ModelConvertible with DescribedSimpleModelConvertible
{
	// IMPLEMENTED  --------------------------
	
	override def toModel = wrapped.toModel ++ Model(Vector(
		"organization_descriptions" -> organizationDescriptions.toVector.map { _.toModel },
		"sender" -> senderData.map { _.toModel }))
	
	
	// OTHER    ------------------------------
	
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
	{
		val organization = Model.withConstants(Constant("id", wrapped.organizationId) +:
			SimplyDescribed.descriptionPropertiesFrom(
				organizationDescriptions.map { _.description }, descriptionRoles).toVector)
		
		Model(Vector("id" -> wrapped.id, "sender" -> senderData.map { _.toSimpleModel },
			"organization" -> organization, "role_id" -> wrapped.startingRoleId,
			"expires" -> wrapped.expireTime))
	}
}
