package utopia.flow.view.template.eventful

import utopia.flow.collection.CollectionExtensions._

/**
  * Common trait for changing items where it may be possible that the changing stops at some point
  * @author Mikko Hilpinen
  * @since 31.8.2023, v2.2
  * @tparam A Type of the changing values in this item
  */
trait MayStopChanging[+A] extends Changing[A]
{
	// ABSTRACT ----------------------------
	
	/**
	  * This method should be called from the subclass once it is certain that this
	  * item will no longer change.
	  *
	  * Informs the assigned stop listeners.
	  * Clears all change-listeners and stop listeners.
	  */
	protected def declareChangingStopped(): Unit
	
	
	// OTHER    ----------------------------
	
	/**
	  * Prepares this pointer to declare a stop once the specified source pointer does the same.
	  * @param source A source pointer to follow
	  */
	protected def stopOnceSourceStops(source: Changing[_]) = onceSourceStops(source) { declareChangingStopped() }
	
	/**
	  * Prepares this pointer to declare a stop once all of the specified source pointers have
	  * done the same.
	  *
	  * If no pointers are specified, declares a stop immediately.
	  *
	  * @param sources Sources to follow
	  */
	protected def stopOnceAllSourcesStop(sources: Iterable[Changing[_]]) =
		onceAllSourcesStop(sources) { declareChangingStopped() }
	
	/**
	  * Prepares this pointer to perform an action in case the specified source pointer becomes fixed to a certain value
	  * @param source A source pointer
	  * @param stopValue Final value that triggers the specified function (call-by-name)
	  * @param action Action to trigger if the source pointer stops at the specified value
	  * @tparam B Type of stop values observed
	  */
	protected def onceSourceStopsAt[B](source: Changing[B], stopValue: => B)(action: => Unit) =
		onceSourceStops(source) { if (source.value == stopValue) action }
	
	/**
	  * Prepares this pointer to do something once the specified source pointer stops changing.
	  * @param source A source pointer to follow
	  * @param action Action to perform once / if the source pointer stops
	  */
	protected def onceSourceStops[U](source: Changing[_])(action: => U) =
		source.onceChangingStops { action }
	/**
	  * Prepares this pointer to perform an action once all the specified source pointers have stopped.
	  * If no pointers are specified, performs the action immediately.
	  * @param sources Sources to follow
	  * @param action Action to perform
	  */
	protected def onceAllSourcesStop[U](sources: Iterable[Changing[_]])(action: => U): Unit = {
		// Skips sources that have already stopped
		sources.filterNot { _.isFixed }.emptyOneOrMany match {
			// Case: Only one source pointer remains => Delegates to another method
			case Some(Left(source)) => onceSourceStops(source)(action)
			// Case: Multiple source pointers => Waits on all of them, but only if it is possible that all stop changing
			case Some(Right(sources)) =>
				if (sources.forall { _.destiny.isPossibleToSeal })
					_onceAllSourcesStop(sources.iterator)(action)
			// Case: No sources specified => Stops immediately
			case None => action
		}
	}
	
	// Recursively waits until each source stops, one by one
	// Once all have stopped (i.e. sourcesIter is empty), calls 'action'
	private def _onceAllSourcesStop[U](sourcesIter: Iterator[Changing[_]])(action: => U): Unit = {
		sourcesIter.next().onceChangingStops {
			if (sourcesIter.hasNext)
				_onceAllSourcesStop(sourcesIter)(action)
			else
				action
		}
	}
}
