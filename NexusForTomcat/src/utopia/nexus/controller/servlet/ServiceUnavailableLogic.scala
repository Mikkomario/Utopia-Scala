package utopia.nexus.controller.servlet

import utopia.access.model.enumeration.ContentCategory.Text
import utopia.access.model.enumeration.Status.ServiceUnavailable
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.nexus.controller.write.WriteResponseBody
import utopia.nexus.controller.write.WriteResponseBody.NoBody
import utopia.nexus.model.request.Request.StreamedRequest
import utopia.nexus.model.response.Response
import utopia.nexus.model.servlet.ParameterEncoding

object ServiceUnavailableLogic
{
	/**
	 * @param message Message to include in the responses (as plaintext)
	 * @param logRequests Whether to record a log entry for each incoming request. Default = false.
	 * @param log Implicit logging implementation used
	 * @param jsonParser Implicit JSON parser used
	 * @return A new ServletLogic implementation that always yields 503 and the specified message.
	 */
	def withMessage(message: String, logRequests: Boolean = false)
	               (implicit log: Logger, jsonParser: JsonParser): ServiceUnavailableLogic =
		apply(WriteResponseBody.string(message, Text.plain), logRequests)
	
	/**
	 * @param body Implementation for writing the response body
	 * @param logRequests Whether to record a log entry for each incoming request. Default = false.
	 * @param log Implicit logging implementation used
	 * @param jsonParser Implicit JSON parser used
	 * @return A new ServletLogic implementation that always yields 503 and the specified response body.
	 */
	def apply(body: WriteResponseBody = NoBody, logRequests: Boolean = false)
	         (implicit log: Logger, jsonParser: JsonParser) =
		new ServiceUnavailableLogic(body, logRequests)
}

/**
 * Servlet logic that responds with 503 to every request.
 * @author Mikko Hilpinen
 * @since 28.09.2026, v1.3.5
 */
class ServiceUnavailableLogic(body: WriteResponseBody = NoBody, logRequests: Boolean = false)
                             (implicit override val logger: Logger, override val jsonParser: JsonParser)
	extends ServletLogic
{
	// ATTRIBUTES   ----------------------
	
	override val expectedParameterEncoding: ParameterEncoding = ParameterEncoding.none
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(request: StreamedRequest): Response = {
		if (logRequests)
			logger("Received a request while unavailable",
				Model.from("method" -> request.method.identifier, "path" -> request.pathString))
		Response(ServiceUnavailable, body = body)
	}
}
