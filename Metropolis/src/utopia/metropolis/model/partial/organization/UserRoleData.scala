package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * An enumeration for different roles a user may have within an organization
  * @param created Time when this UserRole was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserRoleData(created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("created" -> created))
}

