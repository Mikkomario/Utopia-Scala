package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.access.model.enumeration.Status.Unauthorized
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.controller.api.node.ApiNode
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.{Follow, NotFound}
import utopia.nexus.model.request.Request.StreamedRequest
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.database.store.TokenDb
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.combined.token.ScopedToken

object TokensNode
{
	/**
	 * @param templateCreationScope Scope required for creating token templates
	 *                              (recommended = developer or specialized scope)
	 * @param name Name of this node. Default = "tokens"
	 * @param otherChildren Other child nodes to include. Default = empty.
	 * @tparam C Type of the accepted context
	 * @return A new tokens API node
	 */
	def includingTemplates[C <: AuthContext[StreamedRequest] with PostContext](templateCreationScope: ScopeTarget,
	                                                                           name: String = "tokens",
	                                                                           otherChildren: Seq[ApiNode[C]] = Empty) =
		apply[C](name, new TokenTemplatesNode(templateCreationScope) +: otherChildren)
	/**
	 * @param name Name of this node. Default = "tokens"
	 * @param children Children given to this node. Default = empty.
	 * @tparam C Type of the accepted context
	 * @return A new tokens API node
	 */
	def apply[C <: AuthContext[Any]](name: String = "tokens", children: Seq[ApiNode[C]] = Empty) =
		new TokensNode[C](name, children)
}

/**
 * An API node for generating new authentication tokens.
 * Used, for example, when creating new sessions or swapping tokens.
 * @author Mikko Hilpinen
 * @since 04.05.2026, v0.1
 */
class TokensNode[-C <: AuthContext[Any]](override val name: String = "tokens", children: Seq[ApiNode[C]] = Empty)
	extends ApiNode[C]
{
	// ATTRIBUTES   -------------------
	
	override val allowedMethods: Iterable[Method] = Single(Post)
	
	
	// IMPLEMENTED  -------------------
	
	// Provides access to individual tokens
	override def follow(step: String)(implicit context: C): PathFollowResult[C] = {
		if (step ~== "current")
			Follow(new TokenNode(None))
		else
			children.find { _.name ~== step } match {
				case Some(child) => Follow(child)
				case None =>
					step.int match {
						case Some(tokenId) => Follow(new TokenNode(Some(tokenId)))
						case None => NotFound(s"`$step` is not a valid token ID")
					}
			}
	}
	
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: C): RequestResult =
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
