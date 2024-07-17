package utopia.annex.controller

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
	  * @param requestModel A request model
	  * @return Whether this handler should process the specified request / model
	  */
	def shouldHandle(requestModel: Model): Boolean
	
	/**
	  * Processes a previously persisted request
	  * @param requestModel A model which represents a request.
	  *                     Accepted by 'shouldHandle(Model)'
	  * @param queue A request queue which may be used to send the request
	  */
	def handle(requestModel: Model, queue: PersistingRequestQueue): Unit
}
