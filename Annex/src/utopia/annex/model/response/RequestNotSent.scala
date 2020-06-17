package utopia.annex.model.response

/**
  * A status used when a request is not sent for some reason
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait RequestNotSent

object RequestNotSent
{
	/**
	  * Status generated when request gets deprecated before it is successfully sent
	  */
	case object RequestWasDeprecated extends RequestNotSent
	
	/**
	  * Status used when request can't be sent due to some error in the request or the request system
	  * @param error Associated error
	  */
	case class RequestFailed(error: Throwable) extends RequestNotSent
}
