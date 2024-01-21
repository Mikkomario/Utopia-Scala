package utopia.reach.test

import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.drawing.immutable.BorderDrawer
import utopia.firmament.model.Border
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.reach.component.label.empty.EmptyLabel
import utopia.reach.cursor.{DragTo, DragToResize}
import utopia.reach.window.ReachWindow

/**
  * Tests custom window-resize
  * @author Mikko Hilpinen
  * @since 20/01/2024, v1.2
  */
object DragToResizeWindowTest extends App
{
	import ReachTestContext._
	
	private val dragInsets = Insets.symmetric(16)
	
	private val window = ReachWindow.contentContextual.borderless.using(EmptyLabel) { (_, labelF) =>
		labelF.withCustomDrawer(BorderDrawer(Border(dragInsets, Color.red)))(
			200.any.lowPriority.square.mapWidth { _.withMax(600).withMin(100) })
	}
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	
	println(window.content.stackSize)
	DragTo.resize.repositioningWindow.expandingAtSides.fillingAtTop.applyTo(window.content, dragInsets)
	// DragToResize.expandingAtSides.fillingAtTop.applyTo(window.content, dragInsets)
	
	start()
	window.display(centerOnParent = true)
}
