package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.TokenScopeLinkFactory
import utopia.exodus.model.partial.auth.TokenScopeLinkData
import utopia.exodus.model.stored.auth.TokenScopeLink
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing TokenScopeLinkModel instances and for inserting token scope links to the database
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object TokenScopeLinkModel extends DataInserter[TokenScopeLinkModel, TokenScopeLink, TokenScopeLinkData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains token scope link token id
	  */
	val tokenIdAttName = "tokenId"
	
	/**
	  * Name of the property that contains token scope link scope id
	  */
	val scopeIdAttName = "scopeId"
	
	/**
	  * Name of the property that contains token scope link created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains token scope link token id
	  */
	def tokenIdColumn = table(tokenIdAttName)
	
	/**
	  * Column that contains token scope link scope id
	  */
	def scopeIdColumn = table(scopeIdAttName)
	
	/**
	  * Column that contains token scope link created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = TokenScopeLinkFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: TokenScopeLinkData) = 
		apply(None, Some(data.tokenId), Some(data.scopeId), Some(data.created))
	
	override def complete(id: Value, data: TokenScopeLinkData) = TokenScopeLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this token scope link was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A token scope link id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param scopeId Id of the enabled scope
	  * @return A model containing only the specified scope id
	  */
	def withScopeId(scopeId: Int) = apply(scopeId = Some(scopeId))
	
	/**
	  * @param tokenId Id of the linked token
	  * @return A model containing only the specified token id
	  */
	def withTokenId(tokenId: Int) = apply(tokenId = Some(tokenId))
}

/**
  * Used for interacting with TokenScopeLinks in the database
  * @param id token scope link database id
  * @param tokenId Id of the linked token
  * @param scopeId Id of the enabled scope
  * @param created Time when this token scope link was first created
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenScopeLinkModel(id: Option[Int] = None, tokenId: Option[Int] = None, 
	scopeId: Option[Int] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[TokenScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def factory = TokenScopeLinkModel.factory
	
	override def valueProperties = {
		import TokenScopeLinkModel._
		Vector("id" -> id, tokenIdAttName -> tokenId, scopeIdAttName -> scopeId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param scopeId A new scope id
	  * @return A new copy of this model with the specified scope id
	  */
	def withScopeId(scopeId: Int) = copy(scopeId = Some(scopeId))
	
	/**
	  * @param tokenId A new token id
	  * @return A new copy of this model with the specified token id
	  */
	def withTokenId(tokenId: Int) = copy(tokenId = Some(tokenId))
}

