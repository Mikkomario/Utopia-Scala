package utopia.genesis.handling.immutable

import utopia.genesis.handling
import utopia.genesis.handling.MouseWheelListener
import utopia.inception.handling.immutable.{Handleable, Handler}

object MouseWheelHandler
{
	/**
	  * An empty key state handler
	  */
	val empty = new MouseWheelHandler(Vector())
	
	/**
	  * @param elements Elements for this handler
	  * @param parent Handleable this handler is dependent from (default = None = independent)
	  * @return A new handler
	  */
	def apply(elements: TraversableOnce[MouseWheelListener], parent: Option[Handleable] = None) = new MouseWheelHandler(elements)
	
	/**
	  * @param element an element for this handler
	  * @return A new handler
	  */
	def apply(element: MouseWheelListener) = new MouseWheelHandler(Vector(element))
	
	/**
	  * @return A handler with all of the provided elements
	  */
	def apply(first: MouseWheelListener, second: MouseWheelListener, more: MouseWheelListener*): MouseWheelHandler = apply(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the MouseWheelHandler trait
  * @param initialElements Listerners placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class MouseWheelHandler(initialElements: TraversableOnce[MouseWheelListener])
	extends Handler[MouseWheelListener](initialElements) with handling.MouseWheelHandler with Handleable