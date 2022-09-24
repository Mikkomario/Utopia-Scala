package utopia.exodus.database.access.single.auth

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import utopia.exodus.database.factory.auth.TokenTypeFactory
import utopia.exodus.database.model.auth.TokenTypeModel
import utopia.exodus.model.stored.auth.TokenType
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Exists

/**
  * A common trait for access points that return individual and distinct token types.
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait UniqueTokenTypeAccess 
	extends SingleRowModelAccess[TokenType] with DistinctModelAccess[TokenType, Option[TokenType], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Name of this token type for identification. Not localized.. None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	
	/**
	  * Duration that determines how long these tokens remain valid after issuing. None if these tokens don't
	  *  expire automatically.. None if no instance (or value) was found.
	  */
	def duration(implicit connection: Connection) = 
		pullColumn(model.durationColumn).long.map { FiniteDuration(_, TimeUnit.MINUTES) }
	
	/**
	  * Id of the type of token that may be acquired by using this token type as a refresh token, 
	  * if applicable. None if no instance (or value) was found.
	  */
	def refreshedTypeId(implicit connection: Connection) = pullColumn(model.refreshedTypeIdColumn).int
	
	/**
	  * Time when this token type was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * 
		Whether tokens of this type may only be used once (successfully). None if no instance (or value) was found.
	  */
	def isSingleUseOnly(implicit connection: Connection) = pullColumn(model.isSingleUseOnlyColumn).boolean
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenTypeModel
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Whether this is a refresh token
	  */
	def isRefreshToken(implicit connection: Connection) =
		Exists(target, mergeCondition(model.refreshedTypeIdColumn.isNotNull))
	/**
	  * @param connection Implicit DB Connection
	  * @return Whether this is an access token
	  */
	def isAccessToken(implicit connection: Connection) =
		Exists(target, mergeCondition(model.refreshedTypeIdColumn.isNull))
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TokenTypeFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted token types
	  * @param newCreated A new created to assign
	  * @return Whether any token type was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the durations of the targeted token types
	  * @param newDuration A new duration to assign
	  * @return Whether any token type was affected
	  */
	def duration_=(newDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.durationColumn, newDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the are single use only of the targeted token types
	  * @param newIsSingleUseOnly A new is single use only to assign
	  * @return Whether any token type was affected
	  */
	def isSingleUseOnly_=(newIsSingleUseOnly: Boolean)(implicit connection: Connection) = 
		putColumn(model.isSingleUseOnlyColumn, newIsSingleUseOnly)
	
	/**
	  * Updates the names of the targeted token types
	  * @param newName A new name to assign
	  * @return Whether any token type was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the refreshed type ids of the targeted token types
	  * @param newRefreshedTypeId A new refreshed type id to assign
	  * @return Whether any token type was affected
	  */
	def refreshedTypeId_=(newRefreshedTypeId: Int)(implicit connection: Connection) = 
		putColumn(model.refreshedTypeIdColumn, newRefreshedTypeId)
}

