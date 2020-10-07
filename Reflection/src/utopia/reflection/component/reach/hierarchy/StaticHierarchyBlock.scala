package utopia.reflection.component.reach.hierarchy

import utopia.reflection.component.reach.template.ReachComponentLike

/**
  * A component hierarchy block that doesn't mutate its state. Used with static parent-child component connections
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class StaticHierarchyBlock(parentComponent: ReachComponentLike) extends ComponentHierarchy
{
	// ATTRIBUTES	-----------------------------
	
	private lazy val parentHierarchy = parentComponent.parentHierarchy
	
	override lazy val parent = Right(parentHierarchy -> parentComponent)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def isThisLevelLinked = true
	
	override def linkPointer = parentHierarchy.linkPointer
}
