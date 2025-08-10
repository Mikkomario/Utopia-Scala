package utopia.vault.nosql.view

import utopia.vault.model.template.DeprecatesAfterDefined

/**
  * Common trait for access points that target items that can be deprecated by specifying a non-null timestamp
  * @author Mikko Hilpinen
  * @since 3.4.2023, v1.15.1
  */
trait NullDeprecatableView[+Sub] extends TimeDeprecatableView[Sub]
{
	// ABSTRACT -----------------------
	
	override protected def model: DeprecatesAfterDefined
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return Access to deprecated (historical) items
	  */
	def deprecated = filter(model.deprecatedCondition)
}
