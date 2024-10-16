package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.access.many.scope.DbAuthTokenScopes
import utopia.ambassador.database.factory.token.AuthTokenScopeLinkFactory
import utopia.ambassador.database.model.token.AuthTokenScopeLinkModel
import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

object ManyAuthTokenScopeLinksAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyAuthTokenScopeLinksAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyAuthTokenScopeLinksAccess
}

/**
  * A common trait for access points which target multiple AuthTokenScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyAuthTokenScopeLinksAccess 
	extends ManyRowModelAccess[AuthTokenScopeLink] with Indexed 
		with FilterableView[ManyAuthTokenScopeLinksAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * tokenIds of the accessible AuthTokenScopeLinks
	  */
	def tokenIds(implicit connection: Connection) = pullColumn(model.tokenIdColumn)
		.flatMap { value => value.int }
	
	/**
	  * scopeIds of the accessible AuthTokenScopeLinks
	  */
	def scopeIds(implicit connection: Connection) = pullColumn(model.scopeIdColumn)
		.flatMap { value => value.int }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * A copy of this access point which includes scope data
	  */
	def withScopes = {
		accessCondition match
		{
			case Some(c) => DbAuthTokenScopes.filter(c)
			case None => DbAuthTokenScopes
		}
	}
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthTokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenScopeLinkFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyAuthTokenScopeLinksAccess = 
		ManyAuthTokenScopeLinksAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the scopeId of the targeted AuthTokenScopeLink instance(s)
	  * @param newScopeId A new scopeId to assign
	  * @return Whether any AuthTokenScopeLink instance was affected
	  */
	def scopeIds_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn,
		newScopeId)
	
	/**
	  * Updates the tokenId of the targeted AuthTokenScopeLink instance(s)
	  * @param newTokenId A new tokenId to assign
	  * @return Whether any AuthTokenScopeLink instance was affected
	  */
	def tokenIds_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn,
		newTokenId)
	
	/**
	  * @param tokenId Id of the targeted authentication token
	  * @return An access point to that token's scope links
	  */
	def withTokenId(tokenId: Int) = filter(model.withTokenId(tokenId).toCondition)
}

