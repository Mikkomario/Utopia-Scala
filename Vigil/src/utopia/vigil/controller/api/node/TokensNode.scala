package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.access.model.enumeration.Status.Unauthorized
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.nexus.controller.api.node.LeafNode
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
// TODO: Add end session node
class TokensNode(override val name: String = "tokens") extends LeafNode[AuthContext[Any]]
{
	// ATTRIBUTES   -------------------
	
	override val allowedMethods: Iterable[Method] = Single(Post)
	
	
	// IMPLEMENTED  -------------------
	
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
