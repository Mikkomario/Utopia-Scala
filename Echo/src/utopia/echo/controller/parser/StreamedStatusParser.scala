package utopia.echo.controller.parser

import utopia.echo.model.response.llm.StreamedStatus
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.LockablePointer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Used for parsing streamed responses that only contain a "status" property
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
class StreamedStatusParser(implicit override protected val exc: ExecutionContext,
                           override protected val jsonParser: JsonParser, override val log: Logger)
	extends StreamedResponseParser[StreamedStatus, Unit]
{
	// IMPLEMENTED  ----------------------
	
	override protected def emptyResponse: StreamedStatus = StreamedStatus.completed("no content")
	
	override protected def newParser: SingleStreamedResponseParser[StreamedStatus, Unit] =
		new SingleStreamedStatusParser
	
	override protected def failureMessageFrom(response: StreamedStatus): String = response.status
	
	
	// NESTED   --------------------------
	
	private class SingleStreamedStatusParser extends SingleStreamedResponseParser[StreamedStatus, Unit]
	{
		// ATTRIBUTES   ------------------
		
		private val statusPointer = LockablePointer("")
		
		
		// IMPLEMENTED  ------------------
		
		override def updateStatus(response: Model): Unit = response("status").string.foreach { statusPointer.value = _ }
		override def processFinalParseResult(finalResponse: Try[Model]): Try[Unit] = finalResponse.map { _ => () }
		override def finish(): Unit = statusPointer.lock()
		
		override def responseFrom(future: Future[Try[Unit]]): StreamedStatus = StreamedStatus(statusPointer, future)
	}
}
