package utopia.reflection.component

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.{Point, Size}
import utopia.reflection.event.ResizeListener

/**
  * These wrappers look like components, but only wrap another component
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait ComponentWrapper extends ComponentLike
{
	// ABSTRACT	-------------------
	
	/**
	  * @return The component wrapped by this wrapper
	  */
	protected def wrapped: ComponentLike
	
	
	// IMPLEMENTED	---------------
	
	override def resizeListeners = wrapped.resizeListeners
	override def resizeListeners_=(listeners: Vector[ResizeListener]) = wrapped.resizeListeners = listeners
	
	override def parent = wrapped.parent
	
	override def isVisible = wrapped.isVisible
	override def isVisible_=(isVisible: Boolean) = wrapped.isVisible = isVisible
	
	override def background = wrapped.background
	override def background_=(color: Color) = wrapped.background = color
	
	override def isTransparent = wrapped.isTransparent
	
	override def fontMetrics = wrapped.fontMetrics
	
	override def mouseButtonHandler = wrapped.mouseButtonHandler
	override def mouseMoveHandler = wrapped.mouseMoveHandler
	override def mouseWheelHandler = wrapped.mouseWheelHandler
	override def keyStateHandler = wrapped.keyStateHandler
	override def keyTypedHandler = wrapped.keyTypedHandler
	
	override def position = wrapped.position
	override def position_=(p: Point) = wrapped.position = p
	
	override def size = wrapped.size
	override def size_=(s: Size) = wrapped.size = s
	
	override def children = wrapped.children
}
