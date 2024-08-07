package utopia.metropolis.model.combined.user

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{IntType, ModelType}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.metropolis.model.DeepExtender
import utopia.metropolis.model.combined.description.DescribedSimpleModelConvertible
import utopia.metropolis.model.combined.organization.{DescribedOrganization, UserRoleWithRights}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.organization.Organization

object MyOrganization extends FromModelFactory[MyOrganization]
{
	// ATTRIBUTES	-------------------------------
	
	private val schema = ModelDeclaration("organization" -> ModelType, "user" -> ModelType)
	private val idSchema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	
	// IMPLEMENTED	-------------------------------
	
	override def apply(model: template.ModelLike[Property]) =
		// Validates the model
		schema.validate(model).flatMap { valid =>
			// Parses organization data
			DescribedOrganization(valid("organization").getModel).flatMap { organization =>
				// Parses user-related data
				idSchema.validate(valid("user").getModel).map { userModel =>
					val roles = userModel("roles").getVector.flatMap { _.model }
						.flatMap { UserRoleWithRights(_).toOption }.toSet
					MyOrganization(userModel("id"), organization, roles)
				}
			}
		}
}

/**
  * Contains more information about an organization from a single member's perspective
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param userId Id of the described user
  * @param wrapped The wrapped organization instance
  * @param myRoles Described user's roles in this organization
  */
case class MyOrganization(userId: Int, wrapped: DescribedOrganization, myRoles: Set[UserRoleWithRights])
	extends DeepExtender[DescribedOrganization, Organization] with ModelConvertible with DescribedSimpleModelConvertible
{
	// IMPLEMENTED  -----------------------------
	
	override def toModel =
	{
		val organizationModel = wrapped.toModel
		val userModel = Model(Vector("id" -> userId, "roles" -> myRoles.map { _.toModel }.toVector))
		Model(Vector("organization" -> organizationModel, "user" -> userModel))
	}
	
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		wrapped.toSimpleModelUsing(descriptionRoles) ++
			Model(Vector("my_role_ids" -> myRoles.map { _.roleId }.toVector.sorted,
				"my_task_ids" -> myRoles.flatMap { _.taskIds }.toVector.sorted))
}
