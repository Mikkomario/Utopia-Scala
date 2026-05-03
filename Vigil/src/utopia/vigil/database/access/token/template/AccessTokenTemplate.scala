package utopia.vigil.database.access.token.template

import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.model.stored.token.TokenTemplate

object AccessTokenTemplate extends AccessOneRoot[AccessTokenTemplate[TokenTemplate]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessTokenTemplates.root.head
}

/**
  * Used for accessing individual token templates from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenTemplate[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessTokenTemplate[A]] with HasValues[AccessTokenTemplateValue] 
		with FilterTokenTemplates[AccessTokenTemplate[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenTemplateValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessTokenTemplate(newTarget)
}

