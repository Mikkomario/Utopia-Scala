package utopia.reflection.util

import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.Positive
import utopia.reflection.component.reach.template.Focusable
import utopia.reflection.component.template.layout.Area

/**
  * This object manages focus traversal inside a reach canvas
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
class ReachFocusManager
{
	// ATTRIBUTES	-----------------------------
	
	private var targets = Set[Focusable with Area]()
	private var focusOwner: Option[Focusable with Area] = None
	
	
	// COMPUTED	---------------------------------
	
	def hasFocus = focusOwner.nonEmpty
	
	/* TODO: Create focus traversal logic (left to right, up to down (except when up to down is closer))
	private def nextFocusTargets = focusOwner match
	{
		case Some(current) =>
			val currentBounds = current.bounds
			(targets - current).iterator.filter { other =>
				val otherPosition = other.position
				if (otherPosition == currentBounds.position)
					other.size.area < currentBounds.area
				else
					otherPosition.x >= currentBounds.x && otherPosition.y >= currentBounds.y
			}
		case None =>
	}*/
	
	
	// OTHER	---------------------------------
	
	def register(component: Focusable with Area) = targets += component
	// TODO: Move focus if focus owner
	def unregister(component: Focusable with Area) = targets -= component
	
	def isFocusOwner(component: Any) = focusOwner.contains(component)
	
	def moveFocusInside(direction: Direction1D = Positive, allowLooping: Boolean = true) =
	{
		// TODO: Continue
	}
}
