package utopia.vault.nosql.access

import utopia.vault.nosql.factory.{Deprecatable, FromResultFactory}

/**
 * Common trait for access points that use model factories that utilize deprecation
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait NonDeprecatedAccess[+M, +A] extends ModelAccess[M, A]
{
	// ABSTRACT	------------------------
	
	// Non-deprecated access points are more restricted on factories
	override def factory: FromResultFactory[M] with Deprecatable
	
	
	// IMPLEMENTED	-------------------
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
}
