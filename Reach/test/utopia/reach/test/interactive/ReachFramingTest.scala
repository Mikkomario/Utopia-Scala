package utopia.reach.test.interactive

import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.ColorRole.Secondary
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.wrapper.Framing
import utopia.reach.test.ReachTestContext
import utopia.reach.window.ReachWindow

/**
  * Tests construction of a Framing
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
object ReachFramingTest extends App
{
	import ReachTestContext._
	
	private val pointer = EventfulPointer("Some\nText")
	val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		framingF.expandingToRight.build(ViewTextLabel) { _.withBackground(Secondary)(pointer) }
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
	
	Loop.regularly(4.seconds, waitFirst = true) {
		pointer.update { s =>
			if (math.random() < 0.1)
				s"$s\nMore"
			else
				s"$s Text"
		}
		println(s"${window.stackSize} / ${window.content.stackSize} / ${window.content.content.stackSize}")
	}
}
