package utopia.echo.controller.chat

import utopia.annex.controller.RequestQueue
import utopia.annex.model.request.ApiRequest
import utopia.echo.controller.client.{LlmServiceClient, OllamaClient}
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.deepseek.BufferedDeepSeekChatRequest
import utopia.echo.model.request.ollama.chat.BufferedOllamaChatRequest
import utopia.echo.model.request.openai.BufferedOpenAiChatCompletionRequest
import utopia.echo.model.request.vllm.BufferedVllmChatCompletionRequest
import utopia.echo.model.response.BufferedReply

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object BufferingChatRequestExecutor
{
	// COMPUTED ---------------------------
	
	/**
	 * @param client Implicit Ollama client
	 * @param exc Implicit execution context
	 * @return A new interface for executing buffered Ollama chat requests
	 */
	def ollama(implicit client: OllamaClient, exc: ExecutionContext) =
		queueing(client)(BufferedOllamaChatRequest.apply)
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param client Open AI client instance
	 * @param exc Implicit execution context
	 * @return An interface for executing buffered chat completion requests
	 */
	def openAi(client: LlmServiceClient)(implicit exc: ExecutionContext) =
		queueing(client)(BufferedOpenAiChatCompletionRequest.apply)
	/**
	 * @param client DeepSeek client instance
	 * @param exc Implicit execution context
	 * @return A new interface for executing buffered DeepSeek requests
	 */
	def deepSeek(client: LlmServiceClient)(implicit exc: ExecutionContext) =
		queueing(client)(BufferedDeepSeekChatRequest.apply)
	/**
	 * @param client A client instance for connecting to a vLLM server
	 * @param exc Implicit execution context
	 * @return A new interface for executing buffered requests using vLLM
	 */
	def vllm(client: RequestQueue)(implicit exc: ExecutionContext) =
		queueing(client)(BufferedVllmChatCompletionRequest.apply)
	
	/**
	 * @param queue Request queue to use
	 * @param f A function which constructs an API request based on the request parameters
	 * @param exc Implicit execution context
	 * @tparam R Type of the acquired replies
	 * @return An interface for executing chat requests
	 */
	def queueing[R](queue: RequestQueue)(f: ChatParams => ApiRequest[R])
	               (implicit exc: ExecutionContext): QueueingBufferedRequestExecutor[R] =
		new _QueuingBufferedRequestExecutor[R](queue)(f)
	
	/**
	 * @param f A function which executes a chat request.
	 *          Accepts request parameters and yields a future that resolves into the acquired reply, if successful.
	 * @tparam R Type of the replies received.
	 * @return A new interface for executing chat requests
	 */
	def apply[R](f: ChatParams => Future[Try[R]]): BufferingChatRequestExecutor[R] =
		new _BufferingChatRequestExecutor[R](f)
		
	
	// EXTENSIONS   -----------------------
	
	implicit class ToBufferedReplyExecutor[+R <: BufferedReply](val e: BufferingChatRequestExecutor[R]) extends AnyVal
	{
		/**
		 * @param exc Implicit execution context
		 * @return A wrapper of this executor which counts the processed requests
		 */
		def counting(implicit exc: ExecutionContext) = e match {
			case counting: CountingBufferingChatRequestExecutorWrapper[R] => counting
			case e => new CountingBufferingChatRequestExecutorWrapper[R](e)
		}
	}
	
	
	// NESTED   ---------------------------
	
	/**
	 * Common trait for implementations of [[BufferingChatRequestExecutor]], which utilize a [[RequestQueue]]
	 * @tparam R Type of the acquired replies
	 */
	trait QueueingBufferedRequestExecutor[+R] extends BufferingChatRequestExecutor[R]
	{
		// ABSTRACT -----------------------
		
		/**
		 * @return Implicit execution context to use
		 */
		protected implicit def exc: ExecutionContext
		/**
		 * @return Request queue used for sending the requests
		 */
		protected def queue: RequestQueue
		
		/**
		 * @param params Request parameters
		 * @return A reques to send out
		 */
		protected def makeRequest(params: ChatParams): ApiRequest[R]
		
		
		// IMPLEMENTED  -------------------
		
		override def apply(params: ChatParams): Future[Try[R]] = queue.push(makeRequest(params)).future.map { _.toTry }
	}
	
	private class _QueuingBufferedRequestExecutor[+R](override protected val queue: RequestQueue)
	                                                 (f: ChatParams => ApiRequest[R])
	                                                 (implicit override protected val exc: ExecutionContext)
		extends QueueingBufferedRequestExecutor[R]
	{
		override protected def makeRequest(params: ChatParams): ApiRequest[R] = f(params)
	}
	
	private class _BufferingChatRequestExecutor[R](f: ChatParams => Future[Try[R]])
		extends BufferingChatRequestExecutor[R]
	{
		override def apply(params: ChatParams): Future[Try[R]] = f(params)
	}
}

/**
 * Common trait for interfaces which perform some kind of chat requests
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
trait BufferingChatRequestExecutor[+R]
{
	// ABSTRACT ------------------------
	
	/**
	 * @param params Parameters for creating the request
	 * @return A future that will yield a reply for that request, if successful
	 */
	def apply(params: ChatParams): Future[Try[R]]
}
