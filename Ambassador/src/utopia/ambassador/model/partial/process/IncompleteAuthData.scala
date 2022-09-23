package utopia.ambassador.model.partial.process

import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

/**
  * Represents a case where a user arrives from a 3rd party service without first preparing an authentication on this side
  * @param serviceId Id of the service from which the user arrived
  * @param code Authentication code provided by the 3rd party service
  * @param token Token used for authentication the completion of this authentication
  * @param expires Time after which the generated authentication token is no longer valid
  * @param created Time when this IncompleteAuth was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class IncompleteAuthData(serviceId: Int, code: String, token: String, expires: Instant, 
	created: Instant = Now) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this IncompleteAuth is no longer valid because it has expired
	  */
	def hasExpired = expires <= Now
	
	/**
	  * Whether this IncompleteAuth is still valid (hasn't expired yet)
	  */
	def isValid = !hasExpired
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("service_id" -> serviceId, "code" -> code, "token" -> token, "expires" -> expires, 
			"created" -> created))
}

