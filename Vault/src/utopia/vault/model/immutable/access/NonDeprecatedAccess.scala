package utopia.vault.model.immutable.access

import utopia.vault.nosql.factory.{Deprecatable, FromResultFactory}

/**
 * A common trait for accessors that target non-deprecated items
 * @author Mikko Hilpinen
 * @since 11.1.2020, v1.4
 */
@deprecated("Replaced with utopia.vault.nosql.access.NonDeprecatedAccess", "v1.4")
trait NonDeprecatedAccess[+A] extends ConditionalAccess[A]
{
	// ABSTRACT	---------------------
	
	override def factory: FromResultFactory[A] with Deprecatable
	
	
	// IMPLEMENTED	-----------------
	
	override def condition = factory.nonDeprecatedCondition
}
