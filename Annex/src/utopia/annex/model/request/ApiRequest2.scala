package utopia.annex.model.request

import utopia.access.http.Method
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.model.response.RequestResult2
import utopia.flow.generic.model.immutable.Value

import scala.concurrent.Future

// TODO: Add the utility functions from the older version

/**
  * Represents a request that may be sent out using an [[utopia.annex.controller.ApiClient]]
  * @tparam A type of the parsed request response body
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait ApiRequest2[+A] extends Retractable
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
	  * @return Request body value. Empty value if no body should be sent
	  */
	def body: Value
	
	/**
	  * Finalizes a sending process (for this request), determining how the response body is handled
	  * @param prepared A prepared version of this request
	  * @return Future which resolves into a request result of teh correct type
	  */
	def send(prepared: PreparedRequest): Future[RequestResult2[A]]
	
	
	// COMPUTED ---------------------------
	
	@deprecated("Deprecated for removal. Renamed to .deprecated.", "v1.7")
	def isDeprecated = deprecated
	
	
	// IMPLEMENTED  -----------------------
	
	override def toString = s"$method $path"
}
