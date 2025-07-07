package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{Changing, Flag, OptimizedChanging}

import scala.util.Try

object LogicalMirror
{
	/**
	  * @param source1 Primary source pointer to track
	  * @param source2 Secondary source pointer to track
	  * @param log Implicit logging implementation used during event-handling
	  * @return A new flag that contains true only while the two specified pointers contain true
	  */
	def and(source1: Flag, source2: Flag)(implicit log: Logger): Flag = and(Pair(source1, source2))
	/**
	  * @param sources The source pointers to track
	  * @param log Implicit logging implementation used during event-handling
	  * @return A new flag that contains true only while the two specified pointers contain true
	  */
	def and(sources: Pair[Flag])(implicit log: Logger) =
		_apply(sources, stopValue = false) { _ && _ }
	
	/**
	  * @param source1 Primary source pointer to track
	  * @param source2 Secondary source pointer to track
	  * @param log Implicit logging implementation used during event-handling
	  * @return A new flag that contains true while either of the two specified pointers contains true
	  */
	def or(source1: Flag, source2: Flag)(implicit log: Logger): Flag = or(Pair(source1, source2))
	/**
	  * @param sources The source pointers to track
	  * @param log Implicit logging implementation used during event-handling
	  * @return A new flag that contains true while either of the two specified pointers contains true
	  */
	def or(sources: Pair[Flag])(implicit log: Logger) =
		_apply(sources, stopValue = true) { _ || _ }
	
	private def _apply(sources: Pair[Flag], stopValue: Boolean)
	                  (operation: (Boolean, => Boolean) => Boolean)
	                  (implicit log: Logger): Flag =
		sources.first.fixedValue match {
			case Some(v1) => if (v1 == stopValue) Always(stopValue) else sources.second
			case None =>
				sources.second.fixedValue match {
					case Some(v2) => if (v2 == stopValue) Always(stopValue) else sources.first
					case None => new LogicalMirror(sources, stopValue, operation)
				}
		}
}

/**
  * Views the value of two pointers. Applies a logical operation to two boolean input values.
  * Works with AND and OR -operators.
  * @author Mikko Hilpinen
  * @since 03.04.2025, v2.6
  * @param sources Mirrored source pointers
  * @param stopValue Value, when encountered, causes the opposing value to be insignificant.
  *                  E.g. When one of the parts of AND is false, the other part doesn't matter.
  * @param operation An operation that combines two boolean values.
  *                  The second value is call-by-name.
  */
sealed class LogicalMirror(sources: Pair[Changing[Boolean]], stopValue: Boolean,
                           operation: (Boolean, => Boolean) => Boolean)
                          (override implicit val listenerLogger: Logger)
	extends OptimizedChanging[Boolean] with Flag
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * Set to Some(Tracker) while active listening is required
	  */
	private val trackerP = Pointer.empty[Tracker]
	/**
	  * A listener that tracks whether there are attached listeners. Activates or disables tracking as needed.
	  */
	private val activityListener = ChangeListener[Boolean] { e =>
		if (e.newValue)
			trackerP.setOneIfEmpty(new Tracker)
		else
			stopTracking()
			
		Continue
	}
	
	/**
	  * Set to Some once this mirror has stopped reflecting the source pointers
	  */
	private val finalValueP = Pointer.empty[Boolean]
	/**
	  * Listens to source stop events and finalizes the state of this mirror, if necessary.
	  */
	private lazy val stopListener = ChangingStoppedListener {
		// Case: Both sources stopped changing,
		//       or one of them became fixed so that the other didn't have any more significance.
		//       => Seals this mirror
		if (finalValueP.isEmpty && (sources.forall { _.isFixed } || sources.exists { _.fixedValue.contains(stopValue) })) {
			finalValueP.value = Some(value)
			hasListenersFlag.removeListener(activityListener)
			stopTracking()
			declareChangingStopped()
		}
	}
	
	
	// INITIAL CODE -------------------
	
	// Starts listening to tracking requirements
	hasListenersFlag.addListener(activityListener)
	// Registers to listen on source stop events
	sources.foreach { _.addChangingStoppedListener(stopListener) }
		
	
	// IMPLEMENTED  -------------------
	
	// If finalized, returns the cached value
	override def value: Boolean = finalValueP.value.getOrElse {
		trackerP.value match {
			// Case: Actively tracking => Returns the tracker's value
			case Some(tracker) => tracker.value
			// Case: Not tracking => Calculates the value on every call (won't cache)
			case None => operation(sources.first.value, sources.second.value)
		}
	}
	override def destiny: Destiny = {
		// Case: This mirror has been sealed
		if (finalValueP.isDefined)
			Sealed
		else
			sources.oppositeToWhere { _.isFixed } match {
				// Case: One of the pointers has sealed => Seals if the other pointer seals as well
				case Some(onlyChanging) => onlyChanging.destiny
				// Case: Neither pointer has sealed => Possible to seal if either can seal (to the stop value)
				case None => Destiny.maySealIf(sources.exists { _.destiny.isPossibleToSeal })
			}
	}
	
	override def readOnly = this
	
	override def toString = fixedValue match {
		case Some(value) => s"Logically.always($value)"
		case None => s"Mirroring.logically(${ sources.first }).and(${ sources.second })"
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * Stops actively tracking the source states.
	  * Should be called when the last listener is removed.
	  */
	private def stopTracking() = trackerP.pop().foreach { _.detach() }
	
	
	// NESTED   ----------------------------
	
	private class Tracker
	{
		// ATTRIBUTES   --------------------
		
		/**
		  * Caches the computed output value
		  */
		private var _value = operation(sources.first.value, sources.second.value)
		
		/**
		  * Reacts to the secondary pointer, recalculating the output value
		  */
		private lazy val listener2 = ChangeListener[Boolean] { e => value = e.newValue }
		/**
		  * Reacts to the primary pointer, starting or stopping listening on the secondary pointer
		  */
		private val listener1 = ChangeListener[Boolean] { e =>
			val source2 = sources.second
			// Case: This pointer acquired a value that renders the other pointer insignificant
			//       => Stops listening to the other pointer and sets the result value immediately
			if (e.newValue == stopValue) {
				source2.removeListener(listener2)
				value = stopValue
			}
			// Case: This pointer acquired a value that makes the other pointer significant (again)
			//       => Attaches the secondary pointer's listener in order to update the state
			else {
				source2.addListener(listener2)
				value = source2.value
			}
		}
		
		
		// INITIAL CODE -------------------
		
		// Listens to the first pointer as long as this tracker is used
		sources.first.addListenerAndSimulateEvent(stopValue)(listener1)
		
		
		// COMPUTED -----------------------
		
		def value = _value
		private def value_=(newValue: Boolean) = {
			// Fires change events, if necessary
			if (newValue != _value) {
				_value = newValue
				fireEvent(ChangeEvent(!newValue, newValue)).foreach { effect => Try { effect() }.log }
			}
		}
		
		
		// OTHER    -----------------------
		
		/**
		  * Detaches this tracker from the source pointers.
		  * After this method call, this tracker should no longer be relied on.
		  */
		def detach() = {
			sources.first.removeListener(listener1)
			sources.second.removeListener(listener2)
		}
	}
}
