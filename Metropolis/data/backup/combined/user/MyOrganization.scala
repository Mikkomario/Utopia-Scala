package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible, ModelType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.combined.description.SimplyDescribed
import utopia.metropolis.model.combined.organization.UserRoleWithRights
import utopia.metropolis.model.stored.description.{DescriptionLinkOld, DescriptionRole}

object MyOrganization extends FromModelFactory[MyOrganization]
{
	// ATTRIBUTES	-------------------------------
	
	private val schema = ModelDeclaration("organization" -> ModelType, "user" -> ModelType)
	private val idSchema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	
	// IMPLEMENTED	-------------------------------
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		idSchema.validate(valid("organization").getModel).toTry.flatMap { organizationModel =>
			idSchema.validate(valid("user").getModel).toTry.map { userModel =>
				val descriptions = organizationModel("descriptions").getVector.flatMap { _.model }
					.flatMap { DescriptionLinkOld(_).toOption }.toSet
				val roles = userModel("roles").getVector.flatMap { _.model }
					.flatMap { UserRoleWithRights(_).toOption }.toSet
				MyOrganization(organizationModel("id"), userModel("id"), descriptions, roles)
			}
		}
	}
}

/**
  * Contains more information about an organization from a single member's perspective
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param id Organization id
  * @param userId Id of the described user
  * @param descriptions Various descriptions of this organization
  * @param myRoles Described user's roles in this organization
  */
case class MyOrganization(id: Int, userId: Int, override val descriptions: Set[DescriptionLinkOld],
                          myRoles: Set[UserRoleWithRights])
	extends ModelConvertible with SimplyDescribed
{
	// IMPLEMENTED  -----------------------------
	
	override def toModel =
	{
		val organizationModel = Model(Vector("id" -> id, "descriptions" -> descriptions.map { _.toModel }.toVector))
		val userModel = Model(Vector("id" -> userId, "roles" -> myRoles.map { _.toModel }.toVector))
		Model(Vector("organization" -> organizationModel, "user" -> userModel))
	}
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) =
		Model(Vector("id" -> id, "my_role_ids" -> myRoles.map { _.roleId }.toVector.sorted,
			"my_task_ids" -> myRoles.flatMap { _.taskIds }.toVector.sorted))
}
