package utopia.echo.model.response.llm

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.TryFuture
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object StreamedStatus
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * The expected final status string on success
	  */
	val expectedFinalStatus = "success"
	
	/**
	  * A completed success status ("success")
	  */
	lazy val success: StreamedStatus = SuccessCompletion()
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param statusPointer Pointer that contains the current status as a string
	  * @param completionFuture Future that resolves into either a success or a failure,
	  *                         once the associated process has completed
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  *            (used for handling completion flag errors and logging the possible completion failure)
	  * @return A new streamed status instance
	  */
	def apply(statusPointer: Changing[String], completionFuture: Future[Try[Unit]])
	         (implicit exc: ExecutionContext, log: Logger): StreamedStatus =
		_StreamedStatus(statusPointer, completionFuture)
	
	/**
	  * Creates a successfully completed status
	  * @param status Final completion status
	  * @return New successfully completed status
	  */
	def completed(status: String): StreamedStatus = SuccessCompletion(status)
	
	/**
	  * Creates a new completed status
	  * @param result Either the final status or a failure
	  * @return Completed status matching 'result'
	  */
	def completed(result: Try[String]): StreamedStatus = result match {
		case Success(status) => completed(status)
		case Failure(error) => failed(error)
	}
	
	/**
	  * Creates a failure completion
	  * @param cause Cause of failure
	  * @param status Status string (default = "failed")
	  * @return A new failure completion status
	  */
	def failed(cause: Throwable, status: String = "failed"): StreamedStatus = FailureCompletion(cause, status)
	
	
	// NESTED   ------------------------------
	
	private case class _StreamedStatus(statusPointer: Changing[String], completionFuture: Future[Try[Unit]])
	                                  (implicit exc: ExecutionContext, log: Logger)
		extends StreamedStatus
	{
		// ATTRIBUTES   ---------------------
		
		private val _completionFlag = SettableFlag()
		override lazy val completionFlag: Flag = _completionFlag.view
		
		
		// INITIAL CODE ---------------------
		
		completionFuture.foreachResult { result =>
			_completionFlag.set()
			result.log
		}
	}
	
	private case class SuccessCompletion(override val status: String = expectedFinalStatus) extends StreamedStatus
	{
		override lazy val statusPointer: Changing[String] = Fixed(status)
		
		override def completionFuture: Future[Try[Unit]] = TryFuture.successCompletion
		override def completionFlag: Flag = AlwaysTrue
	}
	
	private case class FailureCompletion(cause: Throwable, override val status: String = "failed")
		extends StreamedStatus
	{
		override lazy val statusPointer: Changing[String] = Fixed(status)
		
		override def completionFuture: Future[Try[Unit]] = TryFuture.failure(cause)
		override def completionFlag: Flag = AlwaysTrue
	}
}

/**
  * Used for communicating the status of some Ollama operation in real time.
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
trait StreamedStatus
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return A pointer that contains the current process status as a string.
	  *         Once completed, should contain "success".
	  */
	def statusPointer: Changing[String]
	
	/**
	  * @return A future that resolves once this operation completes.
	  *         If contains a failure, this process has failed
	  *         (however, it is possible that this process fails in another manner as well.
	  *         Always check the status to make sure)
	  */
	def completionFuture: Future[Try[Unit]]
	/**
	  * A flag that contains true once this pull process has completed.
	  */
	def completionFlag: Flag
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return The current status of this process as a string
	  */
	def status = statusPointer.value
	
	/**
	  * @return Whether this process has completed
	  */
	def completed = completionFlag.value
	/**
	  * @return Whether this process has not yet completed
	  */
	def incomplete = !completed
	
	/**
	  * A future that contains this process' final status, once this process completes.
	  * If successful, should contain "success".
	  */
	def finalStatusFuture(implicit exc: ExecutionContext) =
		completionFuture.map { _.map { _ => status } }
}
