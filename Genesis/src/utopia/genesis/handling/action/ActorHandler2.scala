package utopia.genesis.handling.action

import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2, HandlerFactory}

import scala.annotation.unused
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

object ActorHandler2
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A factory for constructing these handlers
	  */
	val factory = ActorHandlerFactory()
	
	
	// IMPLICIT ---------------------------
	
	implicit def objectToFactory(@unused o: ActorHandler2.type): ActorHandlerFactory = factory
	
	
	// NESTED   ---------------------------
	
	case class ActorHandlerFactory(override val condition: FlagLike = AlwaysTrue)
		extends HandlerFactory[Actor2, ActorHandler2, ActorHandlerFactory]
	{
		override def usingCondition(newCondition: FlagLike): ActorHandlerFactory = copy(condition = newCondition)
		
		override def apply(initialItems: IterableOnce[Actor2]): ActorHandler2 =
			new ActorHandler2(initialItems, condition)
	}
}

/**
  * Actor handlers are used for handling multiple actors as a single actor
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class ActorHandler2(initialItems: IterableOnce[Actor2] = Iterable.empty,
                    additionalCondition: Changing[Boolean] = AlwaysTrue)
	extends DeepHandler2[Actor2](initialItems, additionalCondition) with Actor2
{
	override def act(duration: FiniteDuration) = items.foreach { _.act(duration) }
	
	override protected def asHandleable(item: Handleable2): Option[Actor2] = item match {
		case a: Actor2 => Some(a)
		case _ => None
	}
}