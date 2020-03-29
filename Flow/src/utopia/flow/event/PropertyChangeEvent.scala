package utopia.flow.event

import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.template.Property

object PropertyChangeEvent
{
	/**
	  * Creates a new event from property addition
	  * @param newProperty The newly added property
	  * @return A change event for the specified property
	  */
	def propertyAdded(newProperty: Property) = PropertyChangeEvent(newProperty.name,
		Value.emptyWithType(newProperty.dataType), newProperty.value)
	
	/**
	  * Creates a new event from property deletion
	  * @param property The deleted property
	  * @return A change event for the specified property
	  */
	def propertyRemoved(property: Property) = PropertyChangeEvent(property.name, property.value, Value.emptyWithType(property.dataType))
}

/**
  * These events are generated when a model's property changes
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1+
  * @param propertyName The name of the changed property
  * @param oldValue The old value for the property
  * @param newValue The new value for the property
  */
case class PropertyChangeEvent(propertyName: String, oldValue: Value, newValue: Value)
