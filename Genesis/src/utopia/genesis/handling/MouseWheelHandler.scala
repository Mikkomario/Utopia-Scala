package utopia.genesis.handling

import utopia.genesis.event.MouseWheelEvent
import utopia.inception.handling.{Handler, HandlerType}

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
case object MouseWheelHandlerType extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[MouseWheelListener]
}

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait MouseWheelHandler extends ConsumableEventHandler[MouseWheelListener, MouseWheelEvent] with MouseWheelListener
{
	override def handlerType = MouseWheelHandlerType
	
	override def onMouseWheelRotated(event: MouseWheelEvent) = distribute(event)
	
	override protected def inform(listener: MouseWheelListener, event: MouseWheelEvent) = listener.onMouseWheelRotated(event)
	
	override protected def eventFilterFor(listener: MouseWheelListener) = listener.mouseWheelEventFilter
}
