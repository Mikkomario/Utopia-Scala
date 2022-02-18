package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleToken
import utopia.exodus.model.partial.auth.TokenData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a token that has already been stored in the database
  * @param id id of this token in the database
  * @param data Wrapped token data
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class Token(id: Int, data: TokenData) extends StoredModelConvertible[TokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token in the database
	  */
	def access = DbSingleToken(id)
}

