package utopia.flow.view.mutable.eventful

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.mutable.{Pointer, Resettable}
import utopia.flow.view.template.eventful.AbstractChanging

import scala.util.Try

object ResettableFlag
{
	// OTHER    ------------------------
	
	/**
	 * @param initialValue The initial value of this flag
	  * @return A new resettable flag
	  */
	def apply(initialValue: Boolean = false)(implicit log: Logger): ResettableFlag = new _ResettableFlag(initialValue)
	
	
	// NESTED   -----------------------
	
	private class _ResettableFlag(initialValue: Boolean = false)(implicit log: Logger)
		extends AbstractChanging[Boolean] with ResettableFlag with Pointer[Boolean]
	{
		// ATTRIBUTES   -----------------------
		
		private var _value = initialValue
		
		override lazy val view = new FlagView(this)
		
		
		// IMPLEMENTED  -----------------------
		
		override def value = _value
		override def value_=(newValue: Boolean) = _set(newValue)
		
		override def set() = {
			if (isNotSet) {
				_set(true)
				true
			}
			else
				false
		}
		override def reset() = {
			if (isSet) {
				_set(false)
				true
			}
			else
				false
		}
		
		
		// OTHER    --------------------------
		
		private def _set(newValue: Boolean) = {
			val oldValue = _value
			_value = newValue
			fireEventIfNecessary(oldValue, newValue).foreach { effect => Try { effect() }.logFailure }
		}
	}
}

/**
  * A common trait for flags which may be reset after they've been set
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait ResettableFlag extends Flag with Resettable with EventfulPointer[Boolean]
{
	// COMPUTED --------------------------
	
	/**
	  * @return A future that resolves when this flag is reset the next time
	  */
	def nextResetFuture = findMapNextFuture { if (_) None else Some(()) }
	
	
	// IMPLEMENTED  ----------------------
	
	override def destiny: Destiny = ForeverFlux
	
	override def value_=(newValue: Boolean) = if (newValue) set() else reset()
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	
	
	// OTHER    --------------------------
	
	/**
	  * Reverses the current value of this flag
	  * @return The new value in this flag
	  */
	def switch() = updateAndGet { !_ }
}