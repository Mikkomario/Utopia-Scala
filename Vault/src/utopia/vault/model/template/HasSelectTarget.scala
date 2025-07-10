package utopia.vault.model.template

import utopia.vault.model.enumeration.SelectTarget

/**
 * Common trait for classes which can specify a data select target
 *
 * @author Mikko Hilpinen
 * @since 09.07.2025, v1.22
 */
trait HasSelectTarget extends HasTarget
{
	// ABSTRACT --------------------------
	
	/**
	 * @return Targeted selected data
	 */
	def selectTarget: SelectTarget
	
	
	// COMPUTED --------------------------
	
	/**
	 * @return Select statement used for selecting the targeted data
	 */
	def toSelect = selectTarget.toSelect(target)
}
