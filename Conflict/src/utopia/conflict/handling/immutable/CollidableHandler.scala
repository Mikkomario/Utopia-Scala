package utopia.conflict.handling.immutable

import utopia.conflict.handling
import utopia.conflict.handling.Collidable
import utopia.inception.handling.immutable.Handler

object CollidableHandler
{
	/**
	  * @param elements Elements for the handler
	  * @return A new handler
	  */
	def apply(elements: TraversableOnce[Collidable] = Vector()) = new CollidableHandler(elements)
	
	/**
	  * @param element A single element for the handler
	  * @return A new handler
	  */
	def apply(element: Collidable) = new CollidableHandler(Vector(element))
	
	/**
	  * @param first First element
	  * @param second Another element
	  * @param more More elements
	  * @return A new handler with all specified elements
	  */
	def apply(first: Collidable, second: Collidable, more: Collidable*) = new CollidableHandler(Vector(first, second) ++ more)
}

/**
  * This handler handles multiple collidable instances
  * @author Mikko Hilpinen
  * @since 18.4.2019, v1+
  */
class CollidableHandler(initialElements: TraversableOnce[Collidable]) extends Handler[Collidable](initialElements)
	with handling.CollidableHandler