package utopia.genesis.handling

import utopia.inception.handling.{Handler, HandlerType}

import scala.concurrent.duration.FiniteDuration

/**
  * This is the handler type instance for all handlers that operate on Actor instances
  * @author Mikko Hilpinen
  * @since 23.12.2016
  */
case object ActorHandlerType extends HandlerType
{
	override def supportedClass = classOf[Actor]
}

/**
  * Actor handlers are used for handling multiple actors as a single actor
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
trait ActorHandler extends Handler[Actor] with Actor
{
	/**
	  * @return The type of this handler
	  */
	override def handlerType = ActorHandlerType
	
	override def act(duration: FiniteDuration) = handle { _.act(duration) }
}