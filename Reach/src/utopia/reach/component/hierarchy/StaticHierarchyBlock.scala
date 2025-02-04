package utopia.reach.component.hierarchy

import utopia.firmament.model.CoordinateTransform
import utopia.reach.component.template.ReachComponent

/**
  * A component hierarchy block that doesn't mutate its state. Used with static parent-child component connections
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class StaticHierarchyBlock(parentComponent: ReachComponent) extends ComponentHierarchy
{
	// ATTRIBUTES	-----------------------------
	
	private lazy val parentHierarchy = parentComponent.hierarchy
	
	override lazy val parent = Right(parentHierarchy -> parentComponent)
	override lazy val top = super.top
	
	
	// IMPLEMENTED	-----------------------------
	
	override def isThisLevelLinked = true
	
	override def linkedFlag = parentHierarchy.linkedFlag
	
	override def coordinateTransform: Option[CoordinateTransform] = None
}
