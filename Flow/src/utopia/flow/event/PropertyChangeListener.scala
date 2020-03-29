package utopia.flow.event

/**
  * These listeners are interested in property changed -events
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1+
  */
trait PropertyChangeListener
{
	/**
	  * This method is called when a model's property is changed
	  * @param event The change event
	  */
	def onPropertyChanged(event: PropertyChangeEvent): Unit
}
