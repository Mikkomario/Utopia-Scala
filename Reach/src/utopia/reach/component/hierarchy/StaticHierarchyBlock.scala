package utopia.reach.component.hierarchy

import utopia.firmament.model.CoordinateTransform
import utopia.reach.component.template.ReachComponentLike

/**
  * A component hierarchy block that doesn't mutate its state. Used with static parent-child component connections
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class StaticHierarchyBlock(parentComponent: ReachComponentLike) extends ComponentHierarchy
{
	// ATTRIBUTES	-----------------------------
	
	private lazy val parentHierarchy = parentComponent.parentHierarchy
	
	override lazy val parent = Right(parentHierarchy -> parentComponent)
	override lazy val top = super.top
	
	
	// IMPLEMENTED	-----------------------------
	
	override def isThisLevelLinked = true
	
	override def linkPointer = parentHierarchy.linkPointer
	
	override def coordinateTransform: Option[CoordinateTransform] = None
}
