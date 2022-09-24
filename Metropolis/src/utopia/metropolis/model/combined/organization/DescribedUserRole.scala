package utopia.metropolis.model.combined.organization

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.combined.description.{DescribedFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.organization.UserRole

object DescribedUserRole extends DescribedFactory[UserRole, DescribedUserRole]

/**
  * Combines UserRole with the linked descriptions
  * @param userRole UserRole to wrap
  * @param descriptions Descriptions concerning the wrapped UserRole
  * @since 2021-10-23
  */
case class DescribedUserRole(userRole: UserRole, descriptions: Set[LinkedDescription])
	extends DescribedWrapper[UserRole] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = userRole
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) =
		Model(Vector("id" -> userRole.id))
	
	
	// OTHER    -------------------------
	
	/**
	  * @param taskIds Allowed task ids
	  * @return A copy of this role with allowed task ids included
	  */
	def withAllowedTaskIds(taskIds: Set[Int]) = DetailedUserRole(this, taskIds)
}

