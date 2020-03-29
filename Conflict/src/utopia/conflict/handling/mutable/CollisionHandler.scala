package utopia.conflict.handling.mutable

import utopia.conflict.handling
import utopia.conflict.handling.{Collidable, CollisionListener}
import utopia.inception.handling.mutable.{Handleable, Handler}

object CollisionHandler
{
	def apply(collidableHandler: CollidableHandler, elements: TraversableOnce[CollisionListener] = Vector()) =
		new CollisionHandler(collidableHandler, elements)
	
	def apply(collidableHandler: CollidableHandler, first: CollisionListener, more: CollisionListener*) =
		new CollisionHandler(collidableHandler, first +: more)
	
	def apply(collidables: TraversableOnce[Collidable], listeners: TraversableOnce[CollisionListener]) =
		new CollisionHandler(CollidableHandler(collidables), listeners)
}

/**
  * This is a mutable implementation of the CollisionHandler trait
  * @author Mikko Hilpinen
  * @since 18.4.2019, v1+
  */
class CollisionHandler(override val collidableHandler: CollidableHandler,
					   initialElements: TraversableOnce[CollisionListener] = Vector())
	extends Handler[CollisionListener](initialElements) with handling.CollisionHandler with Handleable
