package utopia.ambassador.database.model.token

import utopia.ambassador.database.factory.token.AuthTokenScopeLinkFactory
import utopia.ambassador.model.partial.token.AuthTokenScopeLinkData
import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing AuthTokenScopeLinkModel instances and for inserting AuthTokenScopeLinks to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthTokenScopeLinkModel 
	extends DataInserter[AuthTokenScopeLinkModel, AuthTokenScopeLink, AuthTokenScopeLinkData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthTokenScopeLink tokenId
	  */
	val tokenIdAttName = "tokenId"
	
	/**
	  * Name of the property that contains AuthTokenScopeLink scopeId
	  */
	val scopeIdAttName = "scopeId"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthTokenScopeLink tokenId
	  */
	def tokenIdColumn = table(tokenIdAttName)
	
	/**
	  * Column that contains AuthTokenScopeLink scopeId
	  */
	def scopeIdColumn = table(scopeIdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthTokenScopeLinkFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthTokenScopeLinkData) = apply(None, Some(data.tokenId), Some(data.scopeId))
	
	override def complete(id: Value, data: AuthTokenScopeLinkData) = AuthTokenScopeLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param id A AuthTokenScopeLink id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param scopeId Id of the scope that is accessible by using the linked token
	  * @return A model containing only the specified scopeId
	  */
	def withScopeId(scopeId: Int) = apply(scopeId = Some(scopeId))
	
	/**
	  * @param tokenId Id of the token that provides access to the linked scope
	  * @return A model containing only the specified tokenId
	  */
	def withTokenId(tokenId: Int) = apply(tokenId = Some(tokenId))
}

/**
  * Used for interacting with AuthTokenScopeLinks in the database
  * @param id AuthTokenScopeLink database id
  * @param tokenId Id of the token that provides access to the linked scope
  * @param scopeId Id of the scope that is accessible by using the linked token
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthTokenScopeLinkModel(id: Option[Int] = None, tokenId: Option[Int] = None, 
	scopeId: Option[Int] = None) 
	extends StorableWithFactory[AuthTokenScopeLink]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenScopeLinkModel.factory
	
	override def valueProperties = 
	{
		import AuthTokenScopeLinkModel._
		Vector("id" -> id, tokenIdAttName -> tokenId, scopeIdAttName -> scopeId)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param scopeId A new scopeId
	  * @return A new copy of this model with the specified scopeId
	  */
	def withScopeId(scopeId: Int) = copy(scopeId = Some(scopeId))
	
	/**
	  * @param tokenId A new tokenId
	  * @return A new copy of this model with the specified tokenId
	  */
	def withTokenId(tokenId: Int) = copy(tokenId = Some(tokenId))
}

