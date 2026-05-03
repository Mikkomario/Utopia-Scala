package utopia.vigil.database.access.token.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.sql.Condition
import utopia.vigil.database.access.scope.right.FilterScopeRights
import utopia.vigil.database.storable.token.TokenScopeDbModel

/**
  * Common trait for access points which may be filtered based on token scope properties
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait FilterTokenScopes[+Repr] extends FilterScopeRights[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines token scope database properties
	  */
	def model = TokenScopeDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param tokenId token id to target
	  * @return Copy of this access point that only includes token scopes with the specified token id
	  */
	def ofToken(tokenId: Int) = filter(model.tokenId.column <=> tokenId)
	
	/**
	  * @param tokenIds Targeted token ids
	  * @return Copy of this access point that only includes token scopes where token id is within the 
	  * specified value set
	  */
	def ofTokens(tokenIds: IterableOnce[Int]) = filter(Condition.indexIn(model.tokenId, tokenIds))
}

