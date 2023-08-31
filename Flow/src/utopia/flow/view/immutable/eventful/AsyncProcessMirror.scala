package utopia.flow.view.immutable.eventful

import utopia.flow.async.process.Process
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AsyncMirror.AsyncMirrorValue
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper, MayStopChanging}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.util.Try

object AsyncProcessMirror
{
	/**
	  * Creates a new asynchronous mirror where failures are simply logged and treated as if no mapping happened.
	  * @param source             Pointer that's being mapped
	  * @param placeHolder        A placeholder for the first map result.
	  *                           Used until a real value has been resolved.
	  * @param skipInitialProcess Whether the initial mapping operation should be skipped.
	  *                           Set to true if the placeholder value sufficiently represents the would-be mapping result.
	  *                           Default = false = mapping will start immediately.
	  * @param map                A synchronous mapping function that may fail
	  * @param exc                Implicit execution context
	  * @param log                Implicit logging implementation for encountered errors
	  * @tparam Origin     Original pointer value type
	  * @tparam Reflection Successful mapping result type
	  * @return A new asynchronously mirroring pointer
	  */
	def tryCatching[Origin, Reflection](source: Changing[Origin], placeHolder: Reflection,
	                                    skipInitialProcess: Boolean = false)
	                                   (map: Origin => Try[Reflection])
	                                   (implicit exc: ExecutionContext, log: Logger) =
	{
		merging[Origin, Try[Reflection], Reflection](source, placeHolder, skipInitialProcess)(map) { (previous, result) =>
			result.getOrMap { error =>
				log(error, s"Asynchronous mapping failed. Reverting back to $previous")
				previous
			}
		}
	}
	
	/**
	  * Creates a new asynchronous mirror where failures are simply logged and treated as if no mapping happened.
	  * @param source             Pointer that's being mapped
	  * @param placeHolder        A placeholder for the first map result.
	  *                           Used until a real value has been resolved.
	  * @param skipInitialProcess Whether the initial mapping operation should be skipped.
	  *                           Set to true if the placeholder value sufficiently represents the would-be mapping result.
	  *                           Default = false = mapping will start immediately.
	  * @param map                A synchronous mapping function
	  * @param exc                Implicit execution context
	  * @param log                Implicit logging implementation for encountered errors
	  * @tparam Origin     Original pointer value type
	  * @tparam Reflection Successful mapping result type
	  * @return A new asynchronously mirroring pointer
	  */
	def apply[Origin, Reflection](source: Changing[Origin], placeHolder: Reflection,
	                              skipInitialProcess: Boolean = false)
	                             (map: Origin => Reflection)
	                             (implicit exc: ExecutionContext, log: Logger): Changing[AsyncMirrorValue[Origin, Reflection]] =
		merging[Origin, Reflection, Reflection](source, placeHolder, skipInitialProcess)(map) { (_, result) => result }
	
	/**
	  * Creates a new mirror that asynchronously maps the values of another pointer
	  * @param source             Pointer that's being mapped
	  * @param placeHolder        A placeholder for the first map result.
	  *                           Used until a real value has been resolved.
	  * @param skipInitialProcess Whether the initial mapping operation should be skipped.
	  *                           Set to true if the placeholder value sufficiently represents the would-be mapping result.
	  *                           Default = false = mapping will start immediately.
	  * @param map                A synchronous mapping function
	  * @param merge              A function that merges mapping results and previously acquired value into a new pointer value.
	  *                           Handles possible mapping function errors, also.
	  * @param exc                Implicit execution context
	  * @param log                Implicit logging implementation for encountered errors
	  * @tparam Origin     Original pointer value type
	  * @tparam Result     Mapping result type before merging
	  * @tparam Reflection Mapping result type after merging
	  * @return A new asynchronous mirror
	  */
	def merging[Origin, Result, Reflection](source: Changing[Origin], placeHolder: Reflection,
	                                      skipInitialProcess: Boolean = false)
	                                     (map: Origin => Result)
	                                     (merge: (Reflection, Result) => Reflection)
	                                     (implicit exc: ExecutionContext, log: Logger): Changing[AsyncMirrorValue[Origin, Reflection]] =
	{
		// Case: Mapping required => constructs a proper mirror
		if (source.isChanging || !skipInitialProcess)
			new AsyncProcessMirror[Origin, Result, Reflection](source, placeHolder, skipInitialProcess)(map)(merge)
		// Case: Fixed source and no initial mapping required => Constructs a fixed pointer
		else
			Fixed(AsyncMirrorValue(placeHolder))
	}
}

