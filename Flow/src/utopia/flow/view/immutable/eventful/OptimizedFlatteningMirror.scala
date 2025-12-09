package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.collection.template.MapAccess
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}
import utopia.flow.operator.Identity
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.{Changing, Flag, OptimizedChanging}

import scala.annotation.unchecked.uncheckedVariance
import scala.util.Try

object OptimizedFlatteningMirror
{
	/**
	  * @param source The source pointer to map
	  * @param condition A condition that must be met for mapping to occur (default = AlwaysTrue = no condition)
	  * @param f Applied mapping function. Yields changing items.
	  * @tparam O Type of original pointer's values
	  * @tparam R Type of mapping result pointers' values
	  * @return A pointer that contains the latest value of the latest map-result pointer
	  */
	def apply[O, R](source: Changing[O], condition: => Flag = AlwaysTrue)(f: O => Changing[R]) =
		new OptimizedFlatteningMirror[O, R](source, f, condition = condition)
	
	/**
	  * Creates a new flattening mirror based on incremental mapping
	  * @param source The source pointer to map
	  * @param condition A condition that must be met for mapping to occur (default = AlwaysTrue = no condition)
	  * @param initialMap Mapping function applied initially. Yields changing items.
	  * @param incrementalMap Mapping function applied for remaining 'source' changes.
	  *                       Accepts:
	  *                             1. Previous mapping-result pointer
	  *                             1. Change event that occurred in 'source'
	  *
	  *                       Yields a new mapping-result pointer
	  * @tparam O Type of original pointer's values
	  * @tparam R Type of mapping result pointers' values
	  * @return A pointer that contains the latest value of the latest map-result pointer
	  */
	def incremental[O, R](source: Changing[O], condition: => Flag = AlwaysTrue)(initialMap: O => Changing[R])
	                     (incrementalMap: (Changing[R], ChangeEvent[O]) => Changing[R]) =
		new OptimizedFlatteningMirror[O, R](source, initialMap, Some(incrementalMap), condition)
	
	/**
	  * Creates a flattened view into the specified pointer
	  * @param source A pointer that contains pointers
	  * @tparam A Type of values in the specified pointer's pointers
	  * @return A pointer that contains the latest value of the current 'source' value
	  */
	def flatten[A](source: Changing[Changing[A]]) =
		apply[Changing[A], A](source)(Identity)
}
/**
  * A mirror (i.e. a mapping view to a changing item) that "flattens" result,
  * i.e. handles cases where the mapping function returns changing items.
  *
  * This implementation is optimized to only actively listen the 'source' pointer while this pointer has listeners
  * assigned to it.
  *
  * @author Mikko Hilpinen
  * @since 22.9.2022, v1.17
  */
