package utopia.flow.async

import utopia.flow.event.{ChangeDependency, ChangeListener, Changing, ChangingLike}
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object AsyncMirror
{
	/**
	  * Creates a new asynchronous mirror
	  * @param source A source pointer
	  * @param placeHolder Value initially placed in this pointer
	  * @param synchronousMap A mapping function that returns a try
	  * @param exc Implicit execution context
	  * @param logger Implicit logger to record encountered errors with
	  * @tparam Origin Original pointer value type
	  * @tparam Reflection Successful mapping result type
	  * @return A new asynchronously mirroring pointer
	  */
	def trying[Origin, Reflection](source: ChangingLike[Origin], placeHolder: Reflection)
	                                (synchronousMap: Origin => Try[Reflection])
	                                (implicit exc: ExecutionContext, logger: Logger) =
	{
		if (source.isChanging)
			new AsyncMirror[Origin, Try[Reflection], Reflection](source, placeHolder)(synchronousMap)({ (previous, result) =>
				result.flatten match {
					case Success(value) => value
					case Failure(error) =>
						logger(error)
						previous
				}
			})
		else
			new ChangeFuture[Reflection](placeHolder, Future { synchronousMap(source.value).getOrElse(placeHolder) })
	}
	
	/**
	  * Creates a new asynchronous mirror
	  * @param source A source pointer
	  * @param placeHolder Value initially placed in this pointer
	  * @param synchronousMap A mapping function that is expected to throw exceptions once in a while
	  * @param exc Implicit execution context
	  * @param logger Implicit logger to record encountered errors with
	  * @tparam Origin Original pointer value type
	  * @tparam Reflection Successful mapping result type
	  * @return A new asynchronously mirroring pointer
	  */
	def catching[Origin, Reflection](source: ChangingLike[Origin], placeHolder: Reflection)
	                                (synchronousMap: Origin => Reflection)
	                                (implicit exc: ExecutionContext, logger: Logger) =
	{
		if (source.isChanging)
			new AsyncMirror[Origin, Reflection, Reflection](source, placeHolder)(synchronousMap)({ (previous, result) =>
				result match
				{
					case Success(value) => value
					case Failure(error) =>
						logger(error)
						previous
				}
			})
		else
			new ChangeFuture[Reflection](placeHolder, Future { synchronousMap(source.value) })
	}
	
	/**
	  * Creates a new asynchronous mirror
	  * @param source A source pointer
	  * @param placeHolder Value initially placed in this pointer
	  * @param synchronousMap A mapping function that is expected to always succeed (if/when it throws,
	  *                       errors are only printed but otherwise ignored)
	  * @param exc Implicit execution context
	  * @param logger Implicit logger to record encountered errors with
	  * @tparam Origin Original pointer value type
	  * @tparam Reflection Successful mapping result type
	  * @return A new asynchronously mirroring pointer
	  */
	def apply[Origin, Reflection](source: ChangingLike[Origin], placeHolder: Reflection)
	                             (synchronousMap: Origin => Reflection)
	                             (implicit exc: ExecutionContext, logger: Logger) =
		catching[Origin, Reflection](source, placeHolder)(synchronousMap)
}

/**
  * This mirror (mapped view of a changed item) performs the mapping asynchronously and possibly with a delay, which
  * is useful when the mapping operation takes a while to complete and shouldn't freeze/block any of the active threads
  * while doing so.
  * @author Mikko Hilpinen
  * @since 23.9.2020, v1.9
  * @param source Pointer that's being mapped
  * @param initialPlaceHolder First value in this pointer. Used until a real value arrives,
  *                           which is immediately calculated asynchronously
  * @param synchronousMap A synchronous mapping operation which may take a while to complete
  * @param merge A function that merges mapping results and previously acquired value into a pointer value. Handles
  *              possible mapping function errors in the process.
  * @param exc Implicit execution context
  * @tparam Origin Original pointer value type
  * @tparam Result Mapping result type before merging
  * @tparam Reflection Mapping result type after merging
  */
