package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

/**
  * Represents an access right requirement and/or category.
  * @param name Technical name or identifier of this scope
  * @param created Time when this scope was first created
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class ScopeData(name: String, created: Instant = Now) extends StyledModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("name" -> name, "created" -> created))
	
	override def toSimpleModel = Model(Vector("name" -> name))
}

