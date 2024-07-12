package utopia.annex.model.request

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.flow.generic.model.immutable.Value

object ApiRequest
{
	// OTHER    -------------------------
	
	/**
	  * Creates a GET request
	  * @param path Targeted path on the server (after the server root path)
	  * @param deprecationCondition Condition which yields true if this request gets deprecated
	  *                             and should be retracted (if not sent) (call-by-name).
	  *                             Default = always false.
	  * @return A new GET API request
	  */
	def get(path: String, deprecationCondition: => Boolean = false) = GetRequest(path, deprecationCondition)
	
	/**
	  * Creates a new POST, PUT or PATCH request (i.e. a request with a body)
	  * @param path Path to the targeted resource on the server (after the server root path)
	  * @param body Posted request body
	  * @param method Method used (default = POST)
	  * @param deprecationCondition Condition which yields true if this request gets deprecated
	  *                             and should be retracted (if not sent) (call-by-name).
	  *                             Default = always false.
	  * @return A new request
	  */
	def post(path: String, body: Value, method: Method = Post, deprecationCondition: => Boolean = false): ApiRequest =
		new SimplePostRequest(path, body, method, deprecationCondition)
	
	
	// NESTED   -------------------------
	
	private class SimplePostRequest(override val path: String, override val body: Value, override val method: Method,
	                                deprecationCondition: => Boolean)
		extends ApiRequest
	{
		override def deprecated: Boolean = deprecationCondition
	}
}

/**
  * Represents a relatively simple request that may be sent multiple times if need be
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait ApiRequest extends Retractable
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
	
	
	// COMPUTED ---------------------------
	
	@deprecated("Deprecated for removal. Renamed to .deprecated.", "v1.7")
	def isDeprecated = deprecated
	
	
	// IMPLEMENTED  -----------------------
	
	override def toString = s"$method $path"
}
