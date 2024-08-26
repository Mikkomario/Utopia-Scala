package utopia.reach.test.interactive.drawable

import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.eventful.Fixed
import utopia.genesis.handling.template.Handleable
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.label.drawable.DrawableCanvas
import utopia.reach.cursor.DragTo
import utopia.reach.test.ReachTestContext
import utopia.reach.test.ReachTestContext._
import utopia.reach.window.ReachWindow

/**
  * Sets up a context for testing with a DrawableCanvas
  * @author Mikko Hilpinen
  * @since 25.08.2024, v1.4
  */
object DrawableReachTestContext
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * Bounds of the viewed area
	  */
	val viewBounds = Bounds(0, 0, 640, 480)
	
	/**
	  * Window used in these tests. Only contains a canvas element.
	  */
	val window = ReachWindow.contentContextual.borderless.using(DrawableCanvas) { (_, canvasF) =>
		canvasF.withMinSize(Size.square(320))(Fixed(viewBounds))
	}
	/**
	  * Canvas used in these tests
	  */
	val canvas = window.content
	
	
	// INITIAL CODE --------------------
	
	DragTo.resize.applyTo(window.content, Insets.symmetric(16))
	
	window.setToCloseOnEsc()
	window.setToExitOnClose()
	
	
	// OTHER    ------------------------
	
	/**
	  * Starts this test by displaying the window / canvas
	  * @param viewedItems Items that will be added to the canvas element (for drawing & mouse-event delivery)
	  */
	def start(viewedItems: IterableOnce[Handleable] = Empty) = {
		canvas.viewHandlers ++= viewedItems
		window.display(centerOnParent = true)
		ReachTestContext.start()
	}
	/**
	  * Starts this test by displaying the window / canvas
	  * @param firstItem & moreItems Items that will be added to the canvas element (for drawing & mouse-event delivery)
	  */
	def start(firstItem: Handleable, moreItems: Handleable*): Unit = start(firstItem +: moreItems)
}
