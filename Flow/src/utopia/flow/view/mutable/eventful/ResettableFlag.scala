package utopia.flow.view.mutable.eventful

import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.mutable.{Resettable, Pointer}
import utopia.flow.view.template.eventful.AbstractChanging

object ResettableFlag
{
	// OTHER    ------------------------
	
	/**
	 * @param initialValue The initial value of this flag
	  * @return A new resettable flag
	  */
	def apply(initialValue: Boolean = false): ResettableFlag = new _ResettableFlag()
	
	
	// NESTED   -----------------------
	
	private class _ResettableFlag(initialValue: Boolean = false)
		extends AbstractChanging[Boolean] with ResettableFlag with Pointer[Boolean]
	{
		// ATTRIBUTES   -----------------------
		
		private var _value = initialValue
		
		lazy val view = new FlagView(this)
		
		
		// IMPLEMENTED  -----------------------
		
		override def value = _value
		override def value_=(newValue: Boolean) = _set(newValue)
		
		override def isChanging = true
		
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
			fireEventIfNecessary(oldValue, newValue).foreach { _() }
		}
	}
}

/**
  * A common trait for flags which may be reset after they've been set
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait ResettableFlag extends Flag with Resettable with Pointer[Boolean]
{
	// COMPUTED --------------------------
	
	/**
	  * @return A future that resolves when this flag is reset the next time
	  */
	def nextResetFuture = findMapNextFuture { if (_) None else Some(()) }
	
	
	// IMPLEMENTED  ----------------------
	
	override def value_=(newValue: Boolean) = if (newValue) set() else reset()
}