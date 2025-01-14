package utopia.flow.view.mutable.async

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.util.logging.Logger
import utopia.flow.util.TryExtensions._
import utopia.flow.view.mutable.LoggingPointerFactory
import utopia.flow.view.mutable.eventful.LockablePointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

import scala.util.Try

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
	
	override def lock(): Unit = {
		lockFlag.set()
		declareChangingStopped()
	}
	
	override protected def declareChangingStopped(): Unit =
		changingStoppedListenersP.popAll().foreach { _.onChangingStopped() }
		
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit =
		changingStoppedListenersP.update { _ :+ listener }
	
	override protected def assignToUnlocked(newValue: A): Unit =
		super.assign(newValue).foreach { effect => Try { effect() }.log }
	
	override protected def assign(newValue: A): Seq[() => Unit] = {
		// Case: Locked => Won't allow further mutations
		if (lockFlag.isSet) {
			if (newValue == value)
				Empty
			else
				throw new IllegalStateException("This pointer has already been locked")
		}
		// Case: Not locked => Continues with the default behavior
		else
			super.assign(newValue)
	}
}