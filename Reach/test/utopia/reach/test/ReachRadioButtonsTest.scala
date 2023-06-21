package utopia.reach.test

import utopia.firmament.localization.LocalizedString
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.reach.component.input.selection.RadioButtonGroup
import utopia.reach.container.wrapper.Framing
import utopia.reach.window.ReachWindow

/**
  * Tests radio button creation
  * @author Mikko Hilpinen
  * @since 20.3.2023, v0.5.1
  */
object ReachRadioButtonsTest extends App
{
	import ReachTestContext._
	
	// Controls
	private val valuePointer = new PointerWithEvents(1)
	
	// Creates the components
	val window = ReachWindow.contentContextual.using(Framing) { (_, framingF) =>
		// Framing
		framingF.build(RadioButtonGroup) { btnF =>
			// Radio buttons
			btnF(Vector[(Int, LocalizedString)](1 -> "First", 2 -> "Second", 3 -> "Third Option"), valuePointer)
		}
	}
	
	// Displays the window
	window.display(centerOnParent = true)
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	start()
	
	valuePointer.addListener { e => println(e) }
}
