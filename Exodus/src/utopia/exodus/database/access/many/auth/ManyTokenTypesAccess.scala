package utopia.exodus.database.access.many.auth

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import utopia.exodus.database.factory.auth.TokenTypeFactory
import utopia.exodus.database.model.auth.TokenTypeModel
import utopia.exodus.model.stored.auth.TokenType
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyTokenTypesAccess
{
	// NESTED	--------------------
	
	private class ManyTokenTypesSubView(override val parent: ManyRowModelAccess[TokenType], 
		override val filterCondition: Condition) 
		extends ManyTokenTypesAccess with SubView
}

/**
  * A common trait for access points which target multiple token types at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyTokenTypesAccess 
	extends ManyRowModelAccess[TokenType] with FilterableView[ManyTokenTypesAccess] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * names of the accessible token types
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn).map { v => v.getString }
	
	/**
	  * durations of the accessible token types
	  */
	def durations(implicit connection: Connection) = 
		pullColumn(model.durationColumn).flatMap { _.long }.map { FiniteDuration(_, TimeUnit.MINUTES) }
	
	/**
	  * refreshed type ids of the accessible token types
	  */
	def refreshedTypeIds(implicit connection: Connection) = 
		pullColumn(model.refreshedTypeIdColumn).flatMap { _.int }
	
	/**
	  * creation times of the accessible token types
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	/**
	  * are single use only of the accessible token types
	  */
	def areSingleUseOnly(implicit connection: Connection) = 
		pullColumn(model.isSingleUseOnlyColumn).map { v => v.getBoolean }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenTypeModel
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = TokenTypeFactory
	
	override def filter(additionalCondition: Condition): ManyTokenTypesAccess = 
		new ManyTokenTypesAccess.ManyTokenTypesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the are single use only of the targeted token types
	  * @param newIsSingleUseOnly A new is single use only to assign
	  * @return Whether any token type was affected
	  */
	def areSingleUseOnly_=(newIsSingleUseOnly: Boolean)(implicit connection: Connection) = 
		putColumn(model.isSingleUseOnlyColumn, newIsSingleUseOnly)
	
	/**
	  * Updates the creation times of the targeted token types
	  * @param newCreated A new created to assign
	  * @return Whether any token type was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the durations of the targeted token types
	  * @param newDuration A new duration to assign
	  * @return Whether any token type was affected
	  */
	def durations_=(newDuration: FiniteDuration)(implicit connection: Connection) = 
		putColumn(model.durationColumn, newDuration.toUnit(TimeUnit.MINUTES))
	
	/**
	  * Updates the names of the targeted token types
	  * @param newName A new name to assign
	  * @return Whether any token type was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the refreshed type ids of the targeted token types
	  * @param newRefreshedTypeId A new refreshed type id to assign
	  * @return Whether any token type was affected
	  */
	def refreshedTypeIds_=(newRefreshedTypeId: Int)(implicit connection: Connection) = 
		putColumn(model.refreshedTypeIdColumn, newRefreshedTypeId)
}

