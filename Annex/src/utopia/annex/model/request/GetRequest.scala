package utopia.annex.model.request

import utopia.access.model.enumeration.Method.Get
import utopia.annex.controller.ApiClient.PreparedRequest
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysFalse

import scala.concurrent.Future

object GetRequest
{
	// OTHER    -----------------------------
	
	/**
	  * @param path Targeted resource path (after root path)
	  * @param params Parameters to include on the request path (default = empty)
	 * @param deprecationView A view that contains true if this request should be retracted (unless already sent).
	 *                        Default = always false = this request won't deprecate.
	  * @param send A function which accepts a prepared request and finalizes the sending process,
	  *             determining how the responses are parsed, etc.
	  * @return A new request targeting specified path
	  */
	def apply[A](path: String, params: Model = Model.empty, deprecationView: View[Boolean] = AlwaysFalse)
	            (send: PreparedRequest => Future[RequestResult[A]]): GetRequest[A] =
		new SimpleGetRequest(path, params, deprecationView)(send)
	/**
	  * @param path Targeted resource path (after root path)
	  * @param params Parameters to include on the request path (default = empty)
	 * @param deprecationView A view that contains true if this request should be retracted (unless already sent).
	 *                        Default = always false = this request won't deprecate.
	  * @return A new request targeting specified path.
	  *         Will not post-process responses past the [[Value]] data type.
	  */
	def value(path: String, params: Model = Model.empty, deprecationView: View[Boolean] = AlwaysFalse): GetRequest[Value] =
		new GetValueRequest(path, params, deprecationView)
	
	
	// NESTED   -----------------------------
	
	private class GetValueRequest(override val path: String, override val pathParams: Model,
	                              deprecationView: View[Boolean])
		extends GetRequest[Value]
	{
		override def deprecated: Boolean = deprecationView.value
		
		override def send(prepared: PreparedRequest): Future[RequestResult[Value]] = prepared.getValue
	}
	
	private class SimpleGetRequest[+A](override val path: String, override val pathParams: Model,
	                                   deprecationView: View[Boolean])
	                                  (f: PreparedRequest => Future[RequestResult[A]])
		extends GetRequest[A]
	{
		override def deprecated = deprecationView.value
		
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
	override def body = Left(Value.empty)
}