class OptimizedFlatteningMirror[+O, R](source: Changing[O], directMap: O => Changing[R],
                                       incrementalMap: Option[(Changing[R], ChangeEvent[O]) => Changing[R]] = None,
                                       condition: => Flag = AlwaysTrue)
	extends OptimizedChanging[R]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A pointer that contains the currently tracked mid-pointer
	  */
	private val pointerPointer = incrementalMap match {
		case Some(incrementalMap) => source.viewWhile(condition).incrementalMap(directMap)(incrementalMap)
		case None =>
			// Caches the latest mapping result
			// in order to avoid forming two different pointers of the same origin value (in certain edge cases)
			val mapResultCache: MapAccess[O @uncheckedVariance, Changing[R]] = Cache.onlyLatest(directMap)
			source.mapWhile(condition)(mapResultCache.apply)
	}
	/**
	  * A pointer that contains the currently managed active (mid-pointer) tracker.
	  * Contains None while this pointer has no listeners (and needs no active tracking, therefore)
	  */
	private val activeTrackerP = Pointer.empty[ActiveTracker]
	
	
	// INITIAL CODE -----------------------
	
	// Turns active tracking on or off as necessary
	hasListenersFlag.addHighPriorityListener { event =>
		// Case: Active tracking is required => Starts it
		if (event.newValue)
			activeTrackerP.setOneIfEmpty(new ActiveTracker(currentMidPointer))
		// Case: Active tracking is no longer required => Terminates it
		else
			stopActiveTracking()
			
		Continue
	}
	
	// Handles source pointer stopped -case
	pointerPointer.onceChangingStops {
		// Makes sure the stop-listeners are informed when/if the final mid-pointer stops changing
		currentMidPointer.addChangingStoppedListenerAndSimulateEvent { declareChangingStopped() }
	}
	
	
	// COMPUTED ---------------------------
	
	private def currentMidPointer = pointerPointer.value
	
	
	// IMPLEMENTED  -----------------------
	
	override implicit def listenerLogger: Logger = source.listenerLogger
	
	override def value: R = activeTrackerP.value match {
		case Some(tracker) => tracker.value
		case None => currentMidPointer.value
	}
	// As long as the source pointer is changing, won't know whether the final result will be forever flux or sealed
	// Except that, if the source never stops changing, this pointer never stops changing either
	override def destiny = pointerPointer.destiny match {
		case Sealed => currentMidPointer.destiny
		case sd => sd
	}
	
	override def readOnly = this
	
	override def toString = fixedValue match {
		case Some(value) => s"Reflecting.always($value)"
		case None =>
			pointerPointer.fixedValue match {
				case Some(p) => p.toString
				case None =>
					val suffix = if (condition.isAlwaysTrue) "" else s".while($condition)"
					s"Mirroring.flattening($source)$suffix"
			}
	}
	
	override def lockWhile[B](operation: => B): B = source.lockWhile(operation)
	
	
	// OTHER    -----------------------------
	
	private def stopActiveTracking() = activeTrackerP.pop().foreach { _.deactivate() }
	
	
	// NESTED   -----------------------------
	
	private class ActiveTracker(initialMidPointer: Changing[R]) extends View[R]
	{
		// ATTRIBUTES   -----------------------
		
		/**
		  * A pointer that contains the latest value of the current mid-pointer
		  */
		private val valueP = Pointer(initialMidPointer.value)
		
		/**
		  * A listener that listens to the active mid-pointer and updates the simulated value based on its changes
		  * (also informing the change listeners)
		  */
		private val valueUpdatorP = Volatile.optional(ValueUpdator.startIfNecessary(initialMidPointer))
		/**
		  * A listener that listens to the pointer-pointer and moves the aforementioned listener between mid-pointers
		  */
		private lazy val moveValueUpdatorListener = ChangeListener[Changing[R]] { event =>
			// Starts tracking the new pointer and stops tracking the old
			val newMidPointer = event.newValue
			valueUpdatorP.update { oldUpdator =>
				oldUpdator.foreach { _.terminate() }
				ValueUpdator.startIfNecessary(newMidPointer)
			}
			
			// Updates the simulated value immediately and fires the change events afterwards
			assign(newMidPointer.value)
		}
		
		
		// INITIAL CODE -----------------------
		
		// Starts tracking changes in the pointer-pointer
		pointerPointer.addListenerAndSimulateEvent(initialMidPointer, isHighPriority = true)(moveValueUpdatorListener)
		
		
		// IMPLEMENTED  -----------------------
		
		override def value: R = valueP.value
		
		
		// OTHER    ---------------------------
		
		/**
		  * Deactivates this tracker, so that it no longer follows any pointers.
		  * This tracker should not be used afterwards (because it's value will not be up-to-date).
		  */
		def deactivate() = {
			// Stops tracking the pointer-pointer
			pointerPointer.removeListener(moveValueUpdatorListener)
			
			// Stops tracking the latest mid-pointer
			valueUpdatorP.pop().foreach { _.terminate() }
		}
		
		/**
		  * Assigns a new value and generates the appropriate change events (as after-effects)
		  * @param newValue New value to assign
		  * @return Change response to provide for the pointer that specified this value
		  */
		private def assign(newValue: R) = {
			val oldValue = valueP.getAndSet(newValue)
			val afterEffects = fireEventIfNecessary(oldValue, newValue)
			if (afterEffects.isEmpty)
				Continue
			else
				Continue.and { afterEffects.foreach { effect => Try { effect() }.log } }
		}
		
		
		// NESTED   ----------------------------
		
		private object ValueUpdator
		{
			def startIfNecessary(pointerToTrack: Changing[R]) = {
				if (pointerToTrack.mayChange)
					Some(new ValueUpdator(pointerToTrack))
				else
					None
			}
		}
		/**
		  * A listener that follows a single mid-pointer and performs the appropriate value updates
		  * @param trackedPointer Pointer being listened to
		  */
		private class ValueUpdator(trackedPointer: Changing[R]) extends ChangeListener[R]
		{
			// INITIAL CODE -------------------
			
			// Starts tracking the pointer
			trackedPointer.addHighPriorityListener(this)
			
			
			// IMPLEMENTED  -------------------
			
			override def onChangeEvent(event: ChangeEvent[R]): ChangeResponse = {
				if (currentMidPointer == trackedPointer)
					assign(event.newValue)
				else
					Detach
			}
			
			
			// OTHER    -----------------------
			
			def terminate() = trackedPointer.removeListener(this)
		}
	}
}
