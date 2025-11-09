package utopia.nexus.test

import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.model.api.ApiVersion
import utopia.nexus.model.request.Request.StreamedRequest

/**
 * The request context used in the Nexus test server
 * @author Mikko Hilpinen
 * @since 09.11.2025, v2.0
 */
class NexusTestContext(request: StreamedRequest, val apiVersion: ApiVersion)
                      (implicit log: Logger, jsonParser: JsonParser)
	extends PostContext(request)
