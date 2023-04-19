package utopia.reach.test

import utopia.flow.time.TimeExtensions._
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.flow.async.process.Loop
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.color.ColorRole.Secondary

/**
  * Tests construction of a Framing
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
object ReachFramingTest extends App
{
	import ReachTestContext._
	
	private val pointer = new PointerWithEvents("Some\nText")
	val window = ReachWindow.popupContextual.using(Framing) { (_, framingF) =>
		framingF.build(ViewTextLabel).apply(StackInsets.symmetric(margins.aroundMedium).expandingToRight) { labelF =>
			labelF.withBackground(pointer, Secondary)
		}
	}
	
	window.visible = true
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
