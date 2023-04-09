package utopia.genesis.handling

import utopia.inception.handling.{Handler, HandlerType}

@deprecated("Replaced with a new implementation", "v3.3")
case object DrawableHandlerType extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[Drawable]
}

@deprecated("Replaced with a new implementation", "v3.3")
trait DrawableHandler extends Handler[Drawable] with Drawable
{
	/**
	  * @return The type of this handler
	  */
	override def handlerType = DrawableHandlerType
}