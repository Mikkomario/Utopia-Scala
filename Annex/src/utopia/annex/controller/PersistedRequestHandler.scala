package utopia.annex.controller

import utopia.annex.model.request.{ApiRequest, ApiRequestSeed, RequestQueueable}
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model

/**
  * Used for handling responses for requests that were created and persisted during the previous use session(s)
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait PersistedRequestHandler
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Factory used for parsing requests.
	  *         Only needs to process models accepted by 'shouldHandle'.
	  *         Returns the requests in either prepared form or in "seed" form
	  */
	def factory: FromModelFactory[Either[ApiRequestSeed, ApiRequest]]
	
	/**
	  * @param requestModel A model
	  * @return Whether this handler should attempt to parse the specified model
	  */
	def shouldHandle(requestModel: Model): Boolean
	/**
	  * Handles a response received for a persisted request
	  * @param requestModel The persisted model from which the request was parsed
	  * @param request The request that was parsed and (possibly) sent to the server.
	  *                Either in prepared form (Right) or in "seed" form (Left).
	  * @param result Result received for the persisted request
	  */
	def handle(requestModel: Model, request: RequestQueueable, result: RequestResult): Unit
}
