package utopia.flow.test.event

import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.test.TestContext._

/**
  * Tests delayed mirroring
  * @author Mikko Hilpinen
  * @since 2.12.2022, v2.0
  */
object DelayedViewTest extends App
{
	val p = new PointerWithEvents(0)
	val d = p.delayedBy(0.5.seconds)
	
	assert(p.value == 0)
	assert(d.value == 0)
	
	p.value = 1
	
	assert(p.value == 1)
	assert(d.value == 0)
	
	Wait(0.2.seconds)
	
	assert(d.value == 0)
	
	Wait(0.5.seconds)
	
	assert(d.value == 1)
	
	p.value = 2
	
	assert(d.value == 1)
	
	Wait(0.1.seconds)
	
	assert(d.value == 1)
	
	Wait(0.5.seconds)
	
	assert(d.value == 2)
	
	p.value = 3
	
	Wait(0.3.seconds)
	
	assert(d.value == 2)
	
	p.value = 4
	Wait(0.3.seconds)
	
	assert(d.value == 2)
	
	Wait(0.3.seconds)
	
	assert(d.value == 4)
	
	println("Success!")
}
