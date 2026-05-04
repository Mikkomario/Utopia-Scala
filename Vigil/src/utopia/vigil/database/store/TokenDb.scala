package utopia.vigil.database.store

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.parse.Sha256Hasher
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.UncertainBoolean
import utopia.flow.util.UncertainBoolean.{CertainlyFalse, CertainlyTrue}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.database.{Connection, Store}
import utopia.vigil.database.access.token.AccessTokens
import utopia.vigil.database.access.token.scope.AccessTokenScopes
import utopia.vigil.database.access.token.template.AccessTokenTemplates
import utopia.vigil.database.access.token.template.right.{AccessTokenGrantRight, AccessTokenGrantRights, AccessTokenTemplateScopes}
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
	// ATTRIBUTES   -----------------------
	
	private val _storeGrantRights = Store
		.apply(TokenGrantRightDbModel) { right: (Int, Int, Boolean, UncertainBoolean) =>
			TokenGrantRightData(ownerTemplateId = right._1, grantedTemplateId = right._2, revokesOriginal = right._3,
				revokesEarlier = right._4)
		}
		// May update the revokes -properties
		.updating { case ((_, _, revokesOriginal, revokesEarlier), existing, connection) =>
			val revokesOriginalChanged = existing.revokesOriginal != revokesOriginal
			val revokesEarlierChanged = existing.revokesEarlier != revokesEarlier
			
			if (revokesOriginalChanged || revokesEarlierChanged)
				connection.use { implicit c =>
					val access = existing.access.values
					if (revokesOriginalChanged)
						access.revokesOriginal.set(revokesOriginal)
					if (revokesEarlierChanged)
						access.revokesEarlier.set(revokesEarlier)
					
					Some(existing.withRevokesOriginal(revokesOriginal).withRevokesEarlier(revokesEarlier))
				}
			else
				None
		}
	
	
	// OTHER    ---------------------------
	
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
	 * @param revokesEarlier Whether previously generated session tokens should be revoked when starting a new session.
	 *                       Uncertain if this should be toggled manually.
	 *                       Default = true.
	 * @param connection Implicit DB connection
	 * @return Created refresh token template + created session token template
	 */
	def createRefreshTokenTemplate(scope: Seq[ScopeTarget], sessionDuration: Duration, name: String = "",
	                               sessionTokenName: String = "", revokesEarlier: UncertainBoolean = CertainlyTrue)
	                              (implicit connection: Connection) =
	{
		val session = createSimpleSessionTemplate(sessionDuration, sessionTokenName)
		val refresh = createRefreshTokenTemplateFor(session.id, scope, name, revokesEarlier)
		
		refresh -> session
	}
	/**
	 * Creates a new refresh token template
	 * @param sessionTemplateId ID of the granted session token template
	 * @param scope Scope forwarded by this token.
	 *              Default = empty (assumes that the session token already specifies scope)
	 * @param name Name of this token template (optional)
	 * @param revokesEarlier Whether previously generated session tokens should be revoked when starting a new session.
	 *                       Uncertain if this should be toggled manually.
	 *                       Default = true.
	 * @param connection Implicit DB connection
	 * @return Created refresh token template
	 */
	def createRefreshTokenTemplateFor(sessionTemplateId: Int, scope: Iterable[ScopeTarget] = Empty, name: String = "",
	                                  revokesEarlier: UncertainBoolean = CertainlyTrue)
	                                 (implicit connection: Connection) =
		createTemplate(name, forwardedScopes = scope, grantsTokensOfTemplates = Single(sessionTemplateId),
			revokesEarlier = revokesEarlier)
	
	/**
	 * Creates a new single-use swap token template
	 * @param scope Scope accessible by the created session token
	 * @param duration Duration how long these swap tokens remain usable
	 * @param sessionDuration Duration of the created session tokens
	 * @param name Name of this token template (optional)
	 * @param sessionTokenName Name given to the session token template (optional)
	 * @param connection Implicit DB connection
	 * @return Created swap token template + created session token template
	 */
	def createSwapToSessionTemplate(scope: Iterable[ScopeTarget], duration: Duration,
	                                sessionDuration: Duration = Duration.infinite, name: String = "",
	                                sessionTokenName: String = "")
	                               (implicit connection: Connection) =
	{
		val session = createSimpleSessionTemplate(sessionDuration, sessionTokenName)
		val swap = createSwapTemplateFor(session.id, duration, scope, name)
		
		swap -> session
	}
	/**
	 * Creates a new single-use swap token template
	 * @param acquiredTokenTemplateId ID of the template of the acquired tokens
	 * @param duration Duration how long these swap tokens remain usable
	 * @param scope Scope forwarded by this token.
	 *              Default = empty (assumes that the session token already specifies scope)
	 * @param name Name of this token template (optional)
	 * @param connection Implicit DB connection
	 * @return Created swap token template
	 */
	def createSwapTemplateFor(acquiredTokenTemplateId: Int, duration: Duration, scope: Iterable[ScopeTarget] = Empty,
	                          name: String = "")
	                         (implicit connection: Connection) =
		createTemplate(name, forwardedScopes = scope, grantsTokensOfTemplates = Single(acquiredTokenTemplateId),
			duration = duration, revokesOriginal = true)
	
	/**
	 * Creates a simple session token with no authentication scope of its own
	 * @param sessionDuration Duration how long each session is in effect
	 * @param name Name of this token (optional)
	 * @param connection Implicit DB connection
	 * @return Created session token template
	 */
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
	 * @param revokesOriginal Whether this type of token is swapped / revoked, when granting new tokens.
	 *                        Default = false = These tokens will remain usable afterwards.
	 * @param revokesEarlier Whether the earlier generated tokens should get revoked when generating new tokens.
	 *                       Uncertain if this should be toggled manually.
	 *                       Default = false.
	 * @param connection Implicit DB connection
	 * @return Stored token template
	 */
	def createTemplate(name: String = "", scopeGrantType: ScopeGrantType = Dictate,
	                   accessibleScopes: Seq[ScopeTarget] = Empty,
	                   forwardedScopes: Iterable[ScopeTarget] = Empty, grantsTokensOfTemplates: Iterable[Int] = Empty,
	                   duration: Duration = Duration.infinite, revokesOriginal: Boolean = false,
	                   revokesEarlier: UncertainBoolean = CertainlyFalse)
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
				.map { grantedTemplateId =>
					TokenGrantRightData(template.id, grantedTemplateId, revokesOriginal = revokesOriginal,
						revokesEarlier = revokesEarlier) }
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
	 * @return Returns 3 values:
	 *         1. Generated tokens, where each contains:
	 *              1. Generated token string / key
	 *              1. Stored token entry
	 *         1. Whether 'token' was revoked in this process
	 *         1. Whether previously generated tokens were revoked in this process
	 */
	def grantUsing(token: TokenIdRefs, name: => String = "", revokeEarlierDefault: Boolean = false)
	              (implicit connection: Connection) =
	{
		// Checks the grant rights
		val grantedTemplates = AccessTokenTemplates.whereOriginatingGrantRight.ofTemplate(token.templateId).pull
		// Case: No grant rights => No change
		if (grantedTemplates.isEmpty)
			(Empty, false, false)
		else {
			// Checks what to revoke
			val (revokesOriginal, revokeEarlierFromTemplates) =
				AccessTokenGrantRights.ofTemplate(token.templateId).revokeInfo(revokeEarlierDefault)
			
			// Revokes earlier generated tokens, if applicable
			val earlierWereRevoked = {
				if (revokeEarlierFromTemplates.nonEmpty)
					AccessTokens.active.fromTemplates(revokeEarlierFromTemplates).createdUsing(token.id).revoke()
				else
					false
			}
			
			// Generates the new tokens
			val parentScopeIdsView = Lazy { AccessTokenScopes.ofToken(token.id).scopeIds.toSet }
			val granted = grantedTemplates.map { _createToken(_, Some(token.id), parentScopeIdsView, name) }
			
			// Revokes the original token, if appropriate
			val wasRevoked = {
				if (revokesOriginal)
					token.access.revoke()
				else
					false
			}
			
			(granted, wasRevoked, earlierWereRevoked)
		}
	}
	
	/**
	 * Gives a token type permission to generate tokens of another type
	 * @param ownerTemplateId ID of the token template granted this right
	 * @param grantedTemplateId ID of the token template used when generating new tokens
	 * @param revokesOriginal Whether the original tokens should be revoked when new tokens are generated.
	 *                        Default = false.
	 * @param revokesEarlier Whether earlier generated tokens should be revoked when a new token is generated.
	 *                       Uncertain if this should be toggled manually. Default = false.
	 * @param connection Implicit DB connection
	 * @return Grant right store result
	 */
	def giveGrantRight(ownerTemplateId: Int, grantedTemplateId: Int, revokesOriginal: Boolean = false,
	                   revokesEarlier: UncertainBoolean = CertainlyFalse)
	                  (implicit connection: Connection) =
		_storeGrantRights.single((ownerTemplateId, grantedTemplateId, revokesOriginal, revokesEarlier),
			AccessTokenGrantRight.ofTemplate(ownerTemplateId).toUseTemplate(grantedTemplateId).pull)
	
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
