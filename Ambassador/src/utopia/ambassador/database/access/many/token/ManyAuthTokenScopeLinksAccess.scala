package utopia.ambassador.database.access.many.token

import utopia.ambassador.database.factory.token.AuthTokenScopeLinkFactory
import utopia.ambassador.database.model.token.AuthTokenScopeLinkModel
import utopia.ambassador.model.stored.token.AuthTokenScopeLink
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyAuthTokenScopeLinksAccess
{
	// NESTED	--------------------
	
	private class ManyAuthTokenScopeLinksSubView(override val parent: ManyRowModelAccess[AuthTokenScopeLink], 
		override val filterCondition: Condition) 
		extends ManyAuthTokenScopeLinksAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthTokenScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthTokenScopeLinksAccess extends ManyRowModelAccess[AuthTokenScopeLink] with Indexed
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
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthTokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthTokenScopeLinkFactory
	
	override protected def defaultOrdering = None
	
	override def filter(additionalCondition: Condition): ManyAuthTokenScopeLinksAccess = 
		new ManyAuthTokenScopeLinksAccess.ManyAuthTokenScopeLinksSubView(this, additionalCondition)
	
	
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
}

