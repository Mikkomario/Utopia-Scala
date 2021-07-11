package utopia.vault.nosql.view

import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * A common trait for views that show all non-deprecated items within a target
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait NonDeprecatedView[+A] extends FactoryView[A]
{
	// ABSTRACT  ------------------------------
	
	// Factory must be deprecatable
	override def factory: FromResultFactory[A] with Deprecatable
	
	
	// IMPLEMENTED  ---------------------------
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
}
