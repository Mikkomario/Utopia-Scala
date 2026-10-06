package utopia.vigil.model.stored.token

import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.database.access.token.AccessToken
import utopia.vigil.model.factory.token.TokenFactoryWrapper
import utopia.vigil.model.partial.token.TokenData

object Token extends StandardStoredFactory[TokenData, Token]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = TokenData
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new token
	  * @param id   ID of this token in the database
	  * @param data Wrapped token data
	  * @return token with the specified id and wrapped data
	  */
	def apply(id: Int, data: TokenData): Token = _Token(id, data)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the token trait
	  * @param id   ID of this token in the database
	  * @param data Wrapped token data
	  * @author Mikko Hilpinen
	  * @since 04.05.2026
	  */
	private case class _Token(id: Int, data: TokenData) extends Token
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(data: TokenData) = copy(data = data)
	}
}

/**
  * Represents a token that has already been stored in the database. 
  * Represents a token that may be used for authorizing certain actions
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
trait Token 
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
	
	
	// OTHER    ------------------------
	
	/**
	 * Converts this token to a client-readable model.
	 * Assumes that this token is newly created.
	 * @param key A string representation of this token
	 * @return A model to send to the client
	 */
	def toClientModel(key: String) = Model
		.from("id" -> id, "key" -> key, "name" -> data.name, "created" -> data.created, "expires" -> data.expires)
		.withoutEmptyValues
}

