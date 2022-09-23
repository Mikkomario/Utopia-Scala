package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.TokenScopeLinkFactory
import utopia.exodus.model.partial.auth.TokenScopeLinkData
import utopia.exodus.model.stored.auth.TokenScopeLink
import utopia.flow.collection.value.typeless.Value
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
	
	/**
	  * Name of the property that contains token scope link is directly accessible
	  */
	val isDirectlyAccessibleAttName = "isDirectlyAccessible"
	
	/**
	  * Name of the property that contains token scope link grants forward
	  */
	val grantsForwardAttName = "grantsForward"
	
	
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
	  * Column that contains token scope link is directly accessible
	  */
	def isDirectlyAccessibleColumn = table(isDirectlyAccessibleAttName)
	
	/**
	  * Column that contains token scope link grants forward
	  */
	def grantsForwardColumn = table(grantsForwardAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = TokenScopeLinkFactory
	
	/**
	  * @return A model where 'isDirectlyAccessible' is set to true
	  */
	def directlyAccessible = withIsDirectlyAccessible(true)
	/**
	  * @return A model where 'grantsForward' is set to true
	  */
	def grantedForward = withGrantsForward(true)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: TokenScopeLinkData) = 
		apply(None, Some(data.tokenId), Some(data.scopeId), Some(data.created), 
			Some(data.isDirectlyAccessible), Some(data.grantsForward))
	
	override def complete(id: Value, data: TokenScopeLinkData) = TokenScopeLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this token scope link was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param grantsForward Whether this scope is granted to tokens that are created using this token
	  * @return A model containing only the specified grants forward
	  */
	def withGrantsForward(grantsForward: Boolean) = apply(grantsForward = Some(grantsForward))
	
	/**
	  * @param id A token scope link id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param isDirectlyAccessible Whether the linked scope is directly accessible using the linked token
	  * @return A model containing only the specified is directly accessible
	  */
	def withIsDirectlyAccessible(isDirectlyAccessible: Boolean) = 
		apply(isDirectlyAccessible = Some(isDirectlyAccessible))
	
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
  * @param isDirectlyAccessible Whether the linked scope is directly accessible using the linked token
  * @param grantsForward Whether this scope is granted to tokens that are created using this token
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class TokenScopeLinkModel(id: Option[Int] = None, tokenId: Option[Int] = None, 
	scopeId: Option[Int] = None, created: Option[Instant] = None, 
	isDirectlyAccessible: Option[Boolean] = None, grantsForward: Option[Boolean] = None) 
	extends StorableWithFactory[TokenScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def factory = TokenScopeLinkModel.factory
	
	override def valueProperties = {
		import TokenScopeLinkModel._
		Vector("id" -> id, tokenIdAttName -> tokenId, scopeIdAttName -> scopeId, createdAttName -> created, 
			isDirectlyAccessibleAttName -> isDirectlyAccessible, grantsForwardAttName -> grantsForward)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param grantsForward A new grants forward
	  * @return A new copy of this model with the specified grants forward
	  */
	def withGrantsForward(grantsForward: Boolean) = copy(grantsForward = Some(grantsForward))
	
	/**
	  * @param isDirectlyAccessible A new is directly accessible
	  * @return A new copy of this model with the specified is directly accessible
	  */
	def withIsDirectlyAccessible(isDirectlyAccessible: Boolean) = 
		copy(isDirectlyAccessible = Some(isDirectlyAccessible))
	
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

