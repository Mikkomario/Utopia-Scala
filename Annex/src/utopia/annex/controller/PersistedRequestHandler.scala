package utopia.annex.controller

import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.{RequestNotSent, Response}
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.FromModelFactory

/**
  * Used for handling responses for requests that were created and persisted during the previous use session(s)
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait PersistedRequestHandler
{
	// ABSTRACT ----------------------------
	
	/**
	  * @param requestModel A model
	  * @return Whether this handler should attempt to parse the specified model
	  */
	def shouldHandle(requestModel: Model[Constant]): Boolean
	
	/**
	  * @return Factory used for parsing requests
	  */
	def factory: FromModelFactory[ApiRequest]
	
	/**
	  * Handles a response received for a persisted request
	  * @param result Result received for the persisted request
	  */
	def handle(result: Either[RequestNotSent, Response]): Unit
}
