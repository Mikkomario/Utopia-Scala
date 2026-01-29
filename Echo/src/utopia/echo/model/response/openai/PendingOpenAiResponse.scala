package utopia.echo.model.response.openai

import utopia.annex.model.manifest.SchrodingerState
import utopia.echo.model.response.ReplyLike
import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.{MutableOnce, SettableFlag}
import utopia.flow.view.template.eventful.{Changing, Flag}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Represents a pending Open AI response. Does not implement streaming at this time.
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.4.1
 */
// TODO: Possibly remove this class
class PendingOpenAiResponse(override val future: Future[Try[OpenAiResponse]])
                           (implicit log: Logger, exc: ExecutionContext)
	extends ReplyLike[OpenAiResponse]
{
	// ATTRIBUTES   ------------------------
	
	private val textP = MutableOnce("")
	private val thoughtsP = MutableOnce("")
	private val newTextP = Volatile.lockable("")
	
	private val thinkingCompletionFlag = SettableFlag()
	override val thinkingFlag: Flag = !thinkingCompletionFlag
	
	private val lastUpdatedP = MutableOnce(Now.toInstant)
	
	
	// INITIAL CODE ------------------------
	
	future.forResult {
		case Success(response) =>
			newTextP.value = response.thoughts
			thoughtsP.value = response.thoughts
			newTextP.value = ""
			thinkingCompletionFlag.set()
			
			newTextP.value = response.text
			textP.value = response.text
			lastUpdatedP.value = Now
			
			newTextP.lock()
		
		case Failure(_) =>
			thoughtsP.lock()
			thinkingCompletionFlag.set()
			newTextP.lock()
			textP.lock()
			lastUpdatedP.value = Now
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def isBuffered: Boolean = future.isCompleted
	
	override def text: String = textP.value
	override def thoughts: String = thoughtsP.value
	
	override def textPointer: Changing[String] = textP.readOnly
	override def thoughtsPointer: Changing[String] = thoughtsP.readOnly
	override def newTextPointer: Changing[String] = newTextP.readOnly
	
	override def lastUpdated: Instant = lastUpdatedP.value
	override def lastUpdatedPointer: Changing[Instant] = lastUpdatedP.readOnly
	
	override def state: SchrodingerState = SchrodingerState.of(future)
}
