package utopia.vigil.database.store

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.parse.Sha256Hasher
import utopia.flow.time.{Duration, Now}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.database.Connection
import utopia.vigil.database.access.token.scope.AccessTokenScopes
import utopia.vigil.database.access.token.template.AccessTokenTemplates
import utopia.vigil.database.access.token.template.right.{AccessTokenGrantRights, AccessTokenTemplateScopes}
import utopia.vigil.database.storable.token._
import utopia.vigil.model.cached.scope.ScopeTarget
import utopia.vigil.model.cached.token.TokenIdRefs
import utopia.vigil.model.combined.token.{DetailedTokenTemplate, ScopedToken}
import utopia.vigil.model.enumeration.ScopeGrantType
import utopia.vigil.model.enumeration.ScopeGrantType.{Copy, Dictate, Grant, Restrict}
import utopia.vigil.model.partial.token._
import utopia.vigil.model.stored.token.TokenTemplate

import java.util.UUID

/**
 * An interface used for creating and modifying authorization tokens
 * @author Mikko Hilpinen
 * @since 03.05.2026, v0.1
 */
object TokenDb
{
	/**
	 * Creates a template for permanent API-keys
	 * @param scope Granted scope
	 * @param name Name of this token type (optional)
	 * @param connection Implicit DB connection
	 * @return A new token template
	 */
	def createStaticApiKey(scope: Seq[ScopeTarget], name: String = "")(implicit connection: Connection) =
		createTemplate(name, accessibleScopes = scope)
	
	/**
	 * Creates a new refresh token template
	 * @param scope Scope accessible by the created session tokens
	 * @param sessionDuration Duration of the created session tokens
	 * @param name Name of this token template (optional)
	 * @param sessionTokenName Name given to the session token template (optional)
	 * @param connection Implicit DB connection
	 * @return Created refresh token template + created session token template
	 */
	def createRefreshTokenTemplate(scope: Seq[ScopeTarget], sessionDuration: Duration, name: String = "",
	                               sessionTokenName: String = "")
	                              (implicit connection: Connection) =
	{
		val session = createSimpleSessionTemplate(sessionDuration, sessionTokenName)
		val refresh = createRefreshTokenTemplateFor(session.id, scope, name)
		
		refresh -> session
	}
	/**
	 * Creates a new refresh token template
	 * @param sessionTemplateId ID of the granted session token template
	 * @param scope Scope forwarded by this token.
	 *              Default = empty (assumes that the session token already specifies scope)
	 * @param name Name of this token template (optional)
	 * @param connection Implicit DB connection
	 * @return Created refresh token template + created session token template
	 */
	def createRefreshTokenTemplateFor(sessionTemplateId: Int, scope: Iterable[ScopeTarget] = Empty, name: String = "")
	                                 (implicit connection: Connection) =
		createTemplate(name, forwardedScopes = scope, grantsTokensOfTemplates = Single(sessionTemplateId))
	
	def createSwapToSessionTemplate(scope: Iterable[ScopeTarget], duration: Duration,
	                                sessionDuration: Duration = Duration.infinite, name: String = "",
	                                sessionTokenName: String = "")
	                               (implicit connection: Connection) =
	{
		val session = createSimpleSessionTemplate(sessionDuration, sessionTokenName)
		val swap = createSwapTemplateFor(session.id, duration, scope, name)
		
		swap -> session
	}
	def createSwapTemplateFor(acquiredTokenTemplateId: Int, duration: Duration, scope: Iterable[ScopeTarget] = Empty,
	                          name: String = "")
	                         (implicit connection: Connection) =
		createTemplate(name, forwardedScopes = scope, grantsTokensOfTemplates = Single(acquiredTokenTemplateId),
			duration = duration, swaps = true)
	
	def createSimpleSessionTemplate(sessionDuration: Duration, name: String = "")(implicit connection: Connection) =
		createTemplate(name, scopeGrantType = Copy, duration = sessionDuration)
	
	/**
	 * Creates a new token template
	 * @param name Name to give to this template (optional)
	 * @param scopeGrantType Approach to scope-granting (default = hard-coded scope)
	 * @param accessibleScopes Scopes that are directly accessible using these tokens. Default = empty.
	 * @param forwardedScopes Scopes that may be given to the generated child tokens during token-granting.
	 *                        Default = empty.
	 * @param grantsTokensOfTemplates IDs of the generated token type templates when applying the token grant function.
	 *                                I.e. templates of tokens that may be generated using this kind of token.
	 *                                Default = empty.
	 * @param duration Duration how long generated tokens should remain usable. Default = infinite.
	 * @param swaps Whether this type of token is swapped / revoked, when granting new tokens.
	 *              Default = false = These tokens will remain usable afterwards.
	 * @param connection Implicit DB connection
	 * @return Stored token template
	 */
	def createTemplate(name: String = "", scopeGrantType: ScopeGrantType = Dictate,
	                   accessibleScopes: Seq[ScopeTarget] = Empty,
	                   forwardedScopes: Iterable[ScopeTarget] = Empty, grantsTokensOfTemplates: Iterable[Int] = Empty,
	                   duration: Duration = Duration.infinite, swaps: Boolean = false)
	                  (implicit connection: Connection) =
	{
		// Inserts the template
		val template = TokenTemplateDbModel.insert(TokenTemplateData(name, scopeGrantType, duration.ifFinite))
		
		// Assigns the scopes
		val scopeLinks = TokenTemplateScopeDbModel.insert(
			(accessibleScopes.iterator.map { _ -> true } ++
				forwardedScopes.iterator.filterNot(accessibleScopes.contains).map { _ -> false })
				.map { case (scope, usable) => TokenTemplateScopeData(scope.id, template.id, usable = usable) }
				.toOptimizedSeq)
		// Assigns the token-grant rights
		val grantRights = TokenGrantRightDbModel.insert(
			grantsTokensOfTemplates.view
				.map { grantedTemplateId => TokenGrantRightData(template.id, grantedTemplateId, revokes = swaps) }
				.toOptimizedSeq)

		DetailedTokenTemplate(template.id, template, scopeLinks, grantRights)
	}
	
	/**
	 * Creates a new authorization token
	 * @param template Template used for creating this token
	 * @param parentId ID of the token used for generating this token. Optional.
	 *
	 *                 NB: No authorization / validity checks are performed for this value.
	 *                     This function assumes that referenced token may be used for this purpose.
	 * @param name Name to give to this token (optional)
	 * @param connection Implicit DB connection
	 * @return Returns 2 values:
	 *         1. Generated token string
	 *         1. Stored token entry
	 */
	def createToken(template: TokenTemplate, parentId: Option[Int] = None, name: String = "")
	               (implicit connection: Connection) =
	{
		val parentScopeIdsView = Lazy {
			parentId match {
				case Some(parentId) => AccessTokenScopes.ofToken(parentId).scopeIds.toSet
				case None => Set[Int]()
			}
		}
		_createToken(template, parentId, parentScopeIdsView, name)
	}
	
	/**
	 * Uses a token to grant new authorization tokens, according to that token's template.
	 *
	 * Note: If the token's template doesn't provide any token grant rights, no new tokens are generated.
	 *
	 * @param token Token (ids) to grant new tokens with
	 * @param name Name to give to the new tokens. Call-by-name, default = empty.
	 * @param connection Implicit DB connection
	 * @return Returns 2 values:
	 *         1. Generated tokens, where each contains:
	 *              1. Generated token string / key
	 *              1. Stored token entry
	 *         1. Whether 'token' was revoked in this process
	 */
	def grantUsing(token: TokenIdRefs, name: => String = "")(implicit connection: Connection) = {
		// Checks the grant rights
		val grantedTemplates = AccessTokenTemplates.whereOriginatingGrantRight.ofTemplate(token.templateId).pull
		// Case: No grant rights => No change
		if (grantedTemplates.isEmpty)
			Empty -> false
		else {
			// Generates the new tokens
			val parentScopeIdsView = Lazy { AccessTokenScopes.ofToken(token.id).scopeIds.toSet }
			val granted = grantedTemplates.map { _createToken(_, Some(token.id), parentScopeIdsView, name) }
			
			// Revokes the original token, if appropriate
			val wasRevoked = {
				if (AccessTokenGrantRights.ofTemplate(token.templateId).revokeOriginals.stream { _.contains(true) })
					token.access.revoke()
				else
					false
			}
			
			granted -> wasRevoked
		}
	}
	
	private def _createToken(template: TokenTemplate, parentId: Option[Int], parentScopeIdsView: View[Set[Int]],
	                         name: String)
	                        (implicit connection: Connection) =
	{
		// Creates the new token
		val key = UUID.randomUUID().toString
		val token = TokenDbModel.insert(TokenData(template.id, Sha256Hasher(key), parentId, name,
			expires = template.duration.map { Now + _ }))
		
		// Determines the scope given to the new token
		val scopesToAssign = {
			lazy val templateScopes = AccessTokenTemplateScopes.ofTemplate(template.id).scopeIdsAndUsableStates
			// Case: Template dictates the scope
			if (template.scopeGrantType == Dictate || (parentId.isEmpty && template.scopeGrantType == Grant))
				templateScopes
			// Case: The parent scope affects the given scope
			else if (parentId.isDefined) {
				val parentScopeIds = parentScopeIdsView.value
				template.scopeGrantType match {
					// Case: Parent scope is copied as is (except that all values become usable)
					case Copy => parentScopeIds.view.map { _ -> true }.toOptimizedSeq
					// Case: Parent scope may be restricted
					case Restrict =>
						if (parentScopeIds.isEmpty)
							Empty
						else
							templateScopes.filter { case (scopeId, _) => parentScopeIds.contains(scopeId) }
					// Case: Parent scope may be extended
					case _ =>
						(parentScopeIds.view.map { _ -> true } ++
							templateScopes.iterator
								.filterNot { case (scopeId, _) => parentScopeIds.contains(scopeId) })
							.toOptimizedSeq
				}
			}
			// Case: No parent to acquire the scope from
			else
				Empty
		}
		// Assigns the scope
		val scopeLinks = TokenScopeDbModel.insert(scopesToAssign.map { case (scopeId, direct) =>
			TokenScopeData(scopeId = scopeId, tokenId = token.id, usable = direct)
		})
		
		key -> ScopedToken(token, scopeLinks)
	}
}
