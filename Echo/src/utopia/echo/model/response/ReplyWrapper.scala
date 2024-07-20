package utopia.echo.model.response
import utopia.annex.model.manifest.SchrodingerState
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

/**
  * An implementation of [[Reply]] by wrapping another Reply.
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait ReplyWrapper extends Reply
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return The wrapped reply instance
	  */
	protected def wrapped: Reply
	
	
	// IMPLEMENTED  ------------------------
	
	override def future: Future[Try[BufferedReply]] = wrapped.future
	override def statisticsFuture: Future[Try[ResponseStatistics]] = wrapped.statisticsFuture
	
	override def text: String = wrapped.text
	override def textPointer: Changing[String] = wrapped.textPointer
	
	override def lastUpdated: Instant = wrapped.lastUpdated
	override def lastUpdatedPointer: Changing[Instant] = wrapped.lastUpdatedPointer
	
	override def state: SchrodingerState = wrapped.state
}
