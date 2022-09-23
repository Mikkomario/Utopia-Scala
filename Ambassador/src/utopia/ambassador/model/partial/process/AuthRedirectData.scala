package utopia.ambassador.model.partial.process

import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

/**
  * Records each event when a user is directed to the 3rd party OAuth service. These close the linked preparations.
  * @param preparationId Id of the preparation event for this redirection
  * @param expires Time when the linked redirect token expires
  * @param created Time when this AuthRedirect was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthRedirectData(preparationId: Int, token: String, expires: Instant, created: Instant = Now) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this AuthRedirect is no longer valid because it has expired
	  */
	def hasExpired = expires <= Now
	
	/**
	  * Whether this AuthRedirect is still valid (hasn't expired yet)
	  */
	def isValid = !hasExpired
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("preparation_id" -> preparationId, "token" -> token, "expires" -> expires, 
			"created" -> created))
}

