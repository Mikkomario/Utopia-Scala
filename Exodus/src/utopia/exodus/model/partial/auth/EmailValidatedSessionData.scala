package utopia.exodus.model.partial.auth

import utopia.exodus.util.ExodusContext

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

/**
  * Used for creating a temporary and limited session based on an authenticated email validation attempt
  * @param validationId Reference to the email validation used as the basis for this session
  * @param token Token used to authenticate against this session (default = newly generated)
  * @param expires Time when this EmailValidatedSession expires / becomes invalid (default after 1 hour)
  * @param created Time when this EmailValidatedSession was first created
  * @param closedAfter Time after which this session was manually closed
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
case class EmailValidatedSessionData(validationId: Int, token: String = ExodusContext.uuidGenerator.next(),
                                     expires: Instant = Now + 1.hours,
                                     created: Instant = Now, closedAfter: Option[Instant] = None)
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this EmailValidatedSession has already been deprecated
	  */
	def isDeprecated = closedAfter.isDefined
	
	/**
	  * Whether this EmailValidatedSession is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("validation_id" -> validationId, "token" -> token, "expires" -> expires, 
			"created" -> created, "closed_after" -> closedAfter))
}

