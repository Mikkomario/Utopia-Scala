package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership
import utopia.metropolis.model.stored.user.UserSettings

object DetailedMembership extends FromModelFactory[DetailedMembership]
{
	private val schema = ModelDeclaration(PropertyDeclaration("user_data", ModelType))
	
	// Validates model, then parses membership and settings (if possible)
	override def apply(model: template.Model[Property]) =
		schema.validate(model).toTry.flatMap { valid =>
			Membership(valid).flatMap { membership =>
				UserSettings(valid("user_data").getModel).map { settings =>
					// Finally parses user roles data
					val roles = valid("roles").getVector.flatMap { _.model }
						.flatMap { UserRoleWithRights(_).toOption }.toSet
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
case class DetailedMembership(membership: Membership, roles: Set[UserRoleWithRights], userData: UserSettings)
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
		Vector(Constant("roles", roles.toVector.sortBy { _.roleId }.map { _.toModel }),
			Constant("user_data", userData.toModel))
	
	override def toSimpleModel = userData.toSimpleModel ++ membership.toSimpleModel ++
		Model(Vector("role_ids" -> roles.map { _.roleId }.toVector.sorted,
			"task_ids" -> roles.flatMap { _.taskIds }.toVector.sorted))
}
