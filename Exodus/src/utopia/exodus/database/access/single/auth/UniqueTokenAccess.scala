package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.TokenFactory
import utopia.exodus.database.model.auth.{TokenModel, TokenScopeLinkModel}
import utopia.exodus.model.enumeration.ScopeIdWrapper
import utopia.exodus.model.stored.auth.Token
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.{Exists, Select, Where}

/**
  * A common trait for access points that return individual and distinct tokens.
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait UniqueTokenAccess 
	extends SingleRowModelAccess[Token] with DistinctModelAccess[Token, Option[Token], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the token type applicable to this token. None if no instance (or value) was found.
	  */
	def typeId(implicit connection: Connection) = pullColumn(model.typeIdColumn).int
	
	/**
	  * A hashed version of this token. None if no instance (or value) was found.
	  */
	def hash(implicit connection: Connection) = pullColumn(model.hashColumn).string
	
	/**
	  * Id of the token that was used to acquire this token, 
	  * if applicable & still known. None if no instance (or value) was found.
	  */
	def parentTokenId(implicit connection: Connection) = pullColumn(model.parentTokenIdColumn).int
	
	/**
	  * Id of the user who owns this token, if applicable. None if no instance (or value) was found.
	  */
	def ownerId(implicit connection: Connection) = pullColumn(model.ownerIdColumn).int
	
	/**
	  * Model style preferred during this session. None if no instance (or value) was found.
	  */
	def modelStylePreference(implicit connection: Connection) = 
		pullColumn(model.modelStylePreferenceColumn).int.flatMap(ModelStyle.findForId)
	
	/**
	  * Time when this token expires, if applicable. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	
	/**
	  * Time when this token was issued. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Time when this token was revoked or replaced. None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	/**
	  * Whether this token may only be used once (successfully). None if no instance (or value) was found.
	  */
	def isSingleUseOnly(implicit connection: Connection) = pullColumn(model.isSingleUseOnlyColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenModel
	
	protected def scopeLinkModel = TokenScopeLinkModel
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the scopes that are directly accessible using this token
	  */
	def accessibleScopeIds(implicit connection: Connection) =
		connection(Select(target join scopeLinkModel.table, scopeLinkModel.scopeIdColumn) +
			Where(mergeCondition(scopeLinkModel.directlyAccessible.toCondition)) + factory.defaultOrdering)
			.rowIntValues.toSet
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted tokens
	  * @param newCreated A new created to assign
	  * @return Whether any token was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Deprecates all accessible tokens
	  * @return Whether any row was targeted
	  */
	def deprecate()(implicit connection: Connection) = deprecatedAfter = Now
	
	/**
	  * Updates the deprecation times of the targeted tokens
	  * @param newDeprecatedAfter A new deprecated after to assign
	  * @return Whether any token was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the expiration times of the targeted tokens
	  * @param newExpires A new expires to assign
	  * @return Whether any token was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Tests whether this token may access the specified scope
	  * @param scope Targeted scope
	  * @param connection Implicit DB Connection
	  * @return Whether this token may access that scope
	  */
	def hasScope(scope: ScopeIdWrapper)(implicit connection: Connection) = hasScopeWithId(scope.id)
	
	/**
	  * Tests whether this token may access the specified scope
	  * @param scopeId Id of the targeted scope
	  * @param connection Implicit DB Connection
	  * @return Whether this token may access that scope
	  */
	def hasScopeWithId(scopeId: Int)(implicit connection: Connection) = 
		Exists(target join scopeLinkModel.table, 
			mergeCondition(scopeLinkModel.directlyAccessible.withScopeId(scopeId).toCondition))
	
	/**
	  * @param userId A user id
	  * @param connection Implicit DB Connection
	  * @return Whether this token is owned by that user id
	  */
	def isOwnedByUserWithId(userId: Int)(implicit connection: Connection) =
		exists(model.withOwnerId(userId).toCondition)
	
	/**
	  * Updates the hashes of the targeted tokens
	  * @param newHash A new hash to assign
	  * @return Whether any token was affected
	  */
	def hash_=(newHash: String)(implicit connection: Connection) = putColumn(model.hashColumn, newHash)
	
	/**
	  * Pulls this token from the DB, but only if it allows access to the specified scope
	  * @param scope Targeted scope
	  * @param connection Implicit DB Connection
	  * @return Read token. None if no matching token was found or the token doesn't grant access to the
	  * specified scope.
	  */
	def havingScope(scope: ScopeIdWrapper)(implicit connection: Connection) = havingScopeWithId(scope.id)
	
	/**
	  * Pulls this token from the DB, but only if it allows access to the specified scope
	  * @param scopeId Id of the targeted scope
	  * @param connection Implicit DB Connection
	  * @return Read token. None if no matching token was found or the token doesn't grant access to the
	  * specified scope.
	  */
	def havingScopeWithId(scopeId: Int)(implicit connection: Connection) = 
		factory.findLinked(scopeLinkModel.table, 
			mergeCondition(scopeLinkModel.directlyAccessible.withScopeId(scopeId).toCondition))
	
	/**
	  * Updates the are single use only of the targeted tokens
	  * @param newIsSingleUseOnly A new is single use only to assign
	  * @return Whether any token was affected
	  */
	def isSingleUseOnly_=(newIsSingleUseOnly: Boolean)(implicit connection: Connection) = 
		putColumn(model.isSingleUseOnlyColumn, newIsSingleUseOnly)
	
	/**
	  * Updates the model style preferences of the targeted tokens
	  * @param newModelStylePreference A new model style preference to assign
	  * @return Whether any token was affected
	  */
	def modelStylePreference_=(newModelStylePreference: ModelStyle)(implicit connection: Connection) = 
		putColumn(model.modelStylePreferenceColumn, newModelStylePreference.id)
	
	/**
	  * Updates the owner ids of the targeted tokens
	  * @param newOwnerId A new owner id to assign
	  * @return Whether any token was affected
	  */
	def ownerId_=(newOwnerId: Int)(implicit connection: Connection) = putColumn(model.ownerIdColumn, 
		newOwnerId)
	
	/**
	  * Updates the parent token ids of the targeted tokens
	  * @param newParentTokenId A new parent token id to assign
	  * @return Whether any token was affected
	  */
	def parentTokenId_=(newParentTokenId: Int)(implicit connection: Connection) = 
		putColumn(model.parentTokenIdColumn, newParentTokenId)
	
	/**
	  * Updates the type ids of the targeted tokens
	  * @param newTypeId A new type id to assign
	  * @return Whether any token was affected
	  */
	def typeId_=(newTypeId: Int)(implicit connection: Connection) = putColumn(model.typeIdColumn, newTypeId)
}

