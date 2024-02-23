package utopia.genesis.handling.drawing

import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.DrawOrder
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.Handlers
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * Common trait for Drawable wrappers that perform some kind of a transformation etc. in order to modify
  * wrapped item drawing, as well as mouse positions
  * @author Mikko Hilpinen
  * @since 11/02/2024, v4.0
  */
trait TransformingDrawableWrapper extends Drawable with CoordinateTransform
{
	// ABSTRACT ------------------------
	
	/**
	  * @return The wrapped item
	  */
	protected def wrapped: Drawable
	
	/**
	  * @return Handler used for delivering transformed mouse events
	  */
	protected def mouseHandler: TransformedMouseHandler
	
	def repaintListeners: Seq[RepaintListener]
	protected def repaintListeners_=(listeners: Seq[RepaintListener]): Unit
	
	/**
	  * @param region A sub-region within the draw bounds of the wrapped item
	  * @return A matching sub-region within transformed draw bounds
	  */
	protected def viewSubRegion(region: Bounds): Bounds
	
	
	// INITIAL CODE --------------------

	// Whenever the wrapped item requests a repaint, modifies the repaint call to match the new position
	wrapped.addRepaintListener { (_, region, priority) => repaint(region.map(viewSubRegion), priority) }
	
	
	// IMPLEMENTED  --------------------
	
	override def handleCondition: FlagLike = wrapped.handleCondition
	
	override def drawOrder: DrawOrder = wrapped.drawOrder
	
	override def addRepaintListener(listener: RepaintListener): Unit = {
		if (!repaintListeners.contains(listener))
			repaintListeners :+= listener
	}
	override def removeRepaintListener(listener: RepaintListener): Unit =
		repaintListeners = repaintListeners.filterNot { _ == listener }
	
	override def setupMouseEvents(parentHandlers: Handlers, disableMouseToWrapped: Boolean = false) = {
		// If the wrapped item supports mouse events, starts delivering them to that item as well (unless disabled)
		if (!disableMouseToWrapped && (wrapped.isInstanceOf[MouseMoveListener] ||
			wrapped.isInstanceOf[MouseButtonStateListener] || wrapped.isInstanceOf[MouseWheelListener] ||
			wrapped.isInstanceOf[MouseDragListener]))
			mouseHandler.handlers += wrapped
		
		// Starts receiving events from above
		parentHandlers += mouseHandler
		// Returns the converted mouse handlers
		mouseHandler.handlers
	}
}
