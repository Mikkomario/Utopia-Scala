package utopia.exodus.database.access.many.auth

import utopia.exodus.database.access.many.auth.ManyTokenScopesAccess.ManyTokenScopesSubView
import utopia.exodus.database.factory.auth.TokenScopeFactory
import utopia.exodus.database.model.auth.TokenScopeLinkModel
import utopia.exodus.model.combined.auth.TokenScope
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyTokenScopesAccess
{
	private class ManyTokenScopesSubView(override val parent: ManyModelAccess[TokenScope],
	                                     override val filterCondition: Condition)
		extends ManyTokenScopesAccess with SubView
}

/**
  * Used for accessing multiple token scopes at a time
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
trait ManyTokenScopesAccess extends ManyScopesAccessLike[TokenScope, ManyTokenScopesAccess]
{
	// COMPUTED -----------------------------
	
	protected def linkModel = TokenScopeLinkModel
	
	
	// IMPLEMENTED  -------------------------
	
	override def factory = TokenScopeFactory
	
	override def filter(additionalCondition: Condition): ManyTokenScopesAccess =
		new ManyTokenScopesSubView(this, additionalCondition)
		
	
	// OTHER    -----------------------------
	
	/**
	  * @param tokenId Id of the linked token
	  * @return An access point to scopes linked with that token
	  */
	def withTokenId(tokenId: Int) = filter(linkModel.withTokenId(tokenId).toCondition)
}
