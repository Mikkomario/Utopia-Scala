package utopia.reach.test.interactive

import utopia.firmament.model.stack.StackSize
import utopia.flow.view.mutable.eventful.AssignableOnce
import utopia.genesis.handling.event.keyboard.KeyStateListener
import utopia.genesis.handling.event.mouse.{MouseButtonStateListener, MouseWheelListener}
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.component.template.ReachComponent
import utopia.reach.drawing.MousePositionDrawer
import utopia.reach.test.ReachTestContext
import utopia.reach.window.ReachWindow

/**
  * Tests mouse events in a Reach Window context
  * @author Mikko Hilpinen
  * @since 23/02/2024, v1.3
  */
object WindowMouseTest extends App
{
	import ReachTestContext._
	
	private val window = ReachWindow.contentContextual.using(EmptyLabel) { (_, labelF) =>
		val labelPointer = AssignableOnce[ReachComponent]()
		val drawer = new MousePositionDrawer(labelPointer, 3)
		val label = labelF.withCustomDrawer(drawer)(StackSize.any(Size.square(480)))
		labelPointer.set(label)
		
		label += MouseButtonStateListener.unconditional { e => println(e) }
		label += MouseWheelListener.unconditional { e => println(e) }
		
		label
	}
	
	window.setToCloseOnEsc()
	window.focusKeyStateHandler += KeyStateListener.unconditional { e => println(e) }
	
	window.display(centerOnParent = true)
	start()
}
