package utopia.exodus.database.access.single.auth

import utopia.exodus.database.access.many.auth.DbTokenScopeLinks
import utopia.exodus.model.stored.auth.Token
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual tokens, based on their id
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class DbSingleToken(id: Int) extends UniqueTokenAccess with SingleIntIdModelAccess[Token]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to an email validation attempt using this token
	  */
	def emailValidationAttempt = DbEmailValidationAttempt.usingTokenWithId(id)
	
	/**
	  * @return An access point to this token's scope links
	  */
	def scopeLinks = DbTokenScopeLinks.withTokenId(id)
}

