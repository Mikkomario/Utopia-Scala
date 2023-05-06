package utopia.reach.test

import utopia.firmament.localization.LocalString._
import utopia.flow.async.process.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.reach.component.label.text.TextLabel
import utopia.reach.container.multi.ViewStack
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow

/**
  * Tests view stack component
  *
  * Instructions:
  *     - You should be able to see a window where number labels appear and reset once there are 9
  * Known bugs:
  *     - Window won't paint initially, nor will the initial revalidations work
  *         - This is because Swing rejects window size changes because the window goes below its minimum size.
  *           This is a non-issue, because normally windows are quite much larger than the absolute minimum.
  *
  * @author Mikko Hilpinen
  * @since 6.1.2021, v0.1
  */
object ViewStackTest extends App
{
	import ReachTestContext._
	
	// Data
	val numberPointer = new PointerWithEvents[Int](1)
	
	// Creates the components
	val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		// Framing
		framingF.build(ViewStack).apply(margins.aroundMedium) { stackF =>
			// Stack
			stackF.mapContext { _.withTextExpandingToRight }.row.build(TextLabel) { labelFactories =>
				// 1-9 Labels
				(1 to 9).map { i =>
					labelFactories.next().apply(i.toString.noLanguageLocalizationSkipped) ->
						numberPointer.map { _ >= i }
				}.toVector
			}
		}
	}
	
	// Displays the window
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	window.display(centerOnParent = true)
	start()
	
	// Updates the number within a background loop
	Loop.regularly(1.seconds, waitFirst = true) { numberPointer.update { i => if (i >= 9) 1 else i + 1 } }
}
