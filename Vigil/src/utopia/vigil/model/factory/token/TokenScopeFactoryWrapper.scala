package utopia.vigil.model.factory.token

import utopia.vigil.model.factory.scope.ScopeRightFactoryWrapper

/**
  * Common trait for classes that implement TokenScopeFactory by wrapping a TokenScopeFactory 
  * instance
  * @tparam A Type of constructed instances
  * @tparam Repr Implementing type of this factory
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenScopeFactoryWrapper[A <: TokenScopeFactory[A], +Repr] 
	extends TokenScopeFactory[Repr] with ScopeRightFactoryWrapper[A, Repr]
{
	// IMPLEMENTED	--------------------
	
	override def withTokenId(tokenId: Int) = mapWrapped { _.withTokenId(tokenId) }
}

