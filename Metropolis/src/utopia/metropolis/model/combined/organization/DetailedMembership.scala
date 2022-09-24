package utopia.metropolis.model.combined.organization

import utopia.flow.collection.template.typeless
import utopia.flow.collection.value
import utopia.flow.collection.value.typeless.PropertyDeclaration
import utopia.flow.datastructure.template
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.generic.model.mutable.ModelType
import utopia.flow.generic.model.template.{Model, Property}
import utopia.flow.view.template.Extender
import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.metropolis.model.stored.user.UserSettings

object DetailedMembership extends FromModelFactory[DetailedMembership]
{
	private val schema = ModelDeclaration(PropertyDeclaration("user_data", ModelType))
	
	// Validates model, then parses membership and settings (if possible)
	override def apply(model: Model[Property]) =
		schema.validate(model).toTry.flatMap { valid =>
			Membership(valid).flatMap { membership =>
				UserSettings(valid("user_data").getModel).map { settings =>
					// Finally parses user roles data
					val roles = valid("role_links").getVector.flatMap { _.model }
						.flatMap { MemberRoleWithRights(_).toOption }.toSet
					// Combines parsed data
					DetailedMembership(membership, roles, settings)
				}
			}
		}
}

/**
  * Adds role, allowed task id and user settings information to a membership
  * @author Mikko Hilpinen
  * @since 1.12.2020, v1
  */
case class DetailedMembership(membership: Membership, roleLinks: Set[MemberRoleWithRights], userData: UserSettings)
	extends Extender[MembershipData] with StyledModelConvertible
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Id of this membership
	  */
	def id = membership.id
	
	
	// IMPLEMENTED  ----------------------------
	
	override def wrapped = membership
	
	override def toModel = membership.toModel ++
		Vector(Constant("roles_links", roleLinks.toVector.sortBy { _.roleId }.map { _.toModel }),
			Constant("user_data", userData.toModel))
	
	override def toSimpleModel = userData.toSimpleModel ++ membership.toSimpleModel ++
		Model(Vector("role_ids" -> roleLinks.map { _.roleId }.toVector.sorted,
			"task_ids" -> roleLinks.flatMap { _.taskIds }.toVector.sorted))
}
