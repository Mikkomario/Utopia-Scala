package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method.Post
import utopia.access.model.enumeration.Status.Unauthorized
import utopia.access.model.enumeration.{Method, Status}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.controller.api.node.{ApiNode, LeafNode}
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.Follow
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.database.access.token.template.AccessTokenTemplate
import utopia.vigil.database.store.TokenDb
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.cached.token.TokenIdRefs
import utopia.vigil.model.combined.token.ScopedToken
import utopia.vigil.model.stored.token.TokenTemplate

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
	def includingTemplates[C <: AuthContext[Any] with PostContext](templateCreationScope: ScopeTarget,
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
	
	// Provides access to the following:
	//      1. /current => The token used in the request
	//      2. A custom child
	//      3. /{ID: Int} => A specific token
	//      4. /{tokenType: String} => Tokens of a specific type
	override def follow(step: String)(implicit context: C): PathFollowResult[C] = {
		if (step ~== "current")
			Follow(new TokenNode(None))
		else
			children.find { _.name ~== step } match {
				case Some(child) => Follow(child)
				case None =>
					step.int match {
						case Some(tokenId) => Follow(new TokenNode(Some(tokenId)))
						case None => Follow(new TokensOfTypeNode(step))
					}
			}
	}
	
	// POST => Grants new tokens, based on the authorization token used
	override def apply(method: Method, remainingPath: Seq[String])(implicit context: C): RequestResult =
		context.authorized { (token, connection) =>
			implicit val c: Connection = connection
			grant(token)
		}
		
	
	// OTHER    ---------------------------
	
	private def grant(token: TokenIdRefs, limitToTemplateId: Option[Int] = None)
	                 (implicit connection: Connection, context: C): RequestResult =
	{
		// Generates the new token(s)
		val (tokens, wasRevoked, earlierWereRevoked) = TokenDb.grantUsing(token, limitToTemplateIds = limitToTemplateId,
			revokeEarlierDefault = context.request.parameters("revokeEarlier", "revoke_earlier").getBoolean)
		
		// Forms the result
		tokens.emptyOneOrMany match {
			// Case: No tokens were generated => 401
			case None =>
				val message = {
					if (limitToTemplateId.isDefined)
						"This kind of token can't be granted using the current authorization"
					else
						"You're not authorized to generate tokens"
				}
				Unauthorized -> message
				
			// Case: Token(s) were generated => Returns the generated tokens
			case Some(tokenOrTokens) =>
				def tokenToModel(token: String, stored: ScopedToken, template: TokenTemplate) = Model.from(
					"id" -> stored.id, "key" -> token, "type" -> (template.name: Value).nonEmptyOrElse(template.id),
					"scope" -> stored.scopeLinks.iterator.filter { _.usable }.map { _.scope.key }.toOptimizedSeq,
					"expires" -> stored.expires)
				val tokenProp = tokenOrTokens match {
					case Left((token, stored, template)) => Constant("token", tokenToModel(token, stored, template))
					case Right(tokens) =>
						Constant("tokens",
							tokens.map { case (token, stored, template) => tokenToModel(token, stored, template) })
				}
				RequestResult(tokenProp +:
					Model.from("originalWasRevoked" -> wasRevoked, "earlierWereRevoked" -> earlierWereRevoked))
		}
	}
	
	
	// NESTED   --------------------------
	
	private class TokensOfTypeNode(tokenType: String) extends LeafNode[C]
	{
		// ATTRIBUTES   ------------------
		
		override val allowedMethods: Iterable[Method] = Single(Post)
		
		
		// IMPLEMENTED  ------------------
		
		override def name: String = tokenType
		
		// POST => Grants tokens of this type, if possible
		override def apply(method: Method, remainingPath: Seq[String])(implicit context: C): RequestResult =
			context.authorized { (token, connection) =>
				implicit val c: Connection = connection
				// Identifies the targeted token type
				AccessTokenTemplate.withName(tokenType).id.pull match {
					// Case: Valid token type => Attempts to grant tokens of that type
					case Some(templateId) => grant(token, Some(templateId))
					
					// Case: Invalid token type => 404
					case None => Status.NotFound -> s"`$tokenType` is not a valid token type"
				}
			}
	}
}
