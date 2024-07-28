package utopia.logos.database.access.single.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.logos.database.storable.word.StatementModel

import java.time.Instant

/**
  * A common trait for access points which target individual statements or similar items at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v0.2 11.3.2024
  */
trait UniqueStatementAccessLike[+A] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the delimiter that terminates this sentence. None if this sentence is not terminated 
	  * with any character.. None if no statement (or value) was found.
	  */
	def delimiterId(implicit connection: Connection) = pullColumn(model.delimiterId.column).int
	
	/**
	  * Time when this statement was first made. None if no statement (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StatementModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted statements
	  * @param newCreated A new created to assign
	  * @return Whether any statement was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * Updates the delimiter ids of the targeted statements
	  * @param newDelimiterId A new delimiter id to assign
	  * @return Whether any statement was affected
	  */
	def delimiterId_=(newDelimiterId: Int)(implicit connection: Connection) = 
		putColumn(model.delimiterId.column, newDelimiterId)
}

