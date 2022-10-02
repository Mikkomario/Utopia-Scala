package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.immutable.FlagView
import utopia.flow.event.{ChangeDependency, ChangeListener, Changing}

object ResettableFlag
{
	// OTHER    ------------------------
	
	/**
	  * @return A new resettable flag
	  */
	def apply(): ResettableFlag = new _ResettableFlag()
	
	
	// NESTED   -----------------------
	
	private class _ResettableFlag extends ResettableFlag with Changing[Boolean] with Settable[Boolean]
	{
		// ATTRIBUTES   -----------------------
		
		private var _value = false
		
		override var listeners = Vector[ChangeListener[Boolean]]()
		override var dependencies = Vector[ChangeDependency[Boolean]]()
		
		lazy val view = new FlagView(this)
		
		
		// IMPLEMENTED  -----------------------
		
		override def value = _value
		override def value_=(newValue: Boolean) = {
			if (newValue != value)
				_set(newValue)
		}
		
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
		
		// Only call this if newValue != value
		private def _set(newValue: Boolean) = {
			_value = newValue
			fireChangeEvent(!newValue)
		}
	}
}

/**
  * A common trait for flags which may be reset after they've been set
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait ResettableFlag extends Flag with Resettable with Settable[Boolean]
{
	override def value_=(newValue: Boolean) = if (newValue) set() else reset()
}