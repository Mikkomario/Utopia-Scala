package utopia.echo.model.response
import utopia.annex.model.manifest.SchrodingerState
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

/**
  * An implementation of [[OllamaResponseLike]] by wrapping another response.
 *
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait OllamaResponseWrapper[+Buffered] extends OllamaResponseLike[Buffered]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return The wrapped reply instance
	  */
	protected def wrapped: OllamaResponseLike[Buffered]
	
	
	// IMPLEMENTED  ------------------------
	
	override def isBuffered: Boolean = wrapped.isBuffered
	override def future: Future[Try[Buffered]] = wrapped.future
	override def statisticsFuture: Future[Try[ResponseStatistics]] = wrapped.statisticsFuture
	
	override def text: String = wrapped.text
	override def textPointer: Changing[String] = wrapped.textPointer
	override def newTextPointer: Changing[String] = wrapped.newTextPointer
	
	override def lastUpdated: Instant = wrapped.lastUpdated
	override def lastUpdatedPointer: Changing[Instant] = wrapped.lastUpdatedPointer
	
	override def state: SchrodingerState = wrapped.state
}
