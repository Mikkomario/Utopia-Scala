package utopia.metropolis.model.combined.organization

import utopia.metropolis.model.combined.description.{DescribedWrapper, SimplyDescribed}
import utopia.metropolis.model.stored.description.{DescriptionLink, DescriptionRole}

/**
  * Adds descriptive data to a user role
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  * @param role Wrapped role with associated rights
  * @param descriptions Various descriptions for this role
  */
case class DescribedRole(role: RoleWithRights, override val descriptions: Set[DescriptionLink])
	extends DescribedWrapper[RoleWithRights] with SimplyDescribed
{
	override def wrapped = role
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = role.toModel
}
