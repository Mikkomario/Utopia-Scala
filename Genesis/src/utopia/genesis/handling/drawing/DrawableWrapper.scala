package utopia.genesis.handling.drawing

import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.{DrawOrder, Drawer}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * Common trait for classes that wrap other Drawable items without altering their properties
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
trait DrawableWrapper extends Drawable2
{
	// ABSTRACT    -----------------------
	
	/**
	  * @return The wrapped drawable instance
	  */
	protected def wrapped: Drawable2
	
	
	// IMPLEMENTED  -----------------------
	
	override def handleCondition: FlagLike = wrapped.handleCondition
	override def drawBoundsPointer: Changing[Bounds] = wrapped.drawBoundsPointer
	
	override def drawOrder: DrawOrder = wrapped.drawOrder
	override def opaque: Boolean = wrapped.opaque
	
	override def repaintListeners: Iterable[RepaintListener] = wrapped.repaintListeners
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = wrapped.draw(drawer, bounds)
	
	override def addRepaintListener(listener: RepaintListener): Unit = wrapped.addRepaintListener(listener)
	override def removeRepaintListener(listener: RepaintListener): Unit = wrapped.removeRepaintListener(listener)
}
