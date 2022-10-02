package utopia.flow.test.async

import utopia.flow.view.mutable.async.VolatileFlag

/**
 * This app tests volatile classes
 * @author Mikko Hilpinen
 * @since 17.4.2019
 */
@deprecated("Needs to be rewritten", "v2.0")
object VolatileTest extends App
{
	// Tests volatile flag logic
	var called = false
	val flag = new VolatileFlag()
	
	// Checks initial state first
	assert(!flag.isSet)
	assert(!called)
	
	// Sets the flag and calls
	flag.runAndSet { called = true }
	
	assert(flag.isSet)
	assert(called)
	
	// Tries to set the flag again (shouldn't call)
	called = false
	flag.runAndSet { called = true }
	
	assert(flag.isSet)
	assert(!called)
	
	// Calls on non-set flags (shouldn't call)
	flag.doIfNotSet { called = true }
	
	assert(flag.isSet)
	assert(!called)
	
	// Resets flag
	flag.reset()
	
	assert(!flag.isSet)
	
	// Calls on non-set flags (should call)
	flag.doIfNotSet { called = true }
	
	assert(!flag.isSet)
	assert(called)
	
	assert(!flag.getAndSet())
	assert(flag.getAndSet())
	flag.reset()
	assert(!flag.isSet)
	
	println("Success!")
}
