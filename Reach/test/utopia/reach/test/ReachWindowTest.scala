package utopia.reach.test

import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.util.Screen
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.window.ReachWindow

/**
  * Tests simple reach window creation
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
object ReachWindowTest extends App
{
	import ReachTestContext._
	
	val textPointer = new PointerWithEvents("Text")
	
	val window = ReachWindow.withResizeLogic(User)
		.withWindowBackground(colors.primary.default).muchLarger
		.withTextInsetsScaledBy(4).withoutShrinkingText.withLineSplitThreshold(Screen.width / 3.0)
		.using(ViewTextLabel, title = "Test") { (_, f) =>
			f(textPointer)
		}
	
	Loop.regularly(2.seconds, waitFirst = true) {
		textPointer.update { t => s"more $t" }
	}
	
	window.focusedFlag.addContinuousListenerAndSimulateEvent(false) { e =>
		println(if (e.newValue) "Window focused" else "Window lost focus")
	}
	
	window.setToExitOnClose()
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	
	start()
}
