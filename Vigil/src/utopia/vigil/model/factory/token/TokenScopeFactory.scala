package utopia.vigil.model.factory.token

import utopia.vigil.model.factory.scope.ScopeRightFactory

/**
  * Common trait for token scope-related factories which allow construction with individual 
  * properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait TokenScopeFactory[+A] extends ScopeRightFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param tokenId New token id to assign
	  * @return Copy of this item with the specified token id
	  */
	def withTokenId(tokenId: Int): A
}

