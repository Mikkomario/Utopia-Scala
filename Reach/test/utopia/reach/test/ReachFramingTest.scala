package utopia.reach.test

import utopia.flow.time.TimeExtensions._
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackInsets
import utopia.flow.async.process.Loop
import utopia.flow.view.mutable.eventful.PointerWithEvents

/**
  * Tests construction of a Framing
  * @author Mikko Hilpinen
  * @since 17.4.2023, v1.0
  */
object ReachFramingTest extends App
{
	import ReachTestContext._
	
	private val pointer = new PointerWithEvents("Some\nText")
	val window = ReachWindow.popupContextual.using(Framing) { framingF =>
		framingF.build(ViewTextLabel).apply(StackInsets.symmetric(margins.medium.any).expandingToLeft) { labelF =>
			labelF.apply(pointer)
		}
	}
	
	window.visible = true
	start()
	
	Loop.regularly(2.seconds, waitFirst = true) {
		pointer.update { s =>
			if (math.random() < 0.1)
				s"$s\n$s"
			else
				s"$s Text"
		}
	}
}
