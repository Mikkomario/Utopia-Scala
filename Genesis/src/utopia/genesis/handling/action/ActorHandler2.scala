package utopia.genesis.handling.action

import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

import scala.concurrent.duration.FiniteDuration

object ActorHandler2 extends FromCollectionFactory[Actor2, ActorHandler2]
{
	// IMPLEMENTED  ------------------------
	
	override def from(items: IterableOnce[Actor2]): ActorHandler2 = new ActorHandler2(items)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param actors Actors for this handler to manage
	  * @return A new actor handler
	  */
	def apply(actors: IterableOnce[Actor2]) = new ActorHandler2(actors)
}

/**
  * Actor handlers are used for handling multiple actors as a single actor
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class ActorHandler2(initialItems: IterableOnce[Actor2]) extends DeepHandler2[Actor2](initialItems) with Actor2
{
	override def act(duration: FiniteDuration) = items.foreach { _.act(duration) }
	
	override protected def asHandleable(item: Handleable2): Option[Actor2] = item match {
		case a: Actor2 => Some(a)
		case _ => None
	}
}