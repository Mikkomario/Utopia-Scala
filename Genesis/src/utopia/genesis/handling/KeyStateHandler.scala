package utopia.genesis.handling

import utopia.genesis.event.KeyStateEvent
import utopia.inception.handling.{Handler, HandlerType}

case object KeyStateHandlerType extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[KeyStateListener]
}

/**
  * Key state handlers distribute key state events among multiple listeners
  */
trait KeyStateHandler extends EventHandler[KeyStateListener, KeyStateEvent] with KeyStateListener
{
	override def handlerType = KeyStateHandlerType
	
	override def onKeyState(event: KeyStateEvent) = distribute(event)
	
	override protected def eventFilterFor(listener: KeyStateListener) = listener.keyStateEventFilter
	
	override protected def inform(listener: KeyStateListener, event: KeyStateEvent) = listener.onKeyState(event)
}
