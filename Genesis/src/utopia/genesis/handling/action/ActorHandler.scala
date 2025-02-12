package utopia.genesis.handling.action

import utopia.flow.collection.immutable.Empty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

object ActorHandler
{
	// COMPUTED   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	def factory(implicit log: Logger) = ActorHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: ActorHandler.type)(implicit log: Logger): ActorHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class ActorHandlerFactory(override val condition: Flag = AlwaysTrue)(implicit log: Logger)
		extends HandlerFactory[Actor, ActorHandler, ActorHandlerFactory]
	{
		override def usingCondition(newCondition: Flag): ActorHandlerFactory = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[Actor]): ActorHandler =
			new ActorHandler(initialItems, condition)
	}
}

/**
  * Actor handlers are used for handling multiple actors as a single actor
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class ActorHandler(initialItems: IterableOnce[Actor] = Empty, additionalCondition: Changing[Boolean] = AlwaysTrue)
                  (implicit log: Logger)
	extends DeepHandler[Actor](initialItems, additionalCondition) with Actor
{
	override def act(duration: FiniteDuration) = items.foreach { _.act(duration) }
	
	override protected def asHandleable(item: Handleable): Option[Actor] = item match {
		case a: Actor => Some(a)
		case _ => None
	}
}