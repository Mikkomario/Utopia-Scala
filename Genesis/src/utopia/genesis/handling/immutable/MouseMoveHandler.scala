package utopia.genesis.handling.immutable

import utopia.genesis.handling
import utopia.genesis.handling.MouseMoveListener
import utopia.inception.handling.immutable.{Handleable, Handler}

object MouseMoveHandler
{
	/**
	  * An empty key state handler
	  */
	val empty = new MouseMoveHandler(Vector())
	
	/**
	  * @param elements Elements for this handler
	  * @param parent Handleable this handler is dependent from (default = None = independent)
	  * @return A new handler
	  */
	def apply(elements: TraversableOnce[MouseMoveListener], parent: Option[Handleable] = None) = new MouseMoveHandler(elements)
	
	/**
	  * @param element an element for this handler
	  * @return A new handler
	  */
	def apply(element: MouseMoveListener) = new MouseMoveHandler(Vector(element))
	
	/**
	  * @return A handler with all of the provided elements
	  */
	def apply(first: MouseMoveListener, second: MouseMoveListener, more: MouseMoveListener*): MouseMoveHandler = apply(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the MouseMoveHandler trait
  * @param initialElements Listerners placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class MouseMoveHandler(initialElements: TraversableOnce[MouseMoveListener])
	extends Handler[MouseMoveListener](initialElements) with handling.MouseMoveHandler with Handleable
