package utopia.flow.collection.mutable.iterator

import utopia.flow.event.model.ChangeResponse.Detach
import utopia.flow.view.template.eventful.Changing

import scala.util.{Failure, Success, Try}

/**
  * An iterator that iterates normally until an external failure is delivered to its attention.
  * Used in situations where you want to stop iterating once a failure occurs, in special situations where that
  * failure is only available via logging. Typically used in combination with
  * [[utopia.flow.util.logging.CollectSingleFailureLogger]].
  * @author Mikko Hilpinen
  * @since 16.10.2024, v2.5.1
  */
class UntilExternalFailureIterator[A](wrapped: Iterator[A], failurePointer: Changing[Option[Throwable]])
	extends Iterator[Try[A]]
{
	// ATTRIBUTES   ----------------------
	
	// Contains Some while waiting to return a failure
	private var polledFailure: Option[Throwable] = None
	// Contains true once this iterator has yielded a failure
	private var terminated = false
	
	
	// INITIAL CODE ----------------------
	
	// Once the failurePointer contains a failure, prepares to yield it
	failurePointer.addListenerAndSimulateEvent(None) { e =>
		e.newValue.foreach { error => polledFailure = Some(error) }
		Detach
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def hasNext: Boolean = !terminated && (polledFailure.isDefined || wrapped.hasNext)
	
	override def next(): Try[A] = polledFailure match {
		// Case: A failure has been queued => Yields it and terminates iterating
		case Some(failure) =>
			val result = Failure(failure)
			polledFailure = None
			terminated = true
			result
			
		// Case: No failure encountered => Yields an item from the source iterator
		case None => Success(wrapped.next())
	}
}
