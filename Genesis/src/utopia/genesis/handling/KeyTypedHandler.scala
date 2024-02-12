package utopia.genesis.handling

import utopia.genesis.event.KeyTypedEvent
import utopia.inception.handling.{Handler, HandlerType}

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
case object KeyTypedHandlerType extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[KeyTypedListener]
}

/**
  * This handler is used for distributing key typed events between multiple listeners
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait KeyTypedHandler extends Handler[KeyTypedListener] with KeyTypedListener
{
	override def handlerType = KeyTypedHandlerType
	
	override def onKeyTyped(event: KeyTypedEvent) = handle { _.onKeyTyped(event) }
}
