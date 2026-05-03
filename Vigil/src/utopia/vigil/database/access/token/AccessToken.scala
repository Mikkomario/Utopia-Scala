package utopia.vigil.database.access.token

import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneDeprecatingRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.model.stored.token.Token

object AccessToken extends AccessOneDeprecatingRoot[AccessToken[Token]]
{
	// ATTRIBUTES	--------------------
	
	override val all = AccessTokens.all.head
}

/**
  * Used for accessing individual tokens from the DB at a time
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessToken[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessToken[A]] with HasValues[AccessTokenValue] 
		with FilterTokens[AccessToken[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessTokenValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessToken(newTarget)
}

