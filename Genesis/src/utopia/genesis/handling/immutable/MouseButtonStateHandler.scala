package utopia.genesis.handling.immutable

import utopia.genesis.handling
import utopia.genesis.handling.MouseButtonStateListener
import utopia.inception.handling.immutable.{Handleable, Handler}

object MouseButtonStateHandler
{
	/**
	  * An empty key state handler
	  */
	val empty = new MouseButtonStateHandler(Vector())
	
	/**
	  * @param elements Elements for this handler
	  * @param parent Handleable this handler is dependent from (default = None = independent)
	  * @return A new handler
	  */
	def apply(elements: TraversableOnce[MouseButtonStateListener], parent: Option[Handleable] = None) = new MouseButtonStateHandler(elements)
	
	/**
	  * @param element an element for this handler
	  * @return A new handler
	  */
	def apply(element: MouseButtonStateListener) = new MouseButtonStateHandler(Vector(element))
	
	/**
	  * @return A handler with all of the provided elements
	  */
	def apply(first: MouseButtonStateListener, second: MouseButtonStateListener, more: MouseButtonStateListener*): MouseButtonStateHandler = apply(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the MouseButtonStateHandler trait
  * @param initialElements Listerners placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class MouseButtonStateHandler(initialElements: TraversableOnce[MouseButtonStateListener])
	extends Handler[MouseButtonStateListener](initialElements) with handling.MouseButtonStateHandler with Handleable
