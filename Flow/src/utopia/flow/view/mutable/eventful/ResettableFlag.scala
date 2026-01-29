package utopia.flow.view.mutable.eventful

import utopia.flow.event.model.AfterEffect
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.mutable.Switch
import utopia.flow.view.template.eventful.{ChangingWrapper, Flag}

object ResettableFlag
{
	// COMPUTED ------------------------
	
	/**
	  * @return Access to resettable lockable flag constructors
	  */
	def lockable = ResettableLockableFlag
	
	
	// OTHER    ------------------------
	
	/**
	 * @param initialValue The initial value of this flag
	  * @return A new resettable flag
	  */
	def apply(initialValue: Boolean = false)(implicit log: Logger): ResettableFlag =
		new ResettableFlagWrapper(EventfulPointer(initialValue))
	
	/**
	 * Wraps a mutable boolean pointer
	 * @param pointer A pointer to wrap
	 * @return The specified pointer as a resettable flag
	 */
	def wrap(pointer: EventfulPointer[Boolean]): ResettableFlag = pointer match {
		case flag: ResettableFlag => flag
		case p => new ResettableFlagWrapper(p)
	}
	
	
	// NESTED   -----------------------
	
	private class ResettableFlagWrapper(override protected val wrapped: EventfulPointer[Boolean])
		extends ResettableFlag with ChangingWrapper[Boolean]
	{
		// ATTRIBUTES   ---------------------
		
		override lazy val view: Flag = new FlagView(this)
		
		
		// IMPLEMENTED  ---------------------
		
		override implicit def listenerLogger: Logger = wrapped.listenerLogger
		
		override def toString = s"$wrapped.flag"
		
		override def value_=(newValue: Boolean): Unit = wrapped.value = newValue
		override def setAndQueueEvent(newValue: Boolean): IterableOnce[AfterEffect] = wrapped.setAndQueueEvent(newValue)
	}
}

/**
  * A common trait for flags which may be reset after they've been set
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait ResettableFlag extends SettableFlag with Switch with EventfulPointer[Boolean]
{
	// COMPUTED --------------------------
	
	/**
	  * @return A future that resolves when this flag is reset the next time
	  */
	def nextResetFuture = findMapNextFuture { if (_) None else Some(()) }
}