package utopia.vault.nosql.access.template.model

import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Common trait for access points that use model factories that utilize deprecation
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
@deprecated("Replaced with NonDeprecatableView", "v1.8")
trait NonDeprecatedAccess[+M, +A, +V] extends ModelAccess[M, A, V]
{
	// ABSTRACT	------------------------
	
	// Non-deprecated access points are more restricted on factories
	override def factory: FromResultFactory[M] with Deprecatable
	
	
	// IMPLEMENTED	-------------------
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
}
