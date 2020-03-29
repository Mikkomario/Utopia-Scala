package utopia.genesis.handling.immutable

import utopia.genesis.handling
import utopia.genesis.handling.KeyStateListener
import utopia.inception.handling.immutable.{Handleable, Handler}

object KeyStateHandler
{
	/**
	  * An empty key state handler
	  */
	val empty = new KeyStateHandler(Vector())
	
	/**
	  * @param elements Elements for this handler
	  * @param parent Handleable this handler is dependent from (default = None = independent)
	  * @return A new handler
	  */
	def apply(elements: TraversableOnce[KeyStateListener], parent: Option[Handleable] = None) = new KeyStateHandler(elements)
	
	/**
	  * @param element an element for this handler
	  * @return A new handler
	  */
	def apply(element: KeyStateListener) = new KeyStateHandler(Vector(element))
	
	/**
	  * @return A handler with all of the provided elements
	  */
	def apply(first: KeyStateListener, second: KeyStateListener, more: KeyStateListener*): KeyStateHandler = apply(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the KeyStateHandler trait
  * @param initialElements Listerners placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
class KeyStateHandler(initialElements: TraversableOnce[KeyStateListener])
	extends Handler[KeyStateListener](initialElements) with handling.KeyStateHandler with Handleable