class AsyncMirror[Origin, Result, Reflection](val source: ChangingLike[Origin], initialPlaceHolder: Reflection)
                                             (synchronousMap: Origin => Result)
                                             (merge: (Reflection, Try[Result]) => Reflection)
                                             (implicit exc: ExecutionContext)
	extends Changing[Reflection]
{
	// ATTRIBUTES   ------------------------
	
	override var listeners = Vector[ChangeListener[Reflection]]()
	override var dependencies = Vector[ChangeDependency[Reflection]]()
	
	// Initial value may be calculated synchronously in order to always have some value available
	// (asynchronous calculation is used if placeholder value is provided)
	private val cachedValuePointer = Volatile(initialPlaceHolder)
	// Contains: (value to map, mapping results (if arrived), queued source value (if required))
	private val currentCalculation = Volatile[(Origin, Option[Reflection], Option[Origin])](
		(source.value, Some(cachedValuePointer.value), None))
	
	/**
	  * A pointer that notes whether this mirror is currently performing an asynchronous calculation
	  */
	lazy val isProcessingPointer = currentCalculation.map { _._2.isEmpty }
	
	
	// INITIAL CODE ---------------------
	
	// If a placeholder value was provided, immediately starts a new calculation
	currentCalculation.value = calculateNextValue(source.value)
	
	// Whenever source value changes, requests an asynchronous status update
	source.addListener { event => requestCalculation(event.newValue) }
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Whether this mirror is currently processing a change
	  */
	def isProcessing = isProcessingPointer.value
	
	
	// IMPLEMENTED  ---------------------
	
	override def isChanging = source.isChanging || isProcessing
	
	override def value = cachedValuePointer.value
	
	
	// OTHER    -------------------------
	
	private def requestCalculation(newOrigin: Origin) =
	{
		currentCalculation.update { case (oldOrigin, oldResult, queued) =>
			// Checks whether currently active (or performed) calculation already handles this case
			val isSameOrigin = oldOrigin == newOrigin
			// Case: A new calculation has already been queued
			if (queued.isDefined)
			{
				// May skip it if current calculation already solves this one
				if (isSameOrigin)
					(newOrigin, oldResult, None)
				// Or simply overwrites the queued calculation
				else
					(oldOrigin, oldResult, Some(newOrigin))
			}
			// Case: Current calculation handles same case
			else if (isSameOrigin)
				(newOrigin, oldResult, None)
			// Case: Previous calculation has been completed and a new one should be started
			else if (oldResult.isDefined)
				calculateNextValue(newOrigin)
			// Case: Previous calculation is still going on and needs to be completed first
			else
				(oldOrigin, None, Some(newOrigin))
		}
	}
	
	private def resultsArrived(origin: Origin, result: Try[Result]): Unit =
	{
		// Updates currently cached value, merging results into it
		val (oldValue, newValue) = cachedValuePointer.pop { oldValue =>
			val newValue = merge(oldValue, result)
			(oldValue, newValue) -> newValue
		}
		// Fires a change event
		fireChangeEvent(oldValue)
		
		// cachedValuePointer.set(origin -> result)
		// Updates calculation status, may start a new calculation immediately
		currentCalculation.update { case (_, _, queued) =>
			queued match
			{
				// Case: New calculation was already requested
				case Some(nextOrigin) => calculateNextValue(nextOrigin)
				// Case: No new calculation was requested yet
				case None => (origin, Some(newValue), None)
			}
		}
	}
	
	private def calculateNextValue(origin: Origin): (Origin, Option[Reflection], Option[Origin]) =
	{
		// Starts the new calculation
		val calculation = Future { synchronousMap(origin) }
		// Once that calculation is ready, updates results and may start a new one
		calculation.onComplete { resultsArrived(origin, _) }
		(origin, None, None)
	}
}
