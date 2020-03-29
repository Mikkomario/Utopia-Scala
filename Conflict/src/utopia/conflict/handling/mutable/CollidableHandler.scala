package utopia.conflict.handling.mutable

import utopia.conflict.handling
import utopia.conflict.handling.Collidable
import utopia.inception.handling.mutable.Handler

object CollidableHandler
{
	def apply(elements: TraversableOnce[Collidable] = Vector()) = new CollidableHandler(elements)
	
	def apply(element: Collidable) = new CollidableHandler(Vector(element))
	
	def apply(first: Collidable, second: Collidable, more: Collidable*) = new CollidableHandler(Vector(first, second) ++ more)
}

/**
  * This is a mutable implementation of the CollidableHandler trait
  * @author Mikko Hilpinen
  * @since 18.4.2019, v1+
  */
class CollidableHandler(initialElements: TraversableOnce[Collidable]) extends Handler[Collidable](initialElements)
	with handling.CollidableHandler
