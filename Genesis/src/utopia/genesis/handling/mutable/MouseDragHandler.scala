package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.inception.handling.mutable.DeepHandler

// TODO: WET WET - This handler system needs a refactoring anyway
object MouseDragHandler
{
	def apply(elements: IterableOnce[handling.MouseDragListener] = Vector()) = new MouseDragHandler(elements)
	
	def apply(element: handling.MouseDragListener) = new MouseDragHandler(Vector(element))
	
	def apply(first: handling.MouseDragListener, second: handling.MouseDragListener, more: handling.MouseDragListener*) =
		new MouseDragHandler(Vector(first, second) ++ more)
}

/**
  * A mutable implementation of the MouseDragHandler interface
  * @param initialElements The elements initially placed in this handler
  * @author Mikko Hilpinen
  * @since 20.2.2023
  */
class MouseDragHandler(initialElements: IterableOnce[handling.MouseDragListener])
	extends DeepHandler[handling.MouseDragListener](initialElements) with handling.MouseDragHandler
