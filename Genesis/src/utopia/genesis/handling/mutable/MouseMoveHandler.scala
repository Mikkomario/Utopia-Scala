package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
object MouseMoveHandler
{
	def apply(elements: IterableOnce[handling.MouseMoveListener] = Vector()) = new MouseMoveHandler(elements)
	
	def apply(element: handling.MouseMoveListener) = new MouseMoveHandler(Vector(element))
	
	def apply(first: handling.MouseMoveListener, second: handling.MouseMoveListener, more: handling.MouseMoveListener*) =
		new MouseMoveHandler(Vector(first, second) ++ more)
}

/**
  * This ia a mutable implementation of the MouseMoveHandler interface
  * @param initialElements The elements initially placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
class MouseMoveHandler(initialElements: IterableOnce[handling.MouseMoveListener])
	extends DeepHandler[handling.MouseMoveListener](initialElements) with handling.MouseMoveHandler
