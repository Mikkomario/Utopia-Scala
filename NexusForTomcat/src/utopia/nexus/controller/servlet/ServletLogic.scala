package utopia.nexus.controller.servlet

import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.nexus.model.request.Request.StreamedRequest
import utopia.nexus.model.response.Response
import utopia.nexus.model.servlet.ParameterEncoding

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
	  * @return A response to return back to the client
	  */
	def apply(request: StreamedRequest): Response
}
