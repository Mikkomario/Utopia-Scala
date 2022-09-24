package utopia.ambassador.model.partial.process

import java.time.Instant
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.StyledModelConvertible

/**
  * Used for preparing and authenticating an OAuth process that follows
  * @param userId Id of the user who initiated this process
  * @param token Token used for authenticating the OAuth redirect
  * @param expires Time when this authentication (token) expires
  * @param clientState A custom state provided by the client and sent back upon user redirect
  * @param created Time when this AuthPreparation was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthPreparationData(userId: Int, token: String, expires: Instant, clientState: Option[String] = None,
                               created: Instant = Now)
	extends StyledModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this AuthPreparation is no longer valid because it has expired
	  */
	def hasExpired = expires <= Now
	/**
	  * Whether this AuthPreparation is still valid (hasn't expired yet)
	  */
	def isValid = !hasExpired
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("user_id" -> userId, "token" -> token, "expires" -> expires, "client_state" -> clientState,
			"created" -> created))
	
	override def toSimpleModel = Model(Vector("token" -> token, "expiration" -> expires))
}

