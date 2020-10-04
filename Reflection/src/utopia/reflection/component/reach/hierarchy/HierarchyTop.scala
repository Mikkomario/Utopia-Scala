package utopia.reflection.component.reach.hierarchy

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.reflection.container.reach.ReachCanvas

/**
  * Top of the reach component hierarchy
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
class HierarchyTop(val canvas: ReachCanvas)
{
	// ATTRIBUTES	-----------------------
	
	private val _attachedPointer = new PointerWithEvents(false)
	
	
	// INITIAL CODE	-----------------------
	
	canvas.addStackHierarchyChangeListener(isAttached => _attachedPointer.value = isAttached, callIfAttached = true)
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @return A pointer that shows whether the canvas is currently attached to the higher hierarchy
	  */
	def attachedPointer = _attachedPointer.view
}
