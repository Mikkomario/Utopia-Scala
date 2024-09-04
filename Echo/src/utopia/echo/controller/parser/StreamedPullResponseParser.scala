package utopia.echo.controller.parser

import utopia.echo.model.response.llm.{StreamedDownloadStatus, StreamedPullStatus}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.LockablePointer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * A response parser which tracks the state of a streamed pull request
  * @author Mikko Hilpinen
  * @since 03.09.2024, v1.1
  */
class StreamedPullResponseParser(implicit override protected val exc: ExecutionContext,
                                 override protected val jsonParser: JsonParser, override val log: Logger)
	extends StreamedResponseParser[StreamedPullStatus, Unit]
{
	// IMPLEMENTED  ------------------------
	
	override protected def emptyResponse: StreamedPullStatus = StreamedPullStatus.empty("no content")
	
	override protected def newParser: SingleStreamedResponseParser[StreamedPullStatus, Unit] =
		new SingleStreamedPullResponseParser
	
	override protected def failureMessageFrom(response: StreamedPullStatus): String = response.status
	
	
	// NESTED   ----------------------------
	
	private class SingleStreamedPullResponseParser extends SingleStreamedResponseParser[StreamedPullStatus, Unit]
	{
		// ATTRIBUTES   --------------------
		
		private val statusPointer = LockablePointer("not started")
		private val downloadsPointer = LockablePointer.emptySeq[StreamedDownloadStatus]
		
		private var ongoingDownload: Option[DownloadResponseParser] = None
		
		
		// IMPLEMENTED  --------------------
		
		override def updateStatus(response: Model): Unit = {
			// Updates the status pointer
			response("status").string.foreach { statusPointer.value = _ }
			
			// Checks whether this is a downloading status and updates the current download tracker, if needed
			response("digest").string.foreach { digest =>
				ongoingDownload = ongoingDownload
					// Case: We already have an ongoing download => Checks whether this response starts a new one
					.flatMap { ongoingDownload =>
						// Case: This response concerns that same download
						if (ongoingDownload.digest == digest)
							Some(ongoingDownload)
						// Case: This response concerns a new download
						//       => Completes the previous download & starts a new one
						else {
							ongoingDownload.complete()
							None
						}
					}
					// Case: This is a new download => Checks the total size of this download
					.orElse {
						response("total").long match {
							case Some(totalSize) =>
								// Forms and remembers this new download
								val newDownload = new DownloadResponseParser(digest, totalSize)
								downloadsPointer.update { _ :+ newDownload.download }
								Some(newDownload)
							
							// Case: Total size couldn't be determined (error) => Logs and skips download tracking
							case None =>
								log(s"A downloading response didn't contain the \"total\" property. Defined properties: [${
									response.nonEmptyPropertiesIterator.map { _.name }.mkString(", ") }]")
								None
						}
					}
			}
			// Updates the current download progress, if applicable
			response("completed").long.foreach { completion =>
				ongoingDownload.foreach { download =>
					if (download.updateProgress(completion))
						ongoingDownload = None
				}
			}
		}
		
		override def processFinalParseResult(finalResponse: Try[Model]): Try[Unit] = finalResponse.map { _ => () }
		
		override def finish(): Unit = {
			// Locks the pointers and marks the last download as completed (if not completed already)
			ongoingDownload.foreach { _.complete() }
			ongoingDownload = None
			
			downloadsPointer.lock()
			statusPointer.lock()
		}
		
		override def responseFrom(future: Future[Try[Unit]]): StreamedPullStatus =
			StreamedPullStatus(statusPointer.readOnly, downloadsPointer.readOnly, future)
	}
	
	private class DownloadResponseParser(val digest: String, totalSize: Long)
	{
		// ATTRIBUTES   ---------------------
		
		private val downloadedPointer = LockablePointer(0L)
		
		val download = StreamedDownloadStatus(digest, totalSize, downloadedPointer.readOnly)
		
		
		// OTHER    -------------------------
		
		// Returns whether this download was completed
		def updateProgress(downloaded: Long) = {
			if (!downloadedPointer.locked) {
				downloadedPointer.value = downloaded
				if (downloaded >= totalSize) {
					downloadedPointer.lock()
					true
				}
				else
					false
			}
			else
				true
		}
		
		def complete() = {
			if (!downloadedPointer.locked) {
				downloadedPointer.value = totalSize
				downloadedPointer.lock()
			}
		}
	}
}
