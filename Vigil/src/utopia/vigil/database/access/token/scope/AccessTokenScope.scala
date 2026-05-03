package utopia.vigil.database.access.token.scope

import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.model.stored.token.TokenScope

object AccessTokenScope extends AccessOneRoot[AccessTokenScope[TokenScope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessTokenScopes.root.head
}

/**
  * Used for accessing individual token scopes from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenScope[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessTokenScope[A]] with HasValues[AccessTokenScopeValue] 
		with FilterTokenScopes[AccessTokenScope[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenScopeValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessTokenScope(newTarget)
}

