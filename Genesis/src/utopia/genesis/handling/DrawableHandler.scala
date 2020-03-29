package utopia.genesis.handling

import utopia.inception.handling.{Handler, HandlerType}

case object DrawableHandlerType extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[Drawable]
}

trait DrawableHandler extends Handler[Drawable] with Drawable
{
	/**
	  * @return The type of this handler
	  */
	override def handlerType = DrawableHandlerType
}