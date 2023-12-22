package utopia.annex.controller

import utopia.annex.model.request.ApiRequest
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
	  */
	def factory: FromModelFactory[ApiRequest]
	
	/**
	  * @param requestModel A model
	  * @return Whether this handler should attempt to parse the specified model
	  */
	def shouldHandle(requestModel: Model): Boolean
	/**
	  * Handles a response received for a persisted request
	  * @param requestModel The persisted model from which the request was parsed
	  * @param request The request that was parsed and (possibly) sent to the server
	  * @param result Result received for the persisted request
	  */
	def handle(requestModel: Model, request: ApiRequest, result: RequestResult): Unit
}
