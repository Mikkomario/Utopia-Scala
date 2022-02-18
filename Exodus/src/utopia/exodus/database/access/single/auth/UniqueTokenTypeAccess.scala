package utopia.exodus.database.access.single.auth

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import utopia.exodus.database.factory.auth.TokenTypeFactory
import utopia.exodus.database.model.auth.TokenTypeModel
import utopia.exodus.model.stored.auth.TokenType
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

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
	  * Id of the type of token used to acquire this token, 
		if applicable. None if no instance (or value) was found.
	  */
	def parentTypeId(implicit connection: Connection) = pullColumn(model.parentTypeIdColumn).int
	
	/**
	  * Duration that determines how long these tokens remain valid after issuing. None if these tokens don't
	  *  expire automatically.. None if no instance (or value) was found.
	  */
	def duration(implicit connection: Connection) = 
		pullColumn(model.durationColumn).long.map { FiniteDuration(_, TimeUnit.MINUTES) }
	
	/**
	  * Time when this token type was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = TokenTypeModel
	
	
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
	  * Updates the names of the targeted token types
	  * @param newName A new name to assign
	  * @return Whether any token type was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the parent type ids of the targeted token types
	  * @param newParentTypeId A new parent type id to assign
	  * @return Whether any token type was affected
	  */
	def parentTypeId_=(newParentTypeId: Int)(implicit connection: Connection) = 
		putColumn(model.parentTypeIdColumn, newParentTypeId)
}

