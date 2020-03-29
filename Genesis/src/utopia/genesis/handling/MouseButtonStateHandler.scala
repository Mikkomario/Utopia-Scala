package utopia.genesis.handling

import utopia.genesis.event.MouseButtonStateEvent
import utopia.inception.handling.{Handler, HandlerType}

case object MouseButtonStateHandlerType extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[MouseButtonStateListener]
}

/**
  * Key state handlers distribute key state events among multiple listeners
  */
trait MouseButtonStateHandler extends ConsumableEventHandler[MouseButtonStateListener, MouseButtonStateEvent]
	with MouseButtonStateListener
{
	/**
	  * @return The type of this handler
	  */
	override def handlerType = MouseButtonStateHandlerType
	
	override def onMouseButtonState(event: MouseButtonStateEvent) = distribute(event)
	
	override protected def inform(listener: MouseButtonStateListener, event: MouseButtonStateEvent) = listener.onMouseButtonState(event)
	
	override protected def eventFilterFor(listener: MouseButtonStateListener) = listener.mouseButtonStateEventFilter
}