/**
  * This mirror (mapped view of a changed item) performs the mapping asynchronously, which
  * is useful when the mapping operation takes a while to complete and shouldn't freeze/block any of the active threads
  * while doing so.
  *
  * This is a rewritten version of the AsyncMirror class.
  * This version uses synchronous functions and a dedicated background process.
  *
  * @author Mikko Hilpinen
  * @since 23.9.2020, v1.9
  *
  * @param source Pointer that's being mapped
  * @param initialPlaceHolder A placeholder for the first map result.
  *                           Used until a real value has been resolved.
  * @param skipInitialProcess Whether the initial mapping operation should be skipped.
  *                           Set to true if the placeholder value sufficiently represents the would-be mapping result.
  *                           Default = false = mapping will start immediately.
  * @param f A synchronous mapping function
  * @param merge A function that merges mapping results and previously acquired value into a new pointer value.
  *              Handles possible mapping function errors, also.
  * @param exc Implicit execution context
  * @param log Implicit logging implementation for encountered errors
  *
  * @tparam Origin Original pointer value type
  * @tparam Result Mapping result type before merging
  * @tparam Reflection Mapping result type after merging
  */
class AsyncProcessMirror[Origin, Result, Reflection](val source: Changing[Origin], initialPlaceHolder: Reflection,
                                                     skipInitialProcess: Boolean = false)
                                                    (f: Origin => Result)
                                                    (merge: (Reflection, Result) => Reflection)
                                                    (implicit exc: ExecutionContext, log: Logger)
	extends ChangingWrapper[AsyncMirrorValue[Origin, Reflection]]
		with MayStopChanging[AsyncMirrorValue[Origin, Reflection]]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * Type of value stored in this mirror
	  */
	type Value = AsyncMirrorValue[Origin, Reflection]
	
	private val pointer = Volatile[Value](
		AsyncMirrorValue(initialPlaceHolder, if (skipInitialProcess) None else Some(source.value)))
	private val activeOriginPointer = pointer.map { _.activeOrigin }
	
	private var stopListeners = Vector[ChangingStoppedListener]()
	
	
	// INITIAL CODE ---------------------
	
	// Starts background calculation whenever the "active origin" is updated (unless the process is running already)
	activeOriginPointer.addContinuousListener { e =>
		if (e.newValue.isDefined)
			MappingProcess.runAsync()
	}
	
	// May immediately start to process the first value
	if (!skipInitialProcess)
		MappingProcess.runAsync()
	
	// Whenever source value changes, requests an asynchronous status update
	source.addListener(ChangeListener.continuous { event => requestCalculation(event.newValue) })
	
	// Once (if) the source pointer stops changing, discards listeners and informs the stop listeners
	// Waits for the async process completion first, however
	source.addChangingStoppedListener {
		once { _.isNotProcessing } { _ => declareChangingStopped() }
	}
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def wrapped = pointer
	
	override def isChanging = source.isChanging || value.isProcessing
	override def mayStopChanging = source.mayStopChanging
	
	override protected def declareChangingStopped(): Unit = {
		pointer.clearListeners()
		stopListeners.foreach { _.onChangingStopped() }
		stopListeners = Vector()
	}
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener) =
		stopListeners :+= listener
	
	
	// OTHER    -------------------------
	
	private def requestCalculation(newOrigin: Origin) = {
		pointer.update { current =>
			// Case: A new calculation has already been queued
			if (current.queuedOrigin.isDefined) {
				// Case: Current calculation solves this new item => skips the previously queued item
				if (current.activeOrigin.contains(newOrigin))
					current.withoutQueue
				// Case: New calculation is required => overwrites the queued calculation
				else
					current.copy(queuedOrigin = Some(newOrigin))
			}
			// Case: Current calculation handles same case => Doesn't modify
			else if (current.activeOrigin.contains(newOrigin))
				current
			// Case: Previous calculation is still going on and needs to be completed first => queues this calculation
			else if (current.isProcessing)
				current.copy(queuedOrigin = Some(newOrigin))
			// Case: Previous calculation has been completed => Starts a new one
			else
				current.copy(activeOrigin = Some(newOrigin))
		}
	}
	
	
	// NESTED   ----------------------------
	
	private object MappingProcess extends Process(shutdownReaction = Some(Cancel))
	{
		override protected def isRestartable: Boolean = true
		
		override protected def runOnce(): Unit = {
			// Expects active origin to be set when running
			activeOriginPointer.value.foreach { origin =>
				Try {
					// Performs the computation
					val result = f(origin)
					// Determines the new value by merging the result with the previous value
					pointer.update { current => AsyncMirrorValue(merge(current.current, result), current.queuedOrigin) }
				}.logFailure
			}
		}
	}
}
