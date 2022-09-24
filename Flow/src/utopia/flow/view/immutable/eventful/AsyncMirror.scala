package utopia.flow.view.immutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.{ChangingLike, ChangingWrapper}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object AsyncMirror
{
	/**
	  * Creates a new asynchronous mirror where failures are simply logged and treated as if no mapping happened.
	  * @param source A source pointer
	  * @param placeHolder A placeholder for the first map result.
	  *                    Used until a real value has been resolved.
	  * @param skipInitialProcess Whether the initial source value should not be mapped.
	  *                           Suitable for situations where the placeholder is accurate.
	  *                           Default = false = immediately start mapping.
	  * @param map An asynchronous mapping function that yields a success or a failure.
	  * @param exc Implicit execution context
	  * @param logger Implicit logger to record encountered errors with
	  * @tparam Origin Original pointer value type
	  * @tparam Reflection Successful mapping result type
	  * @return A new asynchronously mirroring pointer
	  */
	def tryCatching[Origin, Reflection](source: ChangingLike[Origin], placeHolder: Reflection,
	                                    skipInitialProcess: Boolean = false)
	                                   (map: Origin => Future[Try[Reflection]])
	                                   (implicit exc: ExecutionContext, logger: Logger) =
	{
		apply[Origin, Try[Reflection], Reflection](source, placeHolder, skipInitialProcess)(map) { (previous, result) =>
			result.flatten.getOrMap { error =>
				logger(error, s"Asynchronous mapping failed. Reverting back to $previous")
				previous
			}
		}
	}
	
	/**
	  * Creates a new asynchronous mirror where failures are simply logged and treated as if no mapping happened.
	  * @param source A source pointer
	  * @param placeHolder A placeholder for the first map result.
	  *                    Used until a real value has been resolved.
	  * @param skipInitialProcess Whether the initial source value should not be mapped.
	  *                           Suitable for situations where the placeholder is accurate.
	  *                           Default = false = immediately start mapping.
	  * @param map An asynchronous mapping function
	  * @param exc Implicit execution context
	  * @param logger Implicit logger to record encountered errors with
	  * @tparam Origin Original pointer value type
	  * @tparam Reflection Successful mapping result type
	  * @return A new asynchronously mirroring pointer
	  */
	def catching[Origin, Reflection](source: ChangingLike[Origin], placeHolder: Reflection,
	                                 skipInitialProcess: Boolean = false)
	                                (map: Origin => Future[Reflection])
	                                (implicit exc: ExecutionContext, logger: Logger) =
	{
		apply[Origin, Reflection, Reflection](source, placeHolder, skipInitialProcess)(map) { (previous, result) =>
			result.getOrMap { error =>
				logger(error, s"Asynchronous mapping failed. Reverts back to $previous")
				previous
			}
		}
	}
	
	/**
	  * Creates a new mirror that asynchronously maps the values of another pointer
	  * @param source Pointer that's being mapped
	  * @param placeHolder A placeholder for the first map result.
	  *                    Used until a real value has been resolved.
	  * @param skipInitialProcess Whether the initial source value should not be mapped.
	  *                           Suitable for situations where the placeholder is accurate.
	  *                           Default = false = immediately start mapping.
	  * @param map An asynchronous mapping function
	  * @param merge A function that merges mapping results and previously acquired value into a new pointer value.
	  *              Handles possible mapping function errors, also.
	  * @param exc Implicit execution context
	  * @tparam Origin Original pointer value type
	  * @tparam Result Mapping result type before merging
	  * @tparam Reflection Mapping result type after merging
	  * @return A new asynchronous mirror
	  */
	def apply[Origin, Result, Reflection](source: ChangingLike[Origin], placeHolder: Reflection,
	                                      skipInitialProcess: Boolean = false)
	                                     (map: Origin => Future[Result])
	                                     (merge: (Reflection, Try[Result]) => Reflection)
	                                     (implicit exc: ExecutionContext) =
	{
		// Case: Mapping required => constructs a proper mirror
		if (source.isChanging || !skipInitialProcess)
			new AsyncMirror[Origin, Result, Reflection](source, placeHolder, skipInitialProcess)(map)(merge)
		// Case: Fixed source and no initial mapping required => Constructs a fixed pointer
		else
			Fixed(AsyncMirrorValue(placeHolder))
	}
	
	
	// NESTED   -------------------------
	
	object AsyncMirrorValue
	{
		// Async mirror values may be implicitly converted to their currently held value
		implicit def autoUnwrap[R](v: AsyncMirrorValue[_, R]): R = v.current
	}
	
	/**
	  * A value state of an asynchronous mirror. Contains the currently held value, as well as information concerning
	  * current and future processes.
	  * @param current The last calculated value
	  * @param activeOrigin The source value that's currently being processed.
	  *                     None if no value is being processed at this time (default)
	  * @param queuedOrigin The source value that's currently queued to be processed after the current process has
	  *                     completed.
	  *                     None if no process has been queued (default).
	  * @tparam Origin The type of process origins
	  * @tparam Reflection The type of resulting (asynchronously mapped) values
	  */
	case class AsyncMirrorValue[+Origin, +Reflection](current: Reflection, activeOrigin: Option[Origin] = None,
	                                                  queuedOrigin: Option[Origin] = None)
	{
		// COMPUTED -------------------------
		
		/**
		  * @return Whether a value is being processed asynchronously
		  */
		def isProcessing = activeOrigin.isDefined
		/**
		  * @return Whether no value is being processed asynchronously right now
		  */
		def isNotProcessing = !isProcessing
		
		/**
		  * @return A copy of this value without the queued process
		  */
		def withoutQueue = copy(queuedOrigin = None)
	}
}

