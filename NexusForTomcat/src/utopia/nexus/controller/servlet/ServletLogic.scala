package utopia.nexus.controller.servlet

import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.nexus.model.request.Request.StreamedRequest
import utopia.nexus.model.response.Response
import utopia.nexus.model.servlet.ParameterEncoding

import scala.language.implicitConversions

object ServletLogic
{
	// IMPLICIT    ----------------------
	
	/**
	 * @param f A function that handles incoming requests
	 * @param log Implicit logging implementation used
	 * @param jsonParser Implicit JSON parser used
	 * @param expectedParameterEncoding Implicit expected parameter encoding
	 * @return A new ServletLogic implementation that wraps the specified function
	 */
	implicit def apply(f: StreamedRequest => Response)
	                  (implicit log: Logger, jsonParser: JsonParser, expectedParameterEncoding: ParameterEncoding): ServletLogic =
		new _ServletLogic(f)
	
	
	// NESTED   -------------------------
	
	private class _ServletLogic(f: StreamedRequest => Response)
	                           (implicit override val logger: Logger, override val jsonParser: JsonParser,
	                            override val expectedParameterEncoding: ParameterEncoding)
		extends ServletLogic
	{
		override def apply(request: StreamedRequest): Response = f(request)
	}
}

/**
  * Common trait for logical servlet implementations
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.2.4
  */
trait ServletLogic
{
	// ABSTRACT ------------------------
	
	/**
	 * @return The logging implementation used
	 */
	def logger: Logger
	/**
	  * @return The JSON parser used
	  */
	def jsonParser: JsonParser
	/**
	 * @return The parameter encoding expected in the received request
	 */
	def expectedParameterEncoding: ParameterEncoding
	
	/**
	  * Receives a request and produces a response
	  * @param request Request to receive
	  * @return A response to return to the client
	  */
	def apply(request: StreamedRequest): Response
}
