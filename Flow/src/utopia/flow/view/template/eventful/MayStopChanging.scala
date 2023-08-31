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
	protected def stopOnceSourceStops(source: Changing[_]) =
		source.addChangingStoppedListenerAndSimulateEvent { declareChangingStopped() }
	
	/**
	  * Prepares this pointer to declare a stop once all of the specified source pointers have
	  * done the same.
	  *
	  * If no pointers are specified, declares a stop immediately.
	  *
	  * @param sources Sources to follow
	  */
	protected def stopOnceAllSourcesStop(sources: Iterable[Changing[_]]) = {
		sources.emptyOneOrMany match {
			// Case: Only one source pointer => Delegates to another method
			case Some(Left(source)) => stopOnceSourceStops(source)
			// Case: Multiple source pointers => Waits on all of them, but only if it is possible that all stop changing
			case Some(Right(sources)) =>
				if (sources.forall { _.mayStopChanging }) {
					var stopCount = 0
					sources.foreach {
						_.addChangingStoppedListenerAndSimulateEvent {
							stopCount += 1
							// Case: All sources stopped changing
							if (sources hasSize stopCount)
								declareChangingStopped()
						}
					}
				}
			// Case: No sources specified => Stops immediately
			case None => declareChangingStopped()
		}
	}
}
