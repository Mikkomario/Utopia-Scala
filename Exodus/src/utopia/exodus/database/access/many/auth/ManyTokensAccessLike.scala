package utopia.exodus.database.access.many.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.TokenFactory
import utopia.exodus.database.model.auth.TokenModel
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

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
	  * owner ids of the accessible tokens
	  */
	def ownerIds(implicit connection: Connection) = pullColumn(model.ownerIdColumn).flatMap { _.int }
	
	/**
	  * device ids of the accessible tokens
	  */
	def deviceIds(implicit connection: Connection) = pullColumn(model.deviceIdColumn).flatMap { _.int }
	
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
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenModel
	
	
	// OTHER	--------------------
	
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
	  * Updates the device ids of the targeted tokens
	  * @param newDeviceId A new device id to assign
	  * @return Whether any token was affected
	  */
	def deviceIds_=(newDeviceId: Int)(implicit connection: Connection) = 
		putColumn(model.deviceIdColumn, newDeviceId)
	
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
	  * Updates the owner ids of the targeted tokens
	  * @param newOwnerId A new owner id to assign
	  * @return Whether any token was affected
	  */
	def ownerIds_=(newOwnerId: Int)(implicit connection: Connection) = putColumn(model.ownerIdColumn, 
		newOwnerId)
	
	/**
	  * Updates the type ids of the targeted tokens
	  * @param newTypeId A new type id to assign
	  * @return Whether any token was affected
	  */
	def typeIds_=(newTypeId: Int)(implicit connection: Connection) = putColumn(model.typeIdColumn, newTypeId)
}

