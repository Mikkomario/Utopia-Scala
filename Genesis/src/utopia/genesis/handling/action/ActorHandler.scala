package utopia.genesis.handling.action

import utopia.flow.collection.immutable.Empty
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.template.{DeepHandler, Handleable, HandlerFactory}

import scala.annotation.unused
import utopia.flow.time.Duration
import scala.language.implicitConversions
import scala.util.Try

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
	// IMPLEMENTED  ----------------------
	
	override def act(duration: Duration) = {
		// Delivers the action event to all active listeners
		// Catches and logs thrown errors, and removes any actor that threw an exception
		val actorsToRemove = items.filter { actor =>
			val result = Try { actor.act(duration) }
			result.logWithMessage(s"Actor $actor threw a failure during act()")
			result.isFailure
		}
		if (actorsToRemove.nonEmpty)
			removeWhere(actorsToRemove.contains)
	}
	
	override protected def asHandleable(item: Handleable): Option[Actor] = item match {
		case a: Actor => Some(a)
		case _ => None
	}
}