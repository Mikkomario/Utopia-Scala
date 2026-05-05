package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.access.model.enumeration.Status.Unauthorized
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.{Follow, NotFound}
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.database.store.TokenDb
import utopia.vigil.model.combined.token.ScopedToken

/**
 * An API node for generating new authentication tokens.
 * Used, for example, when creating new sessions or swapping tokens.
 * @author Mikko Hilpinen
 * @since 04.05.2026, v0.1
 */
class TokensNode(override val name: String = "tokens") extends ApiNode[AuthContext[Any]]
{
	// ATTRIBUTES   -------------------
	
	override val allowedMethods: Iterable[Method] = Single(Post)
	
	
	// IMPLEMENTED  -------------------
	
	// Provides access to individual tokens
	override def follow(step: String)(implicit context: AuthContext[Any]): PathFollowResult[AuthContext[Any]] = {
		if (step ~== "current")
			Follow(new TokenNode(None))
		else
			step.int match {
				case Some(tokenId) => Follow(new TokenNode(Some(tokenId)))
				case None => NotFound(s"`$step` is not a valid token ID")
			}
	}
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: AuthContext[Any]): RequestResult =
		context.authorized { (token, connection) =>
			implicit val c: Connection = connection
			// Generates the new token(s)
			val (tokens, wasRevoked, earlierWereRevoked) = TokenDb.grantUsing(token,
				revokeEarlierDefault = context.request.parameters("revokeEarlier", "revoke_earlier").getBoolean)
			
			// Forms the result
			tokens.emptyOneOrMany match {
				// Case: No tokens were generated => 401
				case None => Unauthorized -> "The specified token can't be used for generating other tokens"
				// Case: Token(s) were generated => Returns the generated tokens
				case Some(tokenOrTokens) =>
					def tokenToModel(token: String, stored: ScopedToken) = Model.from(
						"id" -> stored.id, "key" -> token,
						"scope" -> stored.scopeLinks.iterator.filter { _.usable }.map { _.scope.key }.toOptimizedSeq,
						"expires" -> stored.expires)
					val tokenProp = tokenOrTokens match {
						case Left((token, stored)) => Constant("token", tokenToModel(token, stored))
						case Right(tokens) =>
							Constant("tokens", tokens.map { case (token, stored) => tokenToModel(token, stored) })
					}
					RequestResult(tokenProp +:
						Model.from("originalWasRevoked" -> wasRevoked, "earlierWereRevoked" -> earlierWereRevoked))
			}
		}
}
