package utopia.echo.model.response

import utopia.flow.time.Now
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object StreamedReply
{
	/**
	  * @return An empty reply (final)
	  */
	def empty = success("", ResponseStatistics.empty)
	
	def success(text: String, statistics: ResponseStatistics, lastUpdated: Instant = Now) =
		completed(Success(statistics), text, lastUpdated)
	
	def failure(cause: Throwable) = completed(Failure(cause))
	
	def completed(statistics: Try[ResponseStatistics], text: String = "", lastUpdated: Instant = Now) =
		apply(Fixed(text), Fixed(lastUpdated), Future.successful(statistics))
		
	def apply(textPointer: Changing[String], lastUpdatedPointer: Changing[Instant],
	          statisticsFuture: Future[Try[ResponseStatistics]]) =
		new StreamedReply(textPointer, lastUpdatedPointer, statisticsFuture)
}

/**
  * Represents a reply from an LLM
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  * @param textPointer A pointer which contains the reply message as text.
  *                    Will stop changing once this reply has been fully generated / read.
  * @param lastUpdatedPointer A pointer which contains origin time of the latest version of text in this reply
  * @param statisticsFuture A future that resolves into statistics about this response,
  *                         once this response has been fully generated.
  *                         Will contain a failure if reply parsing failed.
  */
class StreamedReply(val textPointer: Changing[String], val lastUpdatedPointer: Changing[Instant],
                    val statisticsFuture: Future[Try[ResponseStatistics]])
