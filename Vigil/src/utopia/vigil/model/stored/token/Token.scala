package utopia.vigil.model.stored.token

import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.database.access.token.AccessToken
import utopia.vigil.model.factory.token.TokenFactoryWrapper
import utopia.vigil.model.partial.token.TokenData

object Token extends StandardStoredFactory[TokenData, Token]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = TokenData
}

/**
  * Represents a token that has already been stored in the database. 
  * Represents a token that may be used for authorizing certain actions
  * @param id   ID of this token in the database
  * @param data Wrapped token data
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class Token(id: Int, data: TokenData) 
	extends StoredModelConvertible[TokenData] with FromIdFactory[Int, Token] 
		with TokenFactoryWrapper[TokenData, Token]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token in the database
	  */
	def access = AccessToken(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: TokenData) = copy(data = data)
}

