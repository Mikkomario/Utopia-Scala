package utopia.reach.test

import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.window.ReachWindow

/**
  * Tests memory consumption when creating multiple windows back to back
  * @author Mikko Hilpinen
  * @since 31.8.2023, v1.1
  */
object RepeatedWindowsTest extends App
{
	import ReachTestContext._
	
	val runTime = Runtime.getRuntime
	val maxMemory = runTime.maxMemory()
	def printMemoryStatus() = {
		val used = runTime.totalMemory()
		println(s"${ used * 100 / maxMemory }% (${ used / 100000 } M) used")
	}
	
	start()
	
	Iterator.continually {
		printMemoryStatus()
		val window = ReachWindow.contentContextual.using(EmptyLabel) { (_, labelF) =>
			labelF(Size.square(500).any)
		}
		window.display(centerOnParent = true)
		Wait(0.5.seconds)
		window.close()
		System.gc()
		Wait(0.5.seconds)
	}.take(50).foreach { _ => () }
	
	println("Done!")
}
