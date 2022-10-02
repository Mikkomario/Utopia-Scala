package utopia.ambassador.model.partial.service

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now

/**
  * Represents a service that provides an OAuth interface (e.g. Google)
  * @param name Name of this service (from the customer's perspective)
  * @param created Time when this AuthService was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthServiceData(name: String, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("name" -> name, "created" -> created))
}

