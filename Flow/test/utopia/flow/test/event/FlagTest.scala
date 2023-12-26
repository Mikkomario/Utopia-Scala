package utopia.flow.test.event

import utopia.flow.event.listener.ChangeListener
import utopia.flow.view.mutable.eventful.{EventfulPointer, Flag, ResettableFlag}
import utopia.flow.view.template.eventful.FlagLike

/**
  * Tests eventful flags
  * @author Mikko Hilpinen
  * @since 15.11.2023, v2.3
  */
object FlagTest extends App
{
	val i1 = Flag()
	val ri1 = ResettableFlag()
	
	var lastEvent = false
	val updateLastEventListener = ChangeListener.continuous[Boolean, Unit] { e => lastEvent = e.newValue }
	
	// Tests && (can't optimize)
	val and1 = i1 && ri1
	and1.addListener(updateLastEventListener)
	
	assert(and1.isNotSet)
	assert(i1.numberOfListeners == 1)
	assert(ri1.numberOfListeners == 1)
	
	ri1.set()
	
	assert(and1.isNotSet)
	assert(ri1.hasListeners)
	assert(i1.hasListeners)
	
	i1.set()
	
	assert(lastEvent)
	assert(and1.isSet)
	assert(i1.hasNoListeners)
	assert(ri1.hasListeners)
	
	ri1.reset()
	
	assert(and1.isNotSet)
	assert(!lastEvent)
	
	// Tests listener-clearing
	and1.removeListener(updateLastEventListener)
	assert(and1.hasNoListeners)
	assert(ri1.hasNoListeners)
	
	// Tests !
	val i2 = Flag()
	val ni2 = !i2
	ni2.addListener(updateLastEventListener)
	lastEvent = true
	
	assert(ni2.isSet)
	assert(ni2.hasListeners)
	assert(i2.hasListeners)
	
	i2.set()
	
	assert(ni2.isNotSet)
	assert(!lastEvent)
	assert(ni2.hasNoListeners)
	assert(i2.hasNoListeners)
	assert(!i2.mayChange)
	assert(!ni2.mayChange)
	
	// Tests && with ! - Optimized
	val i3 = Flag()
	val ni3 = !i3 // Initially true
	val pi1 = EventfulPointer(false)
	val piv1: FlagLike = pi1.readOnly // Initially false
	val and2 = ni3 && piv1 // Initially false
	ni2.removeListener(updateLastEventListener)
	and2.addListener(updateLastEventListener)
	lastEvent = false
	
	assert(ni3.isSet)
	assert(piv1.isNotSet)
	assert(and2.isNotSet)
	assert(ni3.mayChange)
	assert(piv1.mayChange)
	assert(and2.mayChange)
	
	pi1.value = true
	
	assert(piv1.isSet)
	assert(and2.isSet)
	assert(lastEvent)
	assert(and2.mayChange)
	assert(piv1.mayChange)
	
	i3.set()
	
	assert(ni3.isNotSet)
	assert(and2.isNotSet)
	assert(!lastEvent)
	assert(!ni3.mayChange)
	assert(!and2.mayChange)
	assert(ni3.hasNoListeners)
	assert(i3.hasNoListeners)
	assert(and2.hasNoListeners)
	assert(piv1.hasNoListeners)
	
	// Tests || (optimized)
	val i4 = Flag()
	val or1 = i4 || piv1 // Piv1 == true => or == true
	or1.addListener(updateLastEventListener)
	lastEvent = true
	
	assert(i4.isNotSet)
	assert(piv1.isSet)
	assert(or1.isSet)
	assert(i4.hasListeners)
	assert(piv1.hasListeners)
	assert(or1.hasListeners)
	assert(i4.mayChange)
	assert(piv1.mayChange)
	assert(or1.mayChange)
	
	pi1.value = false
	
	assert(piv1.isNotSet)
	assert(or1.isNotSet)
	assert(!lastEvent)
	
	i4.set()
	
	assert(or1.isSet)
	assert(!i4.mayChange)
	assert(!or1.mayChange)
	assert(i4.hasNoListeners)
	assert(or1.hasNoListeners)
	assert(piv1.hasNoListeners)
	
	println("Success!")
}
