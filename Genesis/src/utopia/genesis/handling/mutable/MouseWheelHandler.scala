package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler


object MouseWheelHandler
{
	def apply(elements: TraversableOnce[handling.MouseWheelListener] = Vector()) = new MouseWheelHandler(elements)
	
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
class MouseWheelHandler(initialElements: TraversableOnce[handling.MouseWheelListener])
	extends DeepHandler[handling.MouseWheelListener](initialElements) with handling.MouseWheelHandler
