package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler

object KeyTypedHandler
{
	def apply(elements: TraversableOnce[handling.KeyTypedListener] = Vector()) = new KeyTypedHandler(elements)
	
	def apply(element: handling.KeyTypedListener) = new KeyTypedHandler(Vector(element))
	
	def apply(first: handling.KeyTypedListener, second: handling.KeyTypedListener, more: handling.KeyTypedListener*) =
		new KeyTypedHandler(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the KeyTypedHandler trait
  * @param initialElements The elements initially placed in this handler
  */
class KeyTypedHandler(initialElements: TraversableOnce[handling.KeyTypedListener])
	extends DeepHandler[handling.KeyTypedListener](initialElements) with handling.KeyTypedHandler
