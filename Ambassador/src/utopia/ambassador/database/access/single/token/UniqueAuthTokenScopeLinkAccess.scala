package utopia.ambassador.database.access.single.token

import utopia.ambassador.database.factory.token.AuthTokenScopeLinkFactory
import utopia.ambassador.database.model.token.AuthTokenScopeLinkModel
import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthTokenScopeLinks.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthTokenScopeLinkAccess 
	extends SingleRowModelAccess[AuthTokenScopeLink] 
		with DistinctModelAccess[AuthTokenScopeLink, Option[AuthTokenScopeLink], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the token that provides access to the linked scope. None if no instance (or value) was found.
	  */
	def tokenId(implicit connection: Connection) = pullColumn(model.tokenIdColumn).int
	
	/**
	  * Id of the scope that is accessible by using the linked token. None if no instance (or value) was found.
	  */
	def scopeId(implicit connection: Connection) = pullColumn(model.scopeIdColumn).int
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthTokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the scopeId of the targeted AuthTokenScopeLink instance(s)
	  * @param newScopeId A new scopeId to assign
	  * @return Whether any AuthTokenScopeLink instance was affected
	  */
	def scopeId_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn, 
		newScopeId)
	
	/**
	  * Updates the tokenId of the targeted AuthTokenScopeLink instance(s)
	  * @param newTokenId A new tokenId to assign
	  * @return Whether any AuthTokenScopeLink instance was affected
	  */
	def tokenId_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn, 
		newTokenId)
}

