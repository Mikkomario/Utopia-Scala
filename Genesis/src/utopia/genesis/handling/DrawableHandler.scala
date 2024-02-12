package utopia.genesis.handling

import utopia.inception.handling.{Handler, HandlerType}

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
case object DrawableHandlerType2 extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[Drawable]
}

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait DrawableHandler extends Handler[Drawable] with Drawable
{
	/**
	  * @return The type of this handler
	  */
	override def handlerType = DrawableHandlerType2
}