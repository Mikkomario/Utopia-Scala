package utopia.ambassador.model.partial.scope

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

/**
  * Scopes are like access rights which can be requested from 3rd party services. They determine what the application is allowed to do in behalf of the user.
  * @param serviceId Id of the service this scope is part of / which uses this scope
  * @param name Name of this scope in the 3rd party service
  * @param priority Priority assigned for this scope where higher values mean higher priority. Used when multiple scopes can be chosen from.
  * @param created Time when this Scope was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class ScopeData(serviceId: Int, name: String, priority: Option[Int] = None, created: Instant = Now) 
	extends StyledModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toSimpleModel = Model(Vector("name" -> name))
	
	override def toModel =
		Model(Vector("service_id" -> serviceId, "name" -> name, "priority" -> priority, "created" -> created))
}

