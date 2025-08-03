package utopia.vault.nosql.storable.deprecation

import utopia.vault.model.template.{DeprecatesAfter, HasTable}
import utopia.vault.nosql.template.Deprecatable

/**
 * Common trait for deprecatable model factories that use a deprecation time column
 * @author Mikko Hilpinen
 * @since 26.9.2021, v1.10
 */
@deprecated("Please use DeprecatesAfter instead", "v2.0")
trait TimeDeprecatable extends Deprecatable with HasTable with DeprecatesAfter
{
	// ABSTRACT ----------------------------------
	
	/**
	 * @return Name of the property that contains item deprecation time
	 */
	def deprecationAttName: String
	
	
	// IMPLEMENTED  -------------------------------
	
	override def deprecationColumn = table(deprecationAttName)
	override def activeCondition = super[Deprecatable].activeCondition
}
