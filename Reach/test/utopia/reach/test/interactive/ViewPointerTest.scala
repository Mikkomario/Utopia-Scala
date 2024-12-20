package utopia.reach.test.interactive

import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.{Color, Hsl}
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext
import utopia.reach.window.ReachWindow

/**
  * Testing pointer management in Reach Windows and Text View Labels
  * @author Mikko Hilpinen
  * @since 19/01/2024, v1.2
  */
object ViewPointerTest extends App
{
	import ReachTestContext._
	
	private val counterPointer = EventfulPointer(0)
	private val colorPointer = counterPointer.map[Color] { i => Hsl(Angle.circles((i % 10) / 10.0)) }
	
	private val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.build(ViewTextLabel) { _.mapContext { _.withTextColorPointer(colorPointer) }(counterPointer) }.toTuple
	}
	
	window.setToCloseOnEsc()
	// window.setToExitOnClose()
	
	window.fullyVisibleFlag.addContinuousListener { e => println(s"Window visible state $e (destiny = ${window.fullyVisibleFlag.destiny})") }
	window.openFlag.addContinuousListener { e => println(s"Window open state $e (destiny = ${window.openFlag.destiny})") }
	window.canvas.attachmentPointer.addContinuousListener { e => println(s"Canvas attachment state $e (destiny = ${window.canvas.attachmentPointer.destiny})") }
	window.result.parentHierarchy.linkPointer.addContinuousListener { e => println(s"Label attachment $e (destiny = ${window.result.parentHierarchy.linkPointer.destiny})") }
	
	start()
	window.display(centerOnParent = true)
	
	Loop.regularly(0.2.seconds, waitFirst = true) { counterPointer.update { _ + 1 } }
}
