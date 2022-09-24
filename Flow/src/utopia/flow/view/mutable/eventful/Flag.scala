package utopia.flow.view.mutable.eventful

import utopia.flow.event.ChangeListener
import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.template.eventful.{Changing, FlagLike}

object Flag
{
	// OTHER    ------------------------
	
	/**
	  * @return A new flag
	  */
	def apply(): Flag = new _Flag()
	
	
	// NESTED   ------------------------
	
	/**
	  * An item that may be set (from false to true) exactly once.
	  * Generates change events when set.
	  * @author Mikko Hilpinen
	  * @since 18.9.2022, v1.17
	  */
	private class _Flag extends Flag with Changing[Boolean]
	{
		// ATTRIBUTES   ---------------------
		
		private var _value = false
		
		private var _listeners = Vector[ChangeListener[Boolean]]()
		private var _dependencies = Vector[ChangeDependency[Boolean]]()
		
		lazy val view = new FlagView(this)
		
		
		// IMPLEMENTED  ---------------------
		
		override def value = _value
		override def isChanging = isNotSet
		
		override def listeners = _listeners
		override def listeners_=(newListeners: Vector[ChangeListener[Boolean]]) = {
			if (isNotSet)
				_listeners = newListeners
		}
		
		override def dependencies = _dependencies
		override def dependencies_=(newDependencies: Vector[ChangeDependency[Boolean]]) = {
			if (isNotSet)
				_dependencies = newDependencies
		}
		
		override def set() = {
			if (isNotSet) {
				_value = true
				fireChangeEvent(false)
				// Forgets all the listeners at this point, because no more change events will be fired
				_listeners = Vector()
				_dependencies = Vector()
				true
			}
			else
				false
		}
	}
}

/**
  * A common trait for flags.
  * I.e. for boolean containers that may be set from false to true.
  */
trait Flag extends FlagLike
{
	/**
	  * @return An immutable view into this flag
	  */
	def view: FlagLike
	
	/**
	  * Sets this flag (from false to true), unless set already
	  * @return Whether the state of this flag was altered, i.e. whether this flag was not set previously.
	  */
	def set(): Boolean
}
