package utopia.genesis.handling.immutable

import utopia.genesis.handling
import utopia.genesis.handling.KeyTypedListener
import utopia.inception.handling.immutable.{Handleable, Handler}

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
object KeyTypedHandler
{
	val empty = new KeyStateHandler(Vector())
	
	def apply(elements: IterableOnce[KeyTypedListener], parent: Option[Handleable] = None) = new KeyTypedHandler(elements)
	
	def apply(element: KeyTypedListener) = new KeyTypedHandler(Vector(element))
	
	def apply(first: KeyTypedListener, second: KeyTypedListener, more: KeyTypedListener*) = new KeyTypedHandler(Vector(first, second) ++ more)
}

/**
  * This is an immutable implementation of the KeyTypedHandler trait
  * @param initialElements Elements placed in this handler
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
class KeyTypedHandler(initialElements: IterableOnce[KeyTypedListener])
	extends Handler[KeyTypedListener](initialElements) with handling.KeyTypedHandler with Handleable
