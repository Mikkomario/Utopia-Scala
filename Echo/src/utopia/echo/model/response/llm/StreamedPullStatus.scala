package utopia.echo.model.response.llm

import utopia.flow.collection.immutable.Empty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object StreamedPullStatus
{
	// OTHER    ---------------------------
	
	/**
	  * Creates a completed pull status where nothing was downloaded
	  * @param status Final pull status
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @return A completed empty pull status
	  */
	def empty(status: String)(implicit exc: ExecutionContext, log: Logger) = completed(status)
	
	/**
	  * Creates a pull status representing a failure
	  * @param cause Cause of this failure
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @return A failed pull status
	  */
	def failed(cause: Throwable)(implicit exc: ExecutionContext, log: Logger): StreamedPullStatus =
		failed(cause.getMessage, cause)
	/**
	  * Creates a pull status representing a failure
	  * @param status Final pull status
	  * @param cause Cause of this failure
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @return A failed pull status
	  */
	def failed(status: String, cause: Throwable)(implicit exc: ExecutionContext, log: Logger) =
		completed(status, Failure(cause))
	
	/**
	  * Creates a pull status that has already been completed
	  * @param status Final pull status
	  * @param result Result of this pull operation (success or failure). Default = success.
	  * @param downloads Completed downloads as digest-size pairs. Default = empty.
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @return A completed pull status
	  */
	def completed(status: String, result: Try[Unit] = Success(()), downloads: Seq[(String, Long)] = Empty)
	             (implicit exc: ExecutionContext, log: Logger) =
		apply(Fixed(status),
			Fixed(downloads.map { case (digest, size) => StreamedDownloadStatus.completed(digest, size) }),
			Future.successful(result))
}

/**
  * Used for communicating the status of a pull operation in real time.
  * Pulling means retrieving model data from the remove Ollama service.
  * @param statusPointer A pointer that contains the current process status as a string.
  *                      Once completed, should contain "success".
  * @param downloadsPointer A pointer that contains the active and the past downloads in chronological order.
  * @param completionFuture A future that resolves once this operation completes.
  *                         If contains a failure, this process has failed
  *                         (however, it is possible that this process fails in another manner as well.
  *                         Always check the status to make sure)
  * @author Mikko Hilpinen
  * @since 03.09.2024, v1.1
  */
case class StreamedPullStatus(statusPointer: Changing[String], downloadsPointer: Changing[Seq[StreamedDownloadStatus]],
                              completionFuture: Future[Try[Unit]])
                             (implicit exc: ExecutionContext, log: Logger)
{
	// ATTRIBUTES   -------------------------
	
	private val _completionFlag = SettableFlag()
	
	/**
	  * A flag that contains true once this pull process has completed.
	  */
	lazy val completionFlag = _completionFlag.view
	/**
	  * A pointer that contains the latest download process or None, if no downloads have been started
	  */
	lazy val latestDownloadPointer = downloadsPointer.map { _.lastOption }
	/**
	  * A pointer that contains the currently active incomplete download.
	  * Contains None if no downloads have been started or when all started downloads have been completed.
	  */
	lazy val activeDownloadPointer = latestDownloadPointer.flatMap {
		case Some(download) => download.completionFlag.map { if (_) None else Some(download) }
		case None => Fixed(None)
	}
	
	/**
	  * A future that contains this process' final status, once this process completes.
	  * If successful, should contain "success".
	  */
	lazy val finalStatusFuture = completionFuture.map { _.map { _ => status } }
	
	
	// INITIAL CODE -------------------------
	
	completionFuture.onComplete { _ => _completionFlag.set() }
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return The current status of this process as a string
	  */
	def status = statusPointer.value
	
	/**
	  * @return The current and past download processes within this process. In chronological order.
	  */
	def downloads = downloadsPointer.value
	
	/**
	  * @return Whether this process has completed
	  */
	def completed = _completionFlag.value
	/**
	  * @return Whether this process has not yet completed
	  */
	def incomplete = !completed
}
