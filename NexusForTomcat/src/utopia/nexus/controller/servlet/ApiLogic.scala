package utopia.nexus.controller.servlet

import utopia.access.model.enumeration.Status.InternalServerError
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.nexus.controller.api.ApiRoot
import utopia.nexus.model.request.Request.StreamedRequest
import utopia.nexus.model.request.StreamOrReader
import utopia.nexus.model.response.Response
import utopia.nexus.model.servlet.ParameterEncoding

/**
  * A logic component for an API, based around ApiRoot
  * @author Mikko Hilpinen
  * @since 18.8.2022, v1.2.4
  * @param root A request handler that will receive and process the requests
  * @param jsonParser Implicit json parser implementation
  * @param logger Implicit logging implementation
 * @param expectedParameterEncoding Expected parameter encoding
  */
class ApiLogic(root: ApiRoot[_, StreamOrReader], logServerErrors: Boolean = false)
              (implicit override val jsonParser: JsonParser, override val logger: Logger,
               override val expectedParameterEncoding: ParameterEncoding)
	extends ServletLogic
{
	// IMPLEMENTED  ------------------------
	
	override def apply(request: StreamedRequest): Response = {
		val requestTime = Now.toInstant
		val response = root(request)
		
		// May log internal server error -responses
		if (logServerErrors && response.status == InternalServerError)
			logger(s"Internal server error while handling $request")
		
		// Adds the date header, if not present in the original response
		response.mapHeaders { headers =>
			if (headers.date.isEmpty)
				headers.withDate(requestTime)
			else
				headers
		}
	}
}
