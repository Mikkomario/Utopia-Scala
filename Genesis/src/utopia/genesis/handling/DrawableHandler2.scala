package utopia.genesis.handling

import utopia.inception.handling.{Handler, HandlerType}

case object DrawableHandlerType2 extends HandlerType
{
	/**
	  * @return The class supported by this handler type
	  */
	override def supportedClass = classOf[Drawable2]
}

trait DrawableHandler2 extends Handler[Drawable2] with Drawable2
{
	/**
	  * @return The type of this handler
	  */
	override def handlerType = DrawableHandlerType
}