package utopia.vigil.controller.api.node

import utopia.access.model.enumeration.Method.Post
import utopia.access.model.enumeration.Status.Unauthorized
import utopia.access.model.enumeration.{Method, Status}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.immutable.caching.Lazy
import utopia.nexus.controller.api.context.PostContext
import utopia.nexus.controller.api.node.{ApiNode, LeafNode}
import utopia.nexus.model.api.PathFollowResult
import utopia.nexus.model.api.PathFollowResult.Follow
import utopia.nexus.model.response.RequestResult
import utopia.vault.database.Connection
import utopia.vigil.controller.api.context.AuthContext
import utopia.vigil.controller.api.node.TokensNode.GrantLogic
import utopia.vigil.controller.api.node.TokensNode.GrantLogic.GrantNormally
import utopia.vigil.database.access.token.template.AccessTokenTemplate
import utopia.vigil.database.store.TokenDb
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.cached.token.TokenIdRefs
import utopia.vigil.model.combined.token.ScopedToken
import utopia.vigil.model.stored.token.TokenTemplate

import scala.language.implicitConversions

object TokensNode
{
	// COMPUTED ----------------------------
	
	/**
	 * @tparam C Type of the required request context
	 * @return A factory for constructing these nodes
	 */
	def factory[C <: AuthContext[Any]] = TokensNodeFactory[C]()
	
	
	// IMPLICIT ----------------------------
	
	// Implicitly treats this object as a node factory
	def objectAsFactory(o: TokensNode.type): TokensNodeFactory[AuthContext[Any]] = o.factory
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param templateCreationScope Scope required for creating token templates
	 *                              (recommended = developer or specialized scope)
	 * @tparam C Type of the accepted context
	 * @return A new tokens API node
	 */
	def includingTemplates[C <: AuthContext[Any] with PostContext](templateCreationScope: ScopeTarget) =
		TokensNodeFactory[C](children = Single(new TokenTemplatesNode(templateCreationScope)))
		
	
	// NESTED   -----------------------------
	
	/**
	 * An interface used for constructing new token nodes
	 * @param name Name of the created node. Default = "tokens"
	 * @param children Child nodes applied. Default = empty.
	 * @param grantLogic Grant logic applied. Default = Normal grant logic.
	 * @tparam C Type of the accepted / required request context.
	 */
	case class TokensNodeFactory[-C <: AuthContext[Any]](name: String = "tokens", children: Seq[ApiNode[C]] = Empty,
	                                                    grantLogic: GrantLogic[C] = GrantNormally)
	{
		/**
		 * @param name Name given to this node
		 * @return Copy of this factory applying the given node name
		 */
		def withName(name: String) = copy(name = name)
		
		/**
		 * @param children Children placed under this node
		 * @tparam C2 Type of the request context required by the child nodes
		 * @return A copy of this factory including the specified child nodes
		 */
		def includingChildren[C2 <: C](children: IterableOnce[ApiNode[C2]]) =
			copy(children = this.children ++ children)
		/**
		 * @param child Child placed under this node
		 * @tparam C2 Type of the request context required by this child
		 * @return A copy of this factory including the specified child node
		 */
		def includingChild[C2 <: C](child: ApiNode[C2]) = copy(children = children :+ child)
		
		/**
		 * @param logic Token grant logic to apply
		 * @tparam C2 Type of the request context required by this logic
		 * @return Copy of this factory applying the specified grant logic
		 */
		def usingGrantLogic[C2 <: C](logic: GrantLogic[C2]) = copy(grantLogic = logic)
		
		/**
		 * @return A new tokens node, based on this factory's settings
		 */
		def apply() = new TokensNode[C](name, children, grantLogic)
	}
	
	object GrantLogic
	{
		// OTHER    ----------------------------
		
		/**
		 * @param f Function to wrap. Receives the following parameters:
		 *              1. Parent token used in authorizing this request
		 *              1. ID of the specifically targeted / granted token template.
		 *                 None if all usable templates are applied.
		 *              1. A lazily initialized container, that will contain the token-granting results.
		 *                 Each value (if any) represents a granted token, and contains the following:
		 *                      1. Generated token, including its scope
		 *                      1. Applicable token template
		 *              1. Request context
		 *              1. DB connection
		 * @tparam C Type of the accepted request context
		 * @return A new grant logic using the specified function
		 */
		def apply[C](f: (TokenIdRefs, Option[Int], Lazy[Seq[(ScopedToken, TokenTemplate)]], C, Connection) => Either[RequestResult, Model]): GrantLogic[C] =
			new _GrantLogic[C](f)
		
		
		// NESTED   ----------------------------
		
