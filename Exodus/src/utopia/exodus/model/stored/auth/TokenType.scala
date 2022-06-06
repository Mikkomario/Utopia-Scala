package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleTokenType
import utopia.exodus.model.partial.auth.TokenTypeData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a token type that has already been stored in the database
  * @param id id of this token type in the database
  * @param data Wrapped token type data
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenType(id: Int, data: TokenTypeData) extends StoredModelConvertible[TokenTypeData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token type in the database
	  */
	def access = DbSingleTokenType(id)
}

