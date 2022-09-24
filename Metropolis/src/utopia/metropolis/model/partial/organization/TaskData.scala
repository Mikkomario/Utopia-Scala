package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Represents a type of task a user can perform (within an organization)
  * @param created Time when this Task was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class TaskData(created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("created" -> created))
}

