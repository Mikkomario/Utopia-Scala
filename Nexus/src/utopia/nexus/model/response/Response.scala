package utopia.nexus.model.response

import utopia.access.model.{Cookie, Headers}
import utopia.access.model.enumeration.Status
import utopia.access.model.enumeration.Status.OK
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.util.Mutate
import utopia.nexus.controller.write.WriteResponseBody
import utopia.nexus.controller.write.WriteResponseBody.NoBody

/**
 * Represents a response to send out to the client
 * @param status Status to send out
 * @param headers Headers to send out
 * @param newCookies New cookies to set
 * @param body Logic for writing the response body
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
case class Response(status: Status = OK, headers: Headers = Headers.empty, newCookies: Seq[Cookie] = Empty,
                    body: WriteResponseBody = NoBody)
	extends MaybeEmpty[Response]
{
	// COMPUTED -----------------------
	
	/**
	 * @return Whether this represents a successful response
	 */
	def isSuccess = status.isSuccess
	/**
	 * @return Whether this represents a failure response
	 */
	def isFailure = !isSuccess
	
	
	// IMPLEMENTED  -------------------
	
	override def self: Response = this
	override def isEmpty: Boolean = body.isEmpty
	
	
	// OTHER    -----------------------
	
	/**
	 * @param status New status to assign to this response
	 * @return A copy of this response with the specified status
	 */
	def withStatus(status: Status) = copy(status = status)
	def mapStatus(f: Mutate[Status]) = withStatus(f(status))
	
	/**
	 * @param headers New headers to assign
	 * @param overwrite Whether to overwrite all current headers (default = false)
	 * @return Copy of this response with the specified headers
	 */
	def withHeaders(headers: Headers, overwrite: Boolean = false) =
		copy(headers = if (overwrite) headers else this.headers ++ headers)
	def mapHeaders(f: Mutate[Headers]) = withHeaders(f(headers), overwrite = true)
	
	/**
	 * @param cookies New cookies to set in the browser
	 * @param overwrite Whether to overwrite the current [[newCookies]]. Default = false.
	 * @return A copy of this response with the specified cookies.
	 */
	def withCookies(cookies: IterableOnce[Cookie], overwrite: Boolean = false) =
		copy(newCookies = if (overwrite) OptimizedIndexedSeq.from(cookies) else newCookies ++ cookies)
	def mapCookies(f: Seq[Cookie] => IterableOnce[Cookie]) = withCookies(f(newCookies), overwrite = true)
}