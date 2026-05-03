package utopia.vigil.model.factory.token

import utopia.vigil.model.factory.scope.ScopeRightFactoryWrapper

/**
  * Common trait for classes that implement TokenTemplateScopeFactory by wrapping a 
  * TokenTemplateScopeFactory instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenTemplateScopeFactoryWrapper[A <: TokenTemplateScopeFactory[A], +Repr] 
	extends TokenTemplateScopeFactory[Repr] with ScopeRightFactoryWrapper[A, Repr]
{
	// IMPLEMENTED	--------------------
	
	override def withTemplateId(templateId: Int) = mapWrapped { _.withTemplateId(templateId) }
}

