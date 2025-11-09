package utopia.nexus.controller.api.interceptor

import utopia.access.model.enumeration.Method
import utopia.nexus.model.api.ApiVersion
import utopia.nexus.model.response.RequestResult

/**
 * Represents a potentially stateful request interception logic,
 * which may be triggered at different points of request handling.
 * May modify the request-handling process at different points.
 * @author Mikko Hilpinen
 * @since 07.11.2025, v2.0
 */
trait RequestInterceptor[-C] extends AutoCloseable
{
	/**
	 * This method is called immediately after a request context has been constructed,
	 * providing this instance an opportunity to modify said context.
	 * @param context Prepared request context
	 * @param apiVersion Targeted API version.
	 *                   If no API version was targeted (the request path being invalid),
	 *                   the latest API version is passed here.
	 * @param path Targeted request path (with API root path excluded)
	 * @tparam C2 Type of the accepted & generated request context
	 *            (this interceptor must yield the same type of context)
	 * @return A (potentially) modified request context to apply for the remaining part of the request-handling process
	 */
	def interceptContext[C2 <: C](context: C2, apiVersion: ApiVersion, path: Seq[String]): C2
	
	/**
	 * This method is called if no API node was targeted with the request,
	 * and a 404 (not found) result is generated instead.
	 *
	 * This function is never called in the same sequence where
	 * [[interceptExecutionNotAllowed]] or [[interceptException]] is called.
	 *
	 * @param preparedResult The result that has been generated to be sent to the client
	 * @param path Targeted request path (with API root path excluded)
	 * @param context Implicit request context
	 * @return Result to send out to the client
	 */
	def interceptNotFound(preparedResult: RequestResult, path: Seq[String])(implicit context: C): RequestResult
	/**
	 * This method is called if the targeted API node has not been prepared to execute the specified request method.
	 * In these instances, either 405 (method not allowed) or 501 (not implemented) is sent out by default.
	 *
	 * This function is never called in the same sequence where
	 * [[interceptNotFound]] or [[interceptException]] is called.
	 *
	 * @param preparedResult The result that has been generated to be sent to the client
	 * @param method The method which was not allowed
	 * @param allowedMethods The methods which would have been allowed (may be empty).
	 * @return Result to send out to the client
	 */
	def interceptExecutionNotAllowed(preparedResult: RequestResult, method: Method,
	                                 allowedMethods: Iterable[Method]): RequestResult
	/**
	 * This method is called right before requesting an ApiNode to execute a request.
	 *
	 * This function is never called in the same sequence where
	 * [[interceptNotFound]] or [[interceptExecutionNotAllowed]] is called.
	 *
	 * Unless the node throws an exception (in which case [[interceptException]] will be called),
	 * this method call will be followed by a call to [[interceptNodeResult]].
	 *
	 * @param method The method that's about to be executed
	 * @param context Request context about to be applied
	 */
	def beforeExecution(method: Method)(implicit context: C): Unit
	/**
	 * This method is called after the targeted ApiNode has executed a request.
	 * @param method  The method that was executed.
	 * @param result  Generated RequestResult
	 * @param context Request context applied during the execution (i.e. the one acquired via [[beforeExecution]]).
	 * @return Result to send out to the client
	 */
	def interceptNodeResult(method: Method, result: RequestResult)(implicit context: C): RequestResult
	
	/**
	 * This method is called in case request-execution fails and throws an Exception.
	 * @param error The exception thrown during the request-handling process (likely by the targeted ApiNode)
	 * @param preparedResult Result prepared to be sent to the client (505, internal server error, by default)
	 * @param context Applicable request context
	 * @return Result to send out to the client
	 */
	def interceptException(error: Throwable, preparedResult: RequestResult)(implicit context: C): RequestResult
}
