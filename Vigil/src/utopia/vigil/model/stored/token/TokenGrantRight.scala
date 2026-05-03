package utopia.vigil.model.stored.token

import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}
import utopia.vigil.database.access.token.template.right.AccessTokenGrantRight
import utopia.vigil.model.factory.token.TokenGrantRightFactoryWrapper
import utopia.vigil.model.partial.token.TokenGrantRightData

object TokenGrantRight extends StandardStoredFactory[TokenGrantRightData, TokenGrantRight]
{
	// ATTRIBUTES	--------------------
	
	override val dataFactory = TokenGrantRightData
}

/**
  * Represents a token grant right that has already been stored in the database. 
  * Used for allowing certain token types (templates) to generate new tokens of other types
  * @param id   ID of this token grant right in the database
  * @param data Wrapped token grant right data
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenGrantRight(id: Int, data: TokenGrantRightData) 
	extends StoredModelConvertible[TokenGrantRightData] with FromIdFactory[Int, TokenGrantRight] 
		with TokenGrantRightFactoryWrapper[TokenGrantRightData, TokenGrantRight]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this token grant right in the database
	  */
	def access = AccessTokenGrantRight(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: TokenGrantRightData) = copy(data = data)
}

