package utopia.vault.nosql.template

import utopia.vault.model.template.Deprecates
import utopia.vault.sql.Condition

/**
  * A common trait for factories that deal with deprecating items (deprecating, in this case means that the rows
  * should rarely be included in basic searches)
  * @author Mikko Hilpinen
  * @since 11.1.2020, v1.4
  */
@deprecated("Replaced with Deprecates", "v2.0")
trait Deprecatable extends Deprecates
{
	// ABSTRACT	---------------------
	
	/**
	  * @return A condition that determines whether a row is deprecated
	  */
	@deprecated("Please use .activeCondition instead", "v2.0")
	def nonDeprecatedCondition: Condition
	
	
	// IMPLEMENTED  -----------------
	
	override def activeCondition: Condition = nonDeprecatedCondition
}