		/**
		 * The default grant logic, which doesn't restrict or modify token-granting in any way.
		 */
		object GrantNormally extends GrantLogic[Any]
		{
			override def apply(parent: TokenIdRefs, targetedTemplateId: Option[Int],
			                   grant: Lazy[Seq[(ScopedToken, TokenTemplate)]])
			                  (implicit context: Any, connection: Connection): Either[RequestResult, Model] =
				Right(Model.empty)
		}
		
		private class _GrantLogic[-C](f: (TokenIdRefs, Option[Int], Lazy[Seq[(ScopedToken, TokenTemplate)]], C, Connection) => Either[RequestResult, Model])
			extends GrantLogic[C]
		{
			override def apply(parent: TokenIdRefs, targetedTemplateId: Option[Int],
			                   grant: Lazy[Seq[(ScopedToken, TokenTemplate)]])
			                  (implicit context: C, connection: Connection): Either[RequestResult, Model] =
				f(parent, targetedTemplateId, grant, context, connection)
		}
	}
	
	/**
	 * A trait for additional logic implementations applied in token-granting.
	 * @tparam C Type of the accepted / required request context.
	 */
	trait GrantLogic[-C]
	{
		/**
		 * Applies this grant logic
		 * @param parent Parent token used in authorizing this request
		 * @param targetedTemplateId ID of the specifically targeted / granted token template.
		 *                           None if all usable templates are applied.
		 * @param grant A lazily initialized container, that will contain the token-granting results.
		 *              Each value represents a granted token, and contains the following:
		 *                  1. Generated token, including its scope
		 *                  1. Applicable token template
		 *
		 *              Note: This container may yield an empty collection,
		 *              in which case token-granting should be considered to have failed.
		 *              In these cases, token-granting always yields 401.
		 * @param context Implicit request context
		 * @param connection Implicit DB connection
		 * @return Either:
		 *              - Right: Properties to include in the response, if applicable
		 *                  - Yielding this will cause the tokens to be granted / generated,
		 *                    even if 'grant' was not called during this function's execution.
		 *              - Left: A request result to send out, representing a failure state
		 *                  - No additional tokens will be generated, if this result is given
		 */
		def apply(parent: TokenIdRefs, targetedTemplateId: Option[Int], grant: Lazy[Seq[(ScopedToken, TokenTemplate)]])
		         (implicit context: C, connection: Connection): Either[RequestResult, Model]
	}
}

/**
 * An API node for generating new authentication tokens.
 * Used, for example, when creating new sessions or swapping tokens.
 * @author Mikko Hilpinen
 * @since 04.05.2026, v0.1
 */
class TokensNode[-C <: AuthContext[Any]](override val name: String = "tokens", children: Seq[ApiNode[C]] = Empty,
                                         grantLogic: GrantLogic[C] = GrantNormally)
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
		// Allows the grant logic to grant tokens using this lazy container
		val lazyGrantResults = Lazy {
			// Generates the new token(s)
			TokenDb.grantUsing(token, limitToTemplateIds = limitToTemplateId,
				revokeEarlierDefault = context.request.parameters("revokeEarlier", "revoke_earlier").getBoolean)
		}
		// Applies the grant logic
		grantLogic(token, limitToTemplateId,
			lazyGrantResults.map { _._1.map { case (_, token, template) => token -> template } })
			// If successful, forms the response
			.leftOrMap { additionalProps =>
				// Grants the tokens, if not granted already
				val (tokens, wasRevoked, earlierWereRevoked) = lazyGrantResults.value
				
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
						def tokenToModel(token: String, stored: ScopedToken, template: TokenTemplate) =
							Model.from(
								"id" -> stored.id, "key" -> token,
								"type" -> (template.name: Value).nonEmptyOrElse(template.id),
								"scope" -> stored.scopeLinks.iterator
									.filter { _.usable }.map { _.scope.key }.toOptimizedSeq,
								"expires" -> stored.expires)
						val tokenProp = tokenOrTokens match {
							case Left((token, stored, template)) =>
								Constant("token", tokenToModel(token, stored, template))
							case Right(tokens) =>
								Constant("tokens",
									tokens.map { case (token, stored, template) =>
										tokenToModel(token, stored, template)
									})
						}
						RequestResult((tokenProp +:
							Model.from("originalWasRevoked" -> wasRevoked, "earlierWereRevoked" -> earlierWereRevoked))
							++ additionalProps)
				}
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
