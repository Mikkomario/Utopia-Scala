package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Used for listing / linking, which tasks different organization membership roles allow
  * @param roleId Id of the organization member role that has authorization to perform the referenced task
  * @param taskId Id of the task the user's with referenced membership role are allowed to perform
  * @param created Time when this UserRoleRight was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserRoleRightData(roleId: Int, taskId: Int, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("role_id" -> roleId, "task_id" -> taskId, "created" -> created))
}

