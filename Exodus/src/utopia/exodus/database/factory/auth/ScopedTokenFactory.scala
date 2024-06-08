package utopia.exodus.database.factory.auth

import utopia.exodus.model.combined.auth.ScopedToken
import utopia.exodus.model.stored.auth.{Token, TokenScopeLink}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading scoped tokens from the database
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object ScopedTokenFactory extends MultiCombiningFactory[ScopedToken, Token, TokenScopeLink] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def parentFactory = TokenFactory
	override def childFactory = TokenScopeLinkFactory
	
	override def isAlwaysLinked = false
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition
	
	override def apply(token: Token, scopeLink: Seq[TokenScopeLink]) = ScopedToken(token, scopeLink)
}

