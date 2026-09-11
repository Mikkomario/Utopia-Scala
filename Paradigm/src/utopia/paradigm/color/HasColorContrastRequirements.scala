package utopia.paradigm.color

/**
 * A common trait for classes which specify color contrast requirements
 * @author Mikko Hilpinen
 * @since 09.09.2026, v1.9
 */
trait HasColorContrastRequirements extends Any
{
	// ABSTRACT ------------------------
	
	/**
	 * @return Applied color contrast requirements
	 */
	def requiredContrast: ColorContrastRequirement
}
