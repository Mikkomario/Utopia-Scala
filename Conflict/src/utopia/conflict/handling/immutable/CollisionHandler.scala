package utopia.conflict.handling.immutable

import utopia.conflict.handling.{Collidable, CollisionListener}
import utopia.inception.handling.immutable.Handler
import utopia.conflict.handling
import utopia.inception.handling.immutable

object CollisionHandler
{
	def apply(collidableHandler: handling.CollidableHandler, elements: TraversableOnce[CollisionListener] = Vector()) =
		new CollisionHandler(collidableHandler, elements)
	
	def apply(collidableHandler: handling.CollidableHandler, first: CollisionListener, more: CollisionListener*) =
		new CollisionHandler(collidableHandler, first +: more)
	
	/**
	  * @param collidables Collidable instances
	  * @param listeners Collision listeners
	  * @return A collision handler that keeps track of both
	  */
	def apply(collidables: TraversableOnce[Collidable], listeners: TraversableOnce[CollisionListener]) =
		new CollisionHandler(CollidableHandler(collidables), listeners)
}

/**
  * This is an immutable implementation of the CollisionHandler trait
  * @author Mikko Hilpinen
  * @since 18.4.2019, v1+
  */
class CollisionHandler(val collidableHandler: handling.CollidableHandler, initialElements: TraversableOnce[CollisionListener] = Vector())
	extends Handler[CollisionListener](initialElements) with handling.CollisionHandler with immutable.Handleable