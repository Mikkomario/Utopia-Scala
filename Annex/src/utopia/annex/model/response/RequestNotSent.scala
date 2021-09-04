package utopia.annex.model.response

import utopia.disciple.model.error.RequestFailedException

import scala.util.Failure

/**
  * A status used when a request is not sent for some reason
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait RequestNotSent
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return A throwable error based on this state
	  */
	def toException: Throwable
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @tparam A Type of failure
	  * @return A failure based on this state
	  */
	def toFailure[A] = Failure[A](toException)
}

object RequestNotSent
{
	/**
	  * Status generated when request gets deprecated before it is successfully sent
	  */
	case object RequestWasDeprecated extends RequestNotSent
	{
		override def toException = new RequestFailedException("Request was deprecated")
	}
	
	/**
	  * Status used when request can't be sent due to some error in the request or the request system
	  * @param error Associated error
	  */
	case class RequestFailed(error: Throwable) extends RequestNotSent
	{
		override def toException = error
	}
}