/**
  * This mirror (mapped view of a changed item) performs the mapping asynchronously, which
  * is useful when the mapping operation takes a while to complete and shouldn't freeze/block any of the active threads
  * while doing so.
  * @author Mikko Hilpinen
  * @since 23.9.2020, v1.9
  * @param source Pointer that's being mapped
  * @param initialPlaceHolder A placeholder for the first map result.
  *                           Used until a real value has been resolved.
  * @param f An asynchronous mapping function
  * @param merge A function that merges mapping results and previously acquired value into a new pointer value.
  *              Handles possible mapping function errors, also.
  * @param exc Implicit execution context
  * @tparam Origin Original pointer value type
  * @tparam Result Mapping result type before merging
  * @tparam Reflection Mapping result type after merging
  */
class AsyncMirror[Origin, Result, Reflection](val source: ChangingLike[Origin], initialPlaceHolder: Reflection,
                                              skipInitialProcess: Boolean = false)
                                             (f: Origin => Future[Result])
                                             (merge: (Reflection, Try[Result]) => Reflection)
                                             (implicit exc: ExecutionContext)
	extends ChangingWrapper[AsyncMirrorValue[Origin, Reflection]]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * Type of value stored in this mirror
	  */
	type Value = AsyncMirrorValue[Origin, Reflection]
	
	private val pointer = Volatile[Value](
		AsyncMirrorValue(initialPlaceHolder, if (skipInitialProcess) None else Some(source.value)))
	
	
	// INITIAL CODE ---------------------
	
	// May immediately start to process the first value
	if (!skipInitialProcess)
		f(source.value).onComplete(resultsArrived)
	
	// Whenever source value changes, requests an asynchronous status update
	source.addListener(ChangeListener.continuous { event => requestCalculation(event.newValue) })
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def wrapped = pointer
	
	
	// OTHER    -------------------------
	
	private def requestCalculation(newOrigin: Origin) = {
		val shouldProcess = pointer.pop { current =>
			// Case: A new calculation has already been queued
			if (current.queuedOrigin.isDefined) {
				// Case: Current calculation solves this new item => skips the previously queued item
				if (current.activeOrigin.contains(newOrigin))
					false -> current.withoutQueue
				// Case: New calculation is required => overwrites the queued calculation
				else
					false -> current.copy(queuedOrigin = Some(newOrigin))
			}
			// Case: Current calculation handles same case => Doesn't modify
			else if (current.activeOrigin.contains(newOrigin))
				false -> current
			// Case: Previous calculation is still going on and needs to be completed first => queues this calculation
			else if (current.isProcessing)
				false -> current.copy(queuedOrigin = Some(newOrigin))
			// Case: Previous calculation has been completed => Starts a new one
			else
				true -> current.copy(activeOrigin = Some(newOrigin))
		}
		if (shouldProcess)
			f(newOrigin).onComplete(resultsArrived)
	}
	
	private def resultsArrived(result: Try[Result]): Unit =
	{
		val queued = pointer.pop { current =>
			// Determines the new value by merging the result with the previous value
			current.queuedOrigin -> AsyncMirrorValue(merge(current.current, result), current.queuedOrigin)
		}
		// Starts a new computation (if needed)
		queued.foreach { f(_).onComplete(resultsArrived) }
	}
}
