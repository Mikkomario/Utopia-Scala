package utopia.genesis.handling.action

import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

import scala.concurrent.duration.FiniteDuration


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