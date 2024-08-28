package utopia.logos.database.access.single.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.storable.text.StatementDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target individual statements or similar items at a time
  * @tparam A Type of read (statements -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait UniqueStatementAccessLike[+A, +Repr] 
	extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value] with FilterableView[Repr] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the delimiter that terminates this sentence. None if this sentence is not terminated 
	  * with any character. 
	  * None if no statement (or value) was found.
	  */
	def delimiterId(implicit connection: Connection) = pullColumn(model.delimiterId.column).int
	
	/**
	  * Time when this statement was first made. 
	  * None if no statement (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created.column).instant
	
	/**
	  * Unique id of the accessible statement. None if no statement was accessible.
	  */
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = StatementDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the delimiter ids of the targeted statements
	  * @param newDelimiterId A new delimiter id to assign
	  * @return Whether any statement was affected
	  */
	def delimiterId_=(newDelimiterId: Int)(implicit connection: Connection) = 
		putColumn(model.delimiterId.column, newDelimiterId)
}

