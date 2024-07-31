package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.TokenScopeLinkFactory
import utopia.exodus.database.model.auth.TokenScopeLinkModel
import utopia.exodus.model.stored.auth.TokenScopeLink
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, ViewFactory}
import utopia.vault.sql.Condition

import java.time.Instant

object ManyTokenScopeLinksAccess extends ViewFactory[ManyTokenScopeLinksAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyTokenScopeLinksAccess = 
		new _ManyTokenScopeLinksAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyTokenScopeLinksAccess(condition: Condition) extends ManyTokenScopeLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple token scope links at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyTokenScopeLinksAccess 
	extends ManyRowModelAccess[TokenScopeLink] with FilterableView[ManyTokenScopeLinksAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * tokens ids of the accessible token scope links
	  */
	def tokensIds(implicit connection: Connection) = pullColumn(model.tokenIdColumn).map { v => v.getInt }
	
	/**
	  * scopes ids of the accessible token scope links
	  */
	def scopesIds(implicit connection: Connection) = pullColumn(model.scopeIdColumn).map { v => v.getInt }
	
	/**
	  * creation times of the accessible token scope links
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	/**
	  * are directly accessible of the accessible token scope links
	  */
	def areDirectlyAccessible(implicit connection: Connection) = 
		pullColumn(model.isDirectlyAccessibleColumn).map { v => v.getBoolean }
	
	/**
	  * grant forward of the accessible token scope links
	  */
	def grantForward(implicit connection: Connection) = 
		pullColumn(model.grantsForwardColumn).map { v => v.getBoolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenScopeLinkFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyTokenScopeLinksAccess = ManyTokenScopeLinksAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the are directly accessible of the targeted token scope links
	  * @param newIsDirectlyAccessible A new is directly accessible to assign
	  * @return Whether any token scope link was affected
	  */
	def areDirectlyAccessible_=(newIsDirectlyAccessible: Boolean)(implicit connection: Connection) = 
		putColumn(model.isDirectlyAccessibleColumn, newIsDirectlyAccessible)
	
	/**
	  * Updates the creation times of the targeted token scope links
	  * @param newCreated A new created to assign
	  * @return Whether any token scope link was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the grant forward of the targeted token scope links
	  * @param newGrantsForward A new grants forward to assign
	  * @return Whether any token scope link was affected
	  */
	def grantForward_=(newGrantsForward: Boolean)(implicit connection: Connection) = 
		putColumn(model.grantsForwardColumn, newGrantsForward)
	
	/**
	  * Updates the scopes ids of the targeted token scope links
	  * @param newScopeId A new scope id to assign
	  * @return Whether any token scope link was affected
	  */
	def scopesIds_=(newScopeId: Int)(implicit connection: Connection) = putColumn(model.scopeIdColumn,
		newScopeId)
	
	/**
	  * Updates the tokens ids of the targeted token scope links
	  * @param newTokenId A new token id to assign
	  * @return Whether any token scope link was affected
	  */
	def tokensIds_=(newTokenId: Int)(implicit connection: Connection) = putColumn(model.tokenIdColumn,
		newTokenId)
	
	/**
	  * @param tokenId Id of the linked token
	  * @return An access point to token scope links concerning that token
	  */
	def withTokenId(tokenId: Int) = filter(model.withTokenId(tokenId).toCondition)
}

