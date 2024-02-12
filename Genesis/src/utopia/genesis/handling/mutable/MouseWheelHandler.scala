package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler

@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
object MouseWheelHandler
{
	def apply(elements: IterableOnce[handling.MouseWheelListener] = Vector()) = new MouseWheelHandler(elements)
	
	def apply(element: handling.MouseWheelListener) = new MouseWheelHandler(Vector(element))
	
	def apply(first: handling.MouseWheelListener, second: handling.MouseWheelListener, more: handling.MouseWheelListener*) =
		new MouseWheelHandler(Vector(first, second) ++ more)
}

/**
  * This ia a mutable implementation of the MouseWheelHandler interface
  * @param initialElements The elements initially placed in this handler
  * @author Mikko Hilpinen
  * @since 6.4.2019, v2+
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
class MouseWheelHandler(initialElements: IterableOnce[handling.MouseWheelListener])
	extends DeepHandler[handling.MouseWheelListener](initialElements) with handling.MouseWheelHandler
