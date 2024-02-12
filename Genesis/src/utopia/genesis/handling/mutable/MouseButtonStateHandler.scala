package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
object MouseButtonStateHandler
{
	def apply(elements: IterableOnce[handling.MouseButtonStateListener] = Vector()) = new MouseButtonStateHandler(elements)
	
	def apply(element: handling.MouseButtonStateListener) = new MouseButtonStateHandler(Vector(element))
	
	def apply(first: handling.MouseButtonStateListener, second: handling.MouseButtonStateListener,
			  more: handling.MouseButtonStateListener*) = new MouseButtonStateHandler(Vector(first, second) ++ more)
}

/**
  * This ia a mutable implementation of the MouseButtonStateHandler interface
  * @param initialElements The elements initially placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
class MouseButtonStateHandler(initialElements: IterableOnce[handling.MouseButtonStateListener])
	extends DeepHandler[handling.MouseButtonStateListener](initialElements) with handling.MouseButtonStateHandler
