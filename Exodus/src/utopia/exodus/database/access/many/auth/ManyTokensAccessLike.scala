package utopia.exodus.database.access.many.auth

import utopia.exodus.database.model.auth.TokenModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple tokens or similar instances at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyTokensAccessLike[+A, +Repr <: ManyModelAccess[A]] 
	extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * type ids of the accessible tokens
	  */
	def typeIds(implicit connection: Connection) = pullColumn(model.typeIdColumn).map { v => v.getInt }
	
	/**
	  * hashes of the accessible tokens
	  */
	def hashes(implicit connection: Connection) = pullColumn(model.hashColumn).map { v => v.getString }
	
	/**
	  * parent token ids of the accessible tokens
	  */
	def parentTokenIds(implicit connection: Connection) = pullColumn(model.parentTokenIdColumn)
		.flatMap { _.int }
	
	/**
	  * owner ids of the accessible tokens
	  */
	def ownerIds(implicit connection: Connection) = pullColumn(model.ownerIdColumn).flatMap { _.int }
	
	/**
	  * model style preferences of the accessible tokens
	  */
	def modelStylePreferences(implicit connection: Connection) = 
		pullColumn(model.modelStylePreferenceColumn).flatMap { _.int }.flatMap(ModelStyle.findForId)
	
	/**
	  * expiration times of the accessible tokens
	  */
	def expirationTimes(implicit connection: Connection) = pullColumn(model.expiresColumn)
		.flatMap { _.instant }
	
	/**
	  * creation times of the accessible tokens
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	/**
	  * deprecation times of the accessible tokens
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { _.instant }
	
	/**
	  * are single use only of the accessible tokens
	  */
	def areSingleUseOnly(implicit connection: Connection) = 
		pullColumn(model.isSingleUseOnlyColumn).map { v => v.getBoolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * A copy of this access point which only targets temporary tokens
	  */
	def temporary = filter(model.expiresColumn.isNotNull)
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the are single use only of the targeted tokens
	  * @param newIsSingleUseOnly A new is single use only to assign
	  * @return Whether any token was affected
	  */
	def areSingleUseOnly_=(newIsSingleUseOnly: Boolean)(implicit connection: Connection) = 
		putColumn(model.isSingleUseOnlyColumn, newIsSingleUseOnly)
	
	/**
	  * @param parentTokenIds A collection of token ids
	  * @return An access point to tokens which were created using one of the specified tokens
	  */
	def createdUsingAnyOfTokens(parentTokenIds: Iterable[Int]) = 
		filter(model.parentTokenIdColumn in parentTokenIds)
	
	/**
	  * @param parentTokenId Id of the linked parent token id
	  * @return An access point to tokens that have been created using that token
	  */
	def createdUsingTokenWithId(parentTokenId: Int) = filter(model
		.withParentTokenId(parentTokenId).toCondition)
	
	/**
	  * Updates the creation times of the targeted tokens
	  * @param newCreated A new created to assign
	  * @return Whether any token was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Deprecates all accessible tokens
	  * @return Whether any row was targeted
	  */
	def deprecate()(implicit connection: Connection) = deprecationTimes = Now
	
	/**
	  * Updates the deprecation times of the targeted tokens
	  * @param newDeprecatedAfter A new deprecated after to assign
	  * @return Whether any token was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * @param tokenId Id of the token that should NOT be targeted
	  * @return A copy of this access point where that token has been excluded
	  */
	def excludingTokenWithId(tokenId: Int) = filter(index <> tokenId)
	
	/**
	  * Updates the expiration times of the targeted tokens
	  * @param newExpires A new expires to assign
	  * @return Whether any token was affected
	  */
	def expirationTimes_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the hashes of the targeted tokens
	  * @param newHash A new hash to assign
	  * @return Whether any token was affected
	  */
	def hashes_=(newHash: String)(implicit connection: Connection) = putColumn(model.hashColumn, newHash)
	
	/**
	  * Updates the model style preferences of the targeted tokens
	  * @param newModelStylePreference A new model style preference to assign
	  * @return Whether any token was affected
	  */
	def modelStylePreferences_=(newModelStylePreference: ModelStyle)(implicit connection: Connection) = 
		putColumn(model.modelStylePreferenceColumn, newModelStylePreference.id)
	
	/**
	  * @param userId Targeted user id
	  * @return An access point to tokens which are owned by that user
	  */
	def ownedByUserWithId(userId: Int) = filter(model.withOwnerId(userId).toCondition)
	
	/**
	  * Updates the owner ids of the targeted tokens
	  * @param newOwnerId A new owner id to assign
	  * @return Whether any token was affected
	  */
	def ownerIds_=(newOwnerId: Int)(implicit connection: Connection) = putColumn(model.ownerIdColumn,
		newOwnerId)
	
	/**
	  * Updates the parent token ids of the targeted tokens
	  * @param newParentTokenId A new parent token id to assign
	  * @return Whether any token was affected
	  */
	def parentTokenIds_=(newParentTokenId: Int)(implicit connection: Connection) = 
		putColumn(model.parentTokenIdColumn, newParentTokenId)
	
	/**
	  * Updates the type ids of the targeted tokens
	  * @param newTypeId A new type id to assign
	  * @return Whether any token was affected
	  */
	def typeIds_=(newTypeId: Int)(implicit connection: Connection) = putColumn(model.typeIdColumn, newTypeId)
}

