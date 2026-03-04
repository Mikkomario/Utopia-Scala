package utopia.echo.controller.chat

import utopia.echo.model.request.ChatParams
import utopia.echo.model.response.{BufferedReply, TokenUsage}
import utopia.echo.model.tokenization.LlmRequestCount
import utopia.flow.async.AsyncExtensions._
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * A wrapper for [[BufferingChatRequestExecutor]] which counts the requests and tokens passing through
 * @tparam R Type of replies generated
 * @param wrapped The wrapped executor
 * @param exc Implicit execution context
 * @author Mikko Hilpinen
 * @since 04.03.2026, v1.5
 */
class CountingBufferingChatRequestExecutorWrapper[+R <: BufferedReply](wrapped: BufferingChatRequestExecutor[R])
                                                                      (implicit exc: ExecutionContext)
	extends BufferingChatRequestExecutor[R]
{
	// ATTRIBUTES   --------------------------
	
	private val requestsInP = Volatile(0)
	private val requestsOutP = Volatile(0)
	private val failuresP = Volatile(0)
	private val tokensP = Volatile(TokenUsage.zero)
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return The current request counts
	 */
	def count = LlmRequestCount(requests, completedRequests, failures, tokens)
	
	/**
	 * @return Number of requests performed (including those currently being executed)
	 */
	def requests = requestsInP.value
	/**
	 * @return Number of requests completed
	 */
	def completedRequests = requestsOutP.value
	/**
	 * @return Number of currently pending requests
	 */
	def pendingRequests = requests - completedRequests
	
	/**
	 * @return Number of tokens in the completed requests
	 */
	def tokens = tokensP.value
	/**
	 * @return Number of failed requests
	 */
	def failures = failuresP.value
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(params: ChatParams): Future[Try[R]] = {
		// Delegates the request execution
		requestsInP.update { _ + 1 }
		val result = wrapped(params)
		
		// Once the result arrives, updates counters
		result.forResult { result =>
			requestsOutP.update { _ + 1 }
			result match {
				case Success(reply) => tokensP.update { _ + reply.tokenUsage }
				case Failure(_) => failuresP.update { _ + 1 }
			}
		}
		
		result
	}
}
