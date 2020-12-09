package utopia.reflection.component.reach.hierarchy

import utopia.flow.event.ChangingLike
import utopia.reflection.component.reach.template.ReachComponentLike

/**
  * A component hierarchy block used in situations where a static parent-child connection switches on and off,
  * for example, when the parent component switches the active component(s) inside it.
  * @author Mikko Hilpinen
  * @since 7.10.2020, v2
  */
class SwitchingHierarchyBlock(parentComponent: ReachComponentLike, switchPointer: ChangingLike[Boolean])
	extends ComponentHierarchy
{
	// ATTRIBUTES	---------------------------
	
	override lazy val parent = Right(parentComponent.parentHierarchy -> parentComponent)
	
	override lazy val linkPointer = parentComponent.parentHierarchy.linkPointer.mergeWith(switchPointer) { _ && _ }
	
	
	// IMPLEMENTED	---------------------------
	
	override def isThisLevelLinked = switchPointer.value
}
