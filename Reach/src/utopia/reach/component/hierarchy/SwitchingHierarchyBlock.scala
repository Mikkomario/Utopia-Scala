package utopia.reach.component.hierarchy

import utopia.firmament.model.CoordinateTransform
import utopia.flow.view.template.eventful.Flag
import utopia.reach.component.template.ReachComponentLike

/**
  * A component hierarchy block used in situations where a static parent-child connection switches on and off,
  * for example, when the parent component switches the active component(s) inside it.
  * @author Mikko Hilpinen
  * @since 7.10.2020, v0.1
  */
class SwitchingHierarchyBlock(parentComponent: ReachComponentLike, switchPointer: Flag)
	extends ComponentHierarchy
{
	// ATTRIBUTES	---------------------------
	
	override lazy val parent = Right(parentComponent.parentHierarchy -> parentComponent)
	override lazy val top = super.top
	
	override lazy val linkPointer = parentComponent.parentHierarchy.linkPointer && switchPointer
	
	
	// IMPLEMENTED	---------------------------
	
	override def isThisLevelLinked = switchPointer.value
	
	override def coordinateTransform: Option[CoordinateTransform] = None
}
