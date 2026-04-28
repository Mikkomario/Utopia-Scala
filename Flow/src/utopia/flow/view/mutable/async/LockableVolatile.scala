package utopia.flow.view.mutable.async

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.AfterEffect
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.LoggingPointerFactory
import utopia.flow.view.mutable.eventful.LockablePointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

object LockableVolatile extends LoggingPointerFactory[LockableVolatile]
{
	// IMPLEMENTED  -------------------------
	
	override def apply[A](initialValue: A)(implicit log: Logger): LockableVolatile[A] =
		new _LockableVolatile[A](initialValue)
	
	
	// NESTED   -----------------------------
	
	private class _LockableVolatile[A](initialValue: A)(implicit listenerLogger: Logger) extends LockableVolatile[A]
	{
		// ATTRIBUTES   -----------
		
		@volatile private var _value: A = initialValue
		
		override lazy val readOnly: Changing[A] = ChangingWrapper(this)
		
		
		// IMPLEMENTED  ----------
		
		override def value: A = _value
		
		override def toString = if (locked) s"Volatile($value).locked" else s"Volatile($value).lockable"
		
		override protected def assignWithoutEvents(newValue: A): Unit = _value = newValue
	}
}

/**
 * A mutable pointer that supports locking and is safe to use in multithreaded environments.
 * @author Mikko Hilpinen
 * @since 13.01.2025, v2.5.1
 */
abstract class LockableVolatile[A](implicit listenerLogger: Logger)
	extends EventfulVolatile[A] with LockablePointer[A]
{
	// ATTRIBUTES   -------------------
	
	private val lockFlag = VolatileSwitch()
	private val changingStoppedListenersP = Volatile.emptySeq[ChangingStoppedListener]
	
	
	// IMPLEMENTED  -------------------
	
	override def locked: Boolean = lockFlag.value
	
	override def lock(): Unit = lockFlag.synchronized {
		if (lockFlag.set())
			declareChangingStopped()
	}
	override def restrictLockingWhile[B](f: => B): B = lockFlag.synchronized(f)
	
	override protected def declareChangingStopped(): Unit =
		changingStoppedListenersP.popAll().foreach { _.onChangingStopped() }
		
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit =
		changingStoppedListenersP.update { _ :+ listener }
	
	override protected def assignToUnlocked(newValue: A): Unit =
		viewLocked { super.assign(_, newValue) }.foreach { _() }
	
	override def setAndQueueEvent(newValue: A): IterableOnce[AfterEffect] =
		failIfLocked { super.setAndQueueEvent(newValue) }
	override protected def setUnlockedAndQueueEvent(newValue: A): IterableOnce[AfterEffect] =
		super.setAndQueueEvent(newValue)
	
	override protected def assign(oldValue: A, newValue: A): Seq[() => Unit] = {
		// Case: Locked => Won't allow further mutations
		if (lockFlag.isSet) {
			if (newValue == oldValue)
				Empty
			else
				throw new IllegalStateException("This pointer has already been locked")
		}
		// Case: Not locked => Continues with the default behavior
		else
			super.assign(oldValue, newValue)
	}
}