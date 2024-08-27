package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{ContinueAnd, DetachAnd}
import utopia.flow.event.model.Destiny.Sealed
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.operator.enumeration.End.First
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.{Changing, ChangingView, OptimizedChanging}

object LockableBridge
{
	/**
	  * Creates a new lockable bridge
	  * @param origin The pointer viewed through this bridge
	  * @tparam A Type of values viewed through this bridge
	  * @return A new bridge that may be locked
	  */
	def apply[A](origin: Changing[A]) = origin match {
		case lb: LockableBridge[A] => lb
		case o => new LockableBridge[A](o)
	}
}

/**
  * A lockable view into the value of another changing item.
  * After this view has been locked, it won't reflect the changes in the original item anymore.
  * @author Mikko Hilpinen
  * @since 21.11.2023, v2.3
  */
class LockableBridge[A](origin: Changing[A]) extends OptimizedChanging[A] with Lockable[A]
{
	// ATTRIBUTES   --------------------------
	
	private var lockedValue: Option[A] = None
	private val relayEventsListener = ChangeListener { e: ChangeEvent[A] =>
		val afterEffects = fireEvent(e)
		if (locked) DetachAnd(afterEffects) else ContinueAnd(afterEffects)
	}
	
	/**
	  * An immutable view into this pointer
	  */
	override lazy val readOnly: Changing[A] = new ChangingView[A](this)
	
	
	// INITIAL CODE --------------------------
	
	origin.addListenerWhile(hasListenersFlag, priority = First)(relayEventsListener)
	stopOnceSourceStops(origin)
	
	
	// COMPUTED -----------------------------
	
	@deprecated("Please use .readOnly instead", "v2.5")
	def view = readOnly
	
	
	// IMPLEMENTED  --------------------------
	
	override implicit def listenerLogger: Logger = origin.listenerLogger
	
	override def value: A = lockedValue.getOrElse(origin.value)
	override def destiny: Destiny = if (locked) Sealed else origin.destiny.possibleToSeal
	override def locked: Boolean = lockedValue.isDefined
	
	override def lock(): Unit = {
		if (!locked) {
			lockedValue = Some(origin.value)
			if (origin.mayChange)
				declareChangingStopped()
		}
	}
	
	override protected def declareChangingStopped() = {
		super.declareChangingStopped()
		origin.removeListener(relayEventsListener)
	}
}
