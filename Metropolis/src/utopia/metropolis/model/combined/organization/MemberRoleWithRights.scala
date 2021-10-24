package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.partial.organization.MemberRoleData
import utopia.metropolis.model.stored.organization.MemberRole

object MemberRoleWithRights extends FromModelFactory[MemberRoleWithRights]
{
	override def apply(model: Model[Property]) = MemberRole(model).map { role =>
		MemberRoleWithRights(role, model("task_ids").getVector.flatMap { _.int }.toSet)
	}
}

/**
  * Represents a single membership role link and includes allowed task ids
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
case class MemberRoleWithRights(roleLink: MemberRole, taskIds: Set[Int])
	extends Extender[MemberRoleData] with ModelConvertible
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
