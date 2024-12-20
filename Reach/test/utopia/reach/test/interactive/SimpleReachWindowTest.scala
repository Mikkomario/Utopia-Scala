package utopia.reach.test.interactive

import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.test.ReachTestContext
import utopia.reach.window.ReachWindow

/**
  * Tests reach window creation with not many components
  * @author Mikko Hilpinen
  * @since 4.5.2023, v1.1
  */
object SimpleReachWindowTest extends App
{
	// [142.0-245.9-, 116.0-189.0-]
	import ReachTestContext._
	
	val window = ReachWindow.contentContextual.using(EmptyLabel) { (_, labelF) =>
		labelF(StackSize(StackLength(142, 245.9), StackLength(116, 189)))
	}
	
	window.setToCloseOnEsc()
	window.display(centerOnParent = true)
	start()
}
