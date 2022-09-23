package utopia.exodus.model.partial.auth

import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Used for authenticating requests before session-based authentication is available
  * @param token The textual representation of this api key
  * @param name Name given to identify this api key
  * @param created Time when this ApiKey was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class ApiKeyData(token: String, name: String, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("token" -> token, "name" -> name, "created" -> created))
}

