package utopia.vigil.database.reader.token

import utopia.vault.nosql.read.linked.MultiLinkedDbReader
import utopia.vigil.model.combined.token.ScopedToken
import utopia.vigil.model.stored.token.{Token, TokenScope}

/**
  * Used for reading scoped tokens from the database
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
object ScopedTokenDbReader 
	extends MultiLinkedDbReader[Token, TokenScope, ScopedToken](TokenDbReader, TokenScopeDbReader, 
		neverEmptyRight = false)
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param token      token to wrap
	  * @param scopeLinks scope links to attach
	  */
	override def combine(token: Token, scopeLinks: Seq[TokenScope]) = ScopedToken(token, scopeLinks)
}

