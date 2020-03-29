package utopia.genesis.handling

import utopia.genesis.event.MouseMoveEvent
import utopia.inception.handling.{Handler, HandlerType}

case object MouseMoveHandlerType extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[MouseMoveListener]
}

/**
  * MouseMoveHandlers distribute mouse move events among multiple listeners
  */
trait MouseMoveHandler extends EventHandler[MouseMoveListener, MouseMoveEvent] with MouseMoveListener
{
	override def handlerType = MouseMoveHandlerType
	
	override def onMouseMove(event: MouseMoveEvent) = distribute(event)
	
	override protected def eventFilterFor(listener: MouseMoveListener) = listener.mouseMoveEventFilter
	
	override protected def inform(listener: MouseMoveListener, event: MouseMoveEvent) = listener.onMouseMove(event)
}
