package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
object KeyStateHandler
{
	def apply(elements: IterableOnce[handling.KeyStateListener] = Vector()) = new KeyStateHandler(elements)
	
	def apply(element: handling.KeyStateListener) = new KeyStateHandler(Vector(element))
	
	def apply(first: handling.KeyStateListener, second: handling.KeyStateListener, more: handling.KeyStateListener*) =
		new KeyStateHandler(Vector(first, second) ++ more)
}

/**
  * This ia a mutable implementation of the key state handler interface
  * @param initialElements The elements initially placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
class KeyStateHandler(initialElements: IterableOnce[handling.KeyStateListener])
	extends DeepHandler[handling.KeyStateListener](initialElements) with handling.KeyStateHandler
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * Filter applied to incoming keyboard state events (mutable)
	  */
	var filter = super.keyStateEventFilter
	
	
	// IMPLEMENTED  ---------------------
	
	override def keyStateEventFilter = filter
}