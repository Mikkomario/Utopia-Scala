package utopia.reflection.component.reach

import utopia.reflection.component.template.layout.stack.Stackable2

/**
  * A common trait for "Reach" (no-swing) style components
  * @author Mikko Hilpinen
  * @since 3.10.2020, v2
  */
trait ReachComponentLike extends Stackable2
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Hierarchy containing all this component's parents
	  */
	protected def parentHierarchy: ComponentHierarchy
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Window that contains this component
	  */
	def parentWindow = parentHierarchy.parentWindow
	
	/**
	  * @return The absolute (on-screen) position of this component. None if not connected to main component
	  *         hierarchy
	  */
	def absolutePosition = parentHierarchy.absolutePositionModifier.map { position + _ }
	
	/**
	  * @return The position of this component inside the so called top component
	  */
	def positionInTop = position + parentHierarchy.positionToTopModifier
	
	
	// OTHER	-------------------------
	
	def revalidate() =
	{
		// Resets the cached stack size of this and upper components
		
	}
}
