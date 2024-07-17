package utopia.annex.model.request

import utopia.access.http.Method.Get
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.model.immutable.Value

import scala.concurrent.Future

object GetRequest
{
	// OTHER    -----------------------------
	
	/**
	  * @param path Targeted resource path (after root path)
	  * @param deprecationCondition A condition that must be true in order for this request to deprecate (optional).
	  *                             Requests don't deprecate by default.
	  * @param send A function which accepts a prepared request and finalizes the sending process,
	  *             determining how the responses are parsed, etc.
	  * @return A new request targeting specified path
	  */
	def apply[A](path: String, deprecationCondition: => Boolean = false)
	            (send: PreparedRequest => Future[RequestResult[A]]): GetRequest[A] =
		new SimpleGetRequest(path, deprecationCondition)(send)
	
	/**
	  * @param path Targeted resource path (after root path)
	  * @param deprecationCondition A condition that must be true in order for this request to deprecate (optional).
	  *                             Requests don't deprecate by default.
	  * @return A new request targeting specified path.
	  *         Will not post-process responses past the [[Value]] data type.
	  */
	def value(path: String, deprecationCondition: => Boolean = false): GetRequest[Value] =
		new GetValueRequest(path, deprecationCondition)
	
	
	// NESTED   -----------------------------
	
	private class GetValueRequest(override val path: String, deprecation: => Boolean) extends GetRequest[Value]
	{
		override def deprecated: Boolean = deprecation
		
		override def send(prepared: PreparedRequest): Future[RequestResult[Value]] = prepared.getValue
	}
	
	private class SimpleGetRequest[+A](override val path: String, deprecationCondition: => Boolean)
	                                 (f: PreparedRequest => Future[RequestResult[A]])
		extends GetRequest[A]
	{
		override def deprecated = deprecationCondition
		
		override def send(prepared: PreparedRequest): Future[RequestResult[A]] = f(prepared)
	}
}

/**
  * Common trait for simple api requests that use the GET method
  * @author Mikko Hilpinen
  * @since 16.6.2020, v1
  */
trait GetRequest[+A] extends ApiRequest[A]
{
	// IMPLEMENTED  -----------------------
	
	override def method = Get
	override def body = Value.empty
}
