package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.mutable.Switch
import utopia.flow.view.template.eventful.Flag.FlagWrapper
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper, Flag}

import scala.util.Try

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
	def apply(initialValue: Boolean = false)(implicit log: Logger): ResettableFlag = new _ResettableFlag(initialValue)
	
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
	
	private class _ResettableFlag(initialValue: Boolean = false)(implicit log: Logger)
		extends AbstractChanging[Boolean] with ResettableFlag
	{
		// ATTRIBUTES   -----------------------
		
		private var _value = initialValue
		override val destiny: Destiny = ForeverFlux
		
		override lazy val view = new FlagView(this)
		
		
		// IMPLEMENTED  -----------------------
		
		override def value = _value
		override def value_=(newValue: Boolean) = {
			val oldValue = _value
			_value = newValue
			fireEventIfNecessary(oldValue, newValue).foreach { effect => Try { effect() }.log }
		}
		
		override def toString = s"Flag(${ _value }).resettable"
		
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	}
	
	private class ResettableFlagWrapper(p: EventfulPointer[Boolean])
		extends ResettableFlag with ChangingWrapper[Boolean]
	{
		override lazy val view: Flag = new FlagView(this)
		
		override implicit def listenerLogger: Logger = p.listenerLogger
		override protected def wrapped: Changing[Boolean] = p
		
		override def value_=(newValue: Boolean): Unit = p.value = newValue
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