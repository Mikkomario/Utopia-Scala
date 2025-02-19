package utopia.reach.test.interactive

import utopia.flow.view.mutable.Pointer
import utopia.genesis.handling.event.keyboard.{KeyStateListener, KeyboardEvents}
import utopia.paradigm.transform.Adjustment
import utopia.reach.component.label.text.ViewTextLabel
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
  * Tests pointer-based line-splitting
  * @author Mikko Hilpinen
  * @since 18.02.2025, v1.6
  */
object LineSplitTest extends App
{
	// ATTRIBUTES   -----------------------
	
	private val adj = Adjustment(0.15)
	private val splitP = Pointer.eventful(320.0)
	
	private val window = ReachWindow.contentContextual.using(ViewTextLabel) { (_, labelF) =>
		labelF.mapContext { _.withLineSplitThresholdPointer(splitP) }.text(
			"A longer string intended to span multiple lines. This tests the automated line-splitting -feature. \nThis line always starts on a new line, however.")
	}
	
	
	// APP CODE ---------------------------
	
	KeyboardEvents += KeyStateListener.pressed.anyArrow { e =>
		e.horizontalArrow.foreach { dir =>
			println("Adjusting")
			splitP.update { _ * adj(dir.sign.modifier) }
		}
	}
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	start()
	
	window.display(centerOnParent = true)
}
