package utopia.flow.event.listener

import utopia.flow.event.model.PropertyChangeEvent

import scala.language.implicitConversions

object PropertyChangeListener
{
	// IMPLICIT ------------------------
	
	/**
	  * Converts a function into a property change listener
	  * @param f Function to convert
	  * @tparam P Type of changed / listened properties
	  * @return A property change listener wrapping the specified function
	  */
	implicit def apply[P](f: PropertyChangeEvent[P] => Unit): PropertyChangeListener[P] =
		new _PropertyChangeListener[P](f)
	
	
	// NESTED   ------------------------
	
	private class _PropertyChangeListener[P](f: PropertyChangeEvent[P] => Unit) extends PropertyChangeListener[P]
	{
		override def onPropertyChange(event: PropertyChangeEvent[P]): Unit = f(event)
	}
}

/**
  * These listeners are interested in property change events
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1
  * @tparam P Type of changed / listened properties
  */
trait PropertyChangeListener[-P]
{
	/**
	  * This method is called when a model's property is changed
	  * @param event The change event
	  */
	def onPropertyChange(event: PropertyChangeEvent[P]): Unit
}
