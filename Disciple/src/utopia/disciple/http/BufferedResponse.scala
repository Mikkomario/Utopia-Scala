package utopia.disciple.http

import utopia.access.http.Status
import utopia.access.http.Headers

import scala.util.{Failure, Success}

/**
* A buffered response is a response that has its body parsed and buffered into program memory
* @author Mikko Hilpinen
* @since 19.2.2018
  * @param body Parsed response body
  * @param status Response status
  * @param headers Response headers
**/
class BufferedResponse[+A](val body: A, override val status: Status, override val headers: Headers,
        /*override val cookies: Set[Cookie]*/) extends Response
{
	// IMPLEMENTED	-------------------
	
	override def toString =
	{
		val bodyString = body match
		{
			case Success(content) => s"Success($content)"
			case Failure(error) => s"Failure(${error.getMessage})"
		}
		s"$status: $bodyString. Headers: $headers"
	}
	
	
	// OTHER	-----------------------
	
	/**
	 * Maps the contents of this response body
	 * @param f A mapping function
	 * @tparam B Resulting body type
	 * @return A response with mapped body
	 */
	def map[B](f: A => B) = new BufferedResponse(f(body), status, headers)
}