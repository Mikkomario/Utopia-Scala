package utopia.metropolis.model.combined.organization

import utopia.metropolis.model.combined.description.{DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole

/**
  * Adds descriptive data to a user role
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param role Wrapped role with associated rights
  * @param descriptions Various descriptions for this role
  */
@deprecated("Replaced with DescribedUserRole", "v2.0")
case class DescribedRole(role: UserRoleWithRights, override val descriptions: Set[LinkedDescription])
	extends DescribedWrapper[UserRoleWithRights] with SimplyDescribed
{
	override def wrapped = role
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = role.toModel
}
