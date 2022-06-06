package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.TokenTypeFactory
import utopia.exodus.database.model.auth.TokenTypeModel
import utopia.exodus.model.stored.auth.TokenType
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual token types
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbTokenType extends SingleRowModelAccess[TokenType] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenTypeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenTypeFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted token type
	  * @return An access point to that token type
	  */
	def apply(id: Int) = DbSingleTokenType(id)
}

