package utopia.vigil.database.access.token.template.right

import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.database.access.token.template.FilterByTokenTemplate
import utopia.vigil.model.stored.token.TokenGrantRight

object AccessTokenGrantRight extends AccessOneRoot[AccessTokenGrantRight[TokenGrantRight]]
{
	// ATTRIBUTES	--------------------
	
	override val root = AccessTokenGrantRights.root.head
}

/**
  * Used for accessing individual token grant rights from the DB at a time
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
case class AccessTokenGrantRight[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessTokenGrantRight[A]] with HasValues[AccessTokenGrantRightValue] 
		with FilterTokenGrantRights[AccessTokenGrantRight[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenGrantRightValue(wrapped)
	
	lazy val joinParent = join(model.ownerTemplateId.column)
	lazy val whereParent = FilterByTokenTemplate(joinParent)
	
	lazy val joinGranted = join(model.grantedTemplateId.column)
	lazy val whereGranted = FilterByTokenTemplate(joinGranted)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessTokenGrantRight(newTarget)
}

