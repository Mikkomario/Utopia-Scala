package utopia.vigil.database.access.token.template.right

import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.model.stored.token.TokenTemplateScope

object AccessTokenTemplateScope extends AccessOneRoot[AccessTokenTemplateScope[TokenTemplateScope]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessTokenTemplateScopes.root.head
}

/**
  * Used for accessing individual token template scopes from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenTemplateScope[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessTokenTemplateScope[A]] 
		with HasValues[AccessTokenTemplateScopeValue] 
		with FilterTokenTemplateScopes[AccessTokenTemplateScope[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenTemplateScopeValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessTokenTemplateScope(newTarget)
}

