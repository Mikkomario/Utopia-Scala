package utopia.reach.test

import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.AsyncExtensions._
import utopia.genesis.util.Screen
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.window.ReachWindow

/**
  * Tests closing of multiple windows using the same hotkey
  * @author Mikko Hilpinen
  * @since 21.8.2023, v1.1
  */
object MultiWindowClosingTest extends App
{
	import ReachTestContext._
	
	def createWindow() = {
		val window = ReachWindow.contentContextual.using(EmptyLabel) { (_, labelF) => labelF(Size(300, 300).any) }
		window.setToCloseOnEsc()
		window
	}
	
	// Creates the windows
	val windowCount = 3
	val windows = (1 to windowCount).map { i =>
		val w = createWindow()
		w.displayAndThen() {
			println(w.size)
			w.position = (Screen.size / Vector2D(windowCount + 1, 2) * Vector2D(i, 1) - w.size / 2).toPoint
		}
		w
	}
	
	// Waits until all windows have closed
	windows.map { _.closeFuture }.waitFor()
	println("All windows closed!")
}
