package utopia.nexus.test

import utopia.access.model.enumeration.Method
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.nexus.controller.api.interceptor.{RequestInterceptor, RequestInterceptorFactory}
import utopia.nexus.model.api.ApiVersion
import utopia.nexus.model.request.{Request, RequestContext}
import utopia.nexus.model.response.RequestResult

import scala.annotation.unused
import scala.language.implicitConversions

object TestInterceptor
{
	// ATTRIBUTES   -------------------------
	
	private val counter = Iterator.iterate(1) { _ + 1 }
	
	
	// IMPLICIT -----------------------------
	
	implicit def objectToFactory(@unused o: TestInterceptor.type)(implicit log: Logger): TestInterceptorFactory =
		new TestInterceptorFactory()
	
	
	// NESTED   -----------------------------
	
	class TestInterceptorFactory(implicit logger: Logger) extends RequestInterceptorFactory[RequestContext[_]]
	{
		override def apply(request: Request[Any]): RequestInterceptor[RequestContext[_]] =
			new TestInterceptor(counter.next())
	}
}

/**
 * Logs requests
 * @author Mikko Hilpinen
 * @since 09.11.2025, v2.0
 */
class TestInterceptor(requestIndex: Int)(implicit logger: Logger) extends RequestInterceptor[RequestContext[_]]
{
	// ATTRIBUTES   ------------------------
	
	private val started = Now.toInstant
	
	
	// IMPLEMENTED  ------------------------
	
	override def interceptContext[C2 <: RequestContext[_]](context: C2, apiVersion: ApiVersion, path: Seq[String]): C2 = {
		logger(s"$requestIndex: Context set up: ${ context.request } in $apiVersion")
		context
	}
	
	override def interceptNotFound(preparedResult: RequestResult, path: Seq[String])
	                              (implicit context: RequestContext[_]): RequestResult =
	{
		logger(s"$requestIndex: ${ path.mkString("/") } Not found => $preparedResult")
		preparedResult
	}
	override def interceptExecutionNotAllowed(preparedResult: RequestResult, method: Method,
	                                          allowedMethods: Iterable[Method]): RequestResult =
	{
		logger(s"$requestIndex: $method not allowed => $preparedResult; Allowed = [${ allowedMethods.mkString(", ") }]")
		preparedResult
	}
	override def beforeExecution(method: Method)(implicit context: RequestContext[_]): Unit =
		logger(s"$requestIndex: Preparing to execute $method")
	override def interceptNodeResult(method: Method, result: RequestResult)
	                                (implicit context: RequestContext[_]): RequestResult =
	{
		logger(s"$requestIndex: $method yielded $result")
		result
	}
	override def interceptException(error: Throwable, preparedResult: RequestResult)
	                               (implicit context: RequestContext[_]): RequestResult =
	{
		logger(s"$requestIndex: Intercepted an exception: ${ error.getMessage } => $preparedResult")
		preparedResult
	}
	
	override def close(): Unit = logger(s"$requestIndex: Completed in ${ (Now - started).description }")
}
