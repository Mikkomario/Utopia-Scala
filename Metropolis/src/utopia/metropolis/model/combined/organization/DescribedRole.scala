package utopia.metropolis.model.combined.organization

import utopia.metropolis.model.combined.description.Described
import utopia.metropolis.model.stored.description.DescriptionLink

/**
  * Adds descriptive data to a user role
  * @author Mikko Hilpinen
  * @since 6.5.2020, v2
  * @param role Wrapped role with associated rights
  * @param descriptions Various descriptions for this role
  */
case class DescribedRole(role: RoleWithRights, descriptions: Set[DescriptionLink]) extends Described[RoleWithRights]
{
	override def wrapped = role
}
