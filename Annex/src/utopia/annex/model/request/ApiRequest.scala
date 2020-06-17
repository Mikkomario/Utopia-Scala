package utopia.annex.model.request

import utopia.access.http.Method
import utopia.flow.datastructure.immutable.{Constant, Model}

/**
  * Represents a relatively simple request that may be sent multiple times if need be
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait ApiRequest
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return Request method
	  */
	def method: Method
	
	/**
	  * @return Request path (root path not included)
	  */
	def path: String
	
	/**
	  * @return Request json object body. Empty model if no body should be sent
	  */
	def body: Model[Constant]
	
	/**
	  * @return Whether this request has been deprecated and shouldn't be sent (anymore)
	  */
	def isDeprecated: Boolean
	
	/**
	  * @return A model that can be stored locally to replicate this request in another session. None if this
	  *         request needn't be persisted and replicated
	  */
	def persistingModel: Option[Model[Constant]]
	
	
	// IMPLEMENTED  -----------------------
	
	override def toString = s"$method $path"
}
