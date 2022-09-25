package utopia.metropolis.model.combined.organization

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.generic.model.template.{ModelLike, ModelConvertible, Property}
import utopia.flow.view.template.Extender
import utopia.metropolis.model.partial.organization.MemberRoleLinkData
import utopia.metropolis.model.stored.organization.MemberRoleLink

object MemberRoleWithRights extends FromModelFactory[MemberRoleWithRights]
{
	override def apply(model: ModelLike[Property]) = MemberRoleLink(model).map { role =>
		MemberRoleWithRights(role, model("task_ids").getVector.flatMap { _.int }.toSet)
	}
}

/**
  * Represents a single membership role link and includes allowed task ids
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
case class MemberRoleWithRights(roleLink: MemberRoleLink, taskIds: Set[Int])
	extends Extender[MemberRoleLinkData] with ModelConvertible
{
	// COMPUTED --------------------------
	
	/**
	  * @return Id of this member role link
	  */
	def id = roleLink.id
	
	
	// IMPLEMENTED  ----------------------
	
	override def wrapped = roleLink.data
	
	override def toModel = roleLink.toModel + Constant("task_ids", taskIds.toVector.sorted)
}
