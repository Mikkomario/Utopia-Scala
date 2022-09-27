package utopia.flow.event.listener

import utopia.flow.event.model.PropertyChangeEvent

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
