package utopia.reflection.component.reach.hierarchy

import utopia.flow.event.Changing
import utopia.reflection.component.reach.template.ReachComponentLike

/**
  * Used for representing individual components within a component hierarchy
  * @author Mikko Hilpinen
  * @since 3.10.2020, v2
  */
trait HierarchyBlock
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return Component represented by this block
	  */
	def component: ReachComponentLike
	
	/**
	  * @return A pointer that shows whether this part / block is currently active and linked to the upper hierarchy.
	  *         None if this block is always linked.
	  */
	def linkPointer: Option[Changing[Boolean]]
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return Whether this block is currently considered to be linked to other hierarchy blocks
	  */
	def isLinked = linkPointer.forall { _.value }
}

object HierarchyBlock
{
	// OTHER	-----------------------
	
	/**
	  * @param component Wrapped component
	  * @return A static hierarchy block
	  */
	def static(component: ReachComponentLike) = new StaticHierarchyBlock(component)
	
	/**
	  * @param component Wrapped component
	  * @param linkPointer A pointer to the link state (whether connected to upper hierarchy) of this component
	  * @return A new hierarchy block
	  */
	def switching(component: ReachComponentLike, linkPointer: Changing[Boolean]) =
		new SwitchingHierarchyBlock(component, linkPointer)
	
	
	// NESTED	-----------------------
	
	/**
	  * A hierarchy block that is always active / linked
	  * @param component Wrapped component
	  */
	class StaticHierarchyBlock(override val component: ReachComponentLike) extends HierarchyBlock
	{
		override def linkPointer = None
	}
	
	/**
	  * A hierarchy block that changes state
	  * @param component Wrapped component
	  * @param linkPointer Pointer to the
	  */
	class SwitchingHierarchyBlock(override val component: ReachComponentLike, linkPointer: Changing[Boolean])
		extends HierarchyBlock
	{
		override def linkPointer = Some(linkPointer)
	}
}
