package utopia.ambassador.database.factory.token

import utopia.ambassador.database.factory.scope.AuthTokenScopeFactory
import utopia.ambassador.model.combined.scope.AuthTokenScope
import utopia.ambassador.model.combined.token.AuthTokenWithScopes
import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.nosql.factory.multi.MultiCombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Reads authentication tokens from the DB and includes granted scope information
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object AuthTokenWithScopesFactory extends MultiCombiningFactory[AuthTokenWithScopes, AuthToken, AuthTokenScope]
	with Deprecatable
{
	// IMPLEMENTED  ------------------------------
	
	override def parentFactory = AuthTokenFactory
	
	override def childFactory = AuthTokenScopeFactory
	
	override def isAlwaysLinked = false
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition
	
	override def apply(parent: AuthToken, children: Seq[AuthTokenScope]) =
		AuthTokenWithScopes(parent, children.view.map { _.scope }.toSet)
}
