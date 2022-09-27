package utopia.flow.event.model

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.Property

/**
  * An event that is fired when (model) properties change or are otherwise modified.
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1
  * @tparam P Type of affected property
  */
sealed trait PropertyChangeEvent[+P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The property that changed or was part of a change
	  */
	def property: P
	/**
	  * @return The change in property value that occurred
	  */
	def valueChange: ChangeEvent[Value]
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return The previous property value
	  */
	def oldValue = valueChange.oldValue
	/**
	  * @return The new property value
	  */
	def newValue = valueChange.newValue
}

object PropertyChangeEvent
{
	// NESTED   -----------------------------
	
	/** A change event that is fired when a new property is added to a model
	  * @param property The property that was added
	  * @tparam P Type of the added property
	  */
	case class PropertyAdded[+P <: Property](property: P) extends PropertyChangeEvent[P]
	{
		override val valueChange = {
			val newVal = property.value
			ChangeEvent(Value.emptyWithType(newVal.dataType), newVal)
		}
	}
	/** A change event that is fired when a property is removed from a model
	  * @param property The property that was just removed
	  * @tparam P Type of affected property
	  */
	case class PropertyRemoved[+P <: Property](property: P) extends PropertyChangeEvent[P]
	{
		override def valueChange = {
			val oldVal = property.value
			ChangeEvent(oldVal, Value.emptyWithType(oldVal.dataType))
		}
	}
	/** A change event fired when one property is swapped for another
	  * @param propertyChange The change in properties that occurred
	  * @tparam P Type of swapped properties
	  */
	case class PropertySwapped[+P <: Property](propertyChange: ChangeEvent[P]) extends PropertyChangeEvent[P]
	{
		override val valueChange = propertyChange.map { _.value }
		
		def oldProperty = propertyChange.oldValue
		def newProperty = propertyChange.newValue
		
		override def property = newProperty
	}
	/** A change event fired when a property's value changes
	  * @param property The property that changed
	  * @param valueChange The change that occurred in that property's value
	  * @tparam P Type of the changed property
	  */
	case class PropertyValueChange[+P](property: P, valueChange: ChangeEvent[Value]) extends PropertyChangeEvent[P]
}
