package utopia.flow.test.generic

import utopia.flow.event.listener.PropertyChangeListener
import utopia.flow.event.model.PropertyChangeEvent
import utopia.flow.event.model.PropertyChangeEvent.{PropertyAdded, PropertyRemoved, PropertySwapped, PropertyValueChange}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.{MutableModel, Variable}
import utopia.flow.test.TestContext._

/**
 * Tests functions introduced in MutableModel, focusing on property change events.
 * @author Mikko Hilpinen
 * @since 17.11.2026, v2.9
 */
object MutableModelTest extends App
{
	// update
	{
		val m = MutableModel()
		// Test m("<key>") = <value> function
		m("a") = 1
		assert(m.properties.size == 1)
		assert(m("a").getInt == 1)
		assert(m.contains("a"))
		
		// Updating existing property
		m("a") = 2
		assert(m.properties.size == 1)
		assert(m("a").getInt == 2)
		
		// Case-insensitivity
		m("A") = 3
		assert(m.properties.size == 1)
		assert(m("a").getInt == 3)
		assert(m("A").getInt == 3)
		
		// Multiple properties
		m("b") = "Hello"
		assert(m.properties.size == 2)
		assert(m("b").getString == "Hello")
	}
	
	// + and -
	{
		val m = MutableModel()
		// Test m += Variable case
		val v1 = Variable("a", 1)
		m += v1
		assert(m.properties.size == 1)
		assert(m("a").getInt == 1)
		assert(m.existingProperty("a").contains(v1))
		
		val v2 = Variable("a", 2)
		m += v2
		assert(m.properties.size == 1)
		assert(m("a").getInt == 2)
		assert(m.existingProperty("a").contains(v2))
		
		val v3 = Variable("b", "test")
		m += v3
		assert(m.properties.size == 2)
		assert(m("b").getString == "test")
		
		// Test m -= Variable and m -= "key" cases
		m -= v3
		assert(m.properties.size == 1)
		assert(!m.contains("b"))
		assert(m.existingProperty("b").isEmpty)
		
		m -= "a"
		assert(m.properties.isEmpty)
		assert(!m.contains("a"))
		assert(m.existingProperty("a").isEmpty)
		
		// Case-insensitive removal by key
		val v4 = Variable("c", 4)
		m += v4
		assert(m.contains("c"))
		m -= "C"
		assert(m.properties.isEmpty)
		assert(!m.contains("c"))
	}
	
	// Listeners
	{
		val m = MutableModel()
		var latestEvent: Option[PropertyChangeEvent[Variable]] = None
		val listener = PropertyChangeListener[Variable] { e => latestEvent = Some(e) }
		m.addListener(listener)
		
		// Make sure the listener is correctly notified of changes in:
		// 1. m("<key>") = <value> case
		// 1.1 Property addition via update
		m("a") = 1
		assert(latestEvent.contains(PropertyAdded(m.property("a"))))
		assert(latestEvent.get.property.name == "a")
		assert(latestEvent.get.newValue.getInt == 1)
		
		// 1.2 Property value update via update
		m("a") = 2
		latestEvent match {
			case Some(PropertyValueChange(prop, valueChange)) =>
				assert(prop.name == "a")
				assert(valueChange.oldValue.getInt == 1)
				assert(valueChange.newValue.getInt == 2)
			case _ => assert(false, s"Expected PropertyValueChange, but got: $latestEvent")
		}
		
		// 1.3 Case-insensitive update
		m("A") = 3
		latestEvent match {
			case Some(PropertyValueChange(prop, valueChange)) =>
				assert(prop.name == "a")
				assert(valueChange.oldValue.getInt == 2)
				assert(valueChange.newValue.getInt == 3)
			case _ => assert(false, s"Expected PropertyValueChange, but got: $latestEvent")
		}
		
		// 1.4 Direct variable mutation
		m.property("a").value = 4
		latestEvent match {
			case Some(PropertyValueChange(prop, valueChange)) =>
				assert(prop.name == "a")
				assert(valueChange.oldValue.getInt == 3)
				assert(valueChange.newValue.getInt == 4)
			case _ => assert(false, s"Expected PropertyValueChange, but got: $latestEvent")
		}
		
		// 2. m += Variable case
		// 2.1 Adding a new variable
		val v1 = Variable("b", "initial")
		m += v1
		assert(latestEvent.contains(PropertyAdded(v1)))
		assert(latestEvent.get.newValue.getString == "initial")
		
		// 2.2 Mutating added variable
		v1.value = "updated"
		latestEvent match {
			case Some(PropertyValueChange(prop, valueChange)) =>
				assert(prop == v1)
				assert(valueChange.oldValue.getString == "initial")
				assert(valueChange.newValue.getString == "updated")
			case _ => assert(false, s"Expected PropertyValueChange, but got: $latestEvent")
		}
		
		// 2.3 Replacing existing variable with new variable (swap)
		val v2 = Variable("b", "replaced")
		m += v2
		latestEvent match {
			case Some(PropertySwapped(propChange)) =>
				assert(propChange.oldValue == v1)
				assert(propChange.newValue == v2)
				assert(latestEvent.get.oldValue.getString == "updated")
				assert(latestEvent.get.newValue.getString == "replaced")
			case _ => assert(false, s"Expected PropertySwapped, but got: $latestEvent")
		}
		
		// 2.4 Mutating swapped-in variable
		v2.value = "v2_updated"
		latestEvent match {
			case Some(PropertyValueChange(prop, valueChange)) =>
				assert(prop == v2)
				assert(valueChange.oldValue.getString == "replaced")
				assert(valueChange.newValue.getString == "v2_updated")
			case _ => assert(false, s"Expected PropertyValueChange, but got: $latestEvent")
		}
		
		// 2.5 Mutating swapped-out variable should NOT trigger events
		latestEvent = None
		v1.value = "v1_mutated_after_swap"
		assert(latestEvent.isEmpty)
		
		// 3. m -= Variable and m -= "key" cases
		// 3.1 Removing by Variable
		m -= v2
		assert(latestEvent.contains(PropertyRemoved(v2)))
		assert(latestEvent.get.oldValue.getString == "v2_updated")
		
		// Mutating removed variable should NOT trigger events
		latestEvent = None
		v2.value = "v2_mutated_after_removal"
		assert(latestEvent.isEmpty)
		
		// 3.2 Removing by key string
		val aProp = m.property("a")
		m -= "a"
		assert(latestEvent.contains(PropertyRemoved(aProp)))
		assert(latestEvent.get.oldValue.getInt == 4)
		
		// Mutating removed variable should NOT trigger events
		latestEvent = None
		aProp.value = 5
		assert(latestEvent.isEmpty)
		
		// 3.3 Case-insensitive key removal
		m("c") = 100
		assert(latestEvent.isDefined)
		val cProp = m.property("c")
		m -= "C"
		assert(latestEvent.contains(PropertyRemoved(cProp)))
		assert(latestEvent.get.oldValue.getInt == 100)
		
		latestEvent = None
		cProp.value = 200
		assert(latestEvent.isEmpty)
		
		// 4. Test listener removal
		m("d") = "before"
		assert(latestEvent.isDefined)
		latestEvent = None
		m.removeListener(listener)
		m("d") = "after"
		assert(latestEvent.isEmpty)
	}
	
	println("Success!")
}
