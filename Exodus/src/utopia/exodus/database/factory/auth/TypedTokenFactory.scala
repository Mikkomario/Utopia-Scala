package utopia.exodus.database.factory.auth

import utopia.exodus.model.combined.auth.TypedToken
import utopia.exodus.model.stored.auth.{Token, TokenType}
import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading typed tokens from the database
  * @author Mikko Hilpinen
  * @since 19.02.2022, v4.0
  */
object TypedTokenFactory extends CombiningFactory[TypedToken, Token, TokenType] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = TokenTypeFactory
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition
	
	override def parentFactory = TokenFactory
	
	override def apply(token: Token, tokenType: TokenType) = TypedToken(token, tokenType)
}

