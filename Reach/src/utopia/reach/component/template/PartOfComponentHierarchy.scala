package utopia.reach.component.template

import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.container.ReachCanvas

/**
  * Common trait for components and other elements that specify a parent component hierarchy.
  * @author Mikko Hilpinen
  * @since 08/01/2024, v1.2
  */
trait PartOfComponentHierarchy
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The component hierarchy this component is part of.
	  *         Returns the hierarchy block that matches the direct parent of this component.
	  */
	def parentHierarchy: ComponentHierarchy
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The Reach Canvas -element to which this component belongs
	  */
	implicit def canvas: ReachCanvas = parentHierarchy.top
}
