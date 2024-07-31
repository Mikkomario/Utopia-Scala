package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.TokenScopeFactory
import utopia.exodus.database.model.auth.TokenScopeLinkModel
import utopia.exodus.model.combined.auth.TokenScope
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyTokenScopesAccess extends ViewFactory[ManyTokenScopesAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyTokenScopesAccess = new _ManyTokenScopesAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyTokenScopesAccess(condition: Condition) extends ManyTokenScopesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * Used for accessing multiple token scopes at a time
  * @author Mikko Hilpinen
  * @since 19.02.2022, v4.0
  */
trait ManyTokenScopesAccess extends ManyScopesAccessLike[TokenScope, ManyTokenScopesAccess]
{
	// COMPUTED	--------------------
	
	protected def linkModel = TokenScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenScopeFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyTokenScopesAccess = ManyTokenScopesAccess(condition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param tokenId Id of the linked token
	  * @return An access point to scopes linked with that token
	  */
	def withTokenId(tokenId: Int) = filter(linkModel.withTokenId(tokenId).toCondition)
}

