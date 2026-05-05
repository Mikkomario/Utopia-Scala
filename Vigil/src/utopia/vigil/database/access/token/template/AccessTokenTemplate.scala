package utopia.vigil.database.access.token.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneRoot, AccessOneWrapper, TargetingOne}
import utopia.vigil.database.VigilTables
import utopia.vigil.database.access.token.FilterByToken
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
	
	lazy val joinToken = join(VigilTables.token)
	lazy val whereToken = FilterByToken(joinToken)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessTokenTemplate(newTarget)
	
	
	// OTHER    ------------------------
	
	/**
	 * @param tokenId ID of a token
	 * @return Access to that token's template
	 */
	def ofToken(tokenId: Int) = whereToken.withId(tokenId)
}

