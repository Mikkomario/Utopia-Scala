package utopia.logos.database.access.many.text.statement

import utopia.flow.collection.immutable.IntSet
import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.storable.text.StatementDbModel
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

/**
  * A common trait for access points which target multiple statements or similar instances at a time
  * @tparam A Type of read (statements -like) instances
  * @tparam Repr Type of this access point
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait ManyStatementsAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Access to statements that don't specify any delimiter at the end
	  */
	def withoutDelimiter = filter(model.delimiterId.column.isNull)
	
	/**
	  * delimiter ids of the accessible statements
	  */
	def delimiterIds(implicit connection: Connection) = 
		pullColumn(model.delimiterId.column).flatMap { v => v.int }
	/**
	  * creation times of the accessible statements
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.created.column).map { v => v.getInstant }
	/**
	  * Unique ids of the accessible statements
	  */
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	protected def model = StatementDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param delimiterId delimiter id to target
	  * @return Copy of this access point that only includes statements with the specified delimiter id
	  */
	def endingWith(delimiterId: Int) = filter(model.delimiterId.column <=> delimiterId)
	/**
	  * @param delimiterId Id of the targeted delimiter.
	  * None if the targeted statements shouldn't end with any delimiter.
	  * @return Access to statements that end with the specified delimiter
	  */
	def endingWith(delimiterId: Option[Int]): Repr = delimiterId match {
		case Some(id) => endingWith(id)
		case None => withoutDelimiter
	}
	/**
	  * @param delimiterIds Targeted delimiter ids
	  * @return Copy of this access point that only includes statements where delimiter id is within the specified
	  *  value set
	  */
	def endingWithDelimiters(delimiterIds: IterableOnce[Int]) =
		filter(model.delimiterId.column.in(IntSet.from(delimiterIds)))
	
	/**
	  * Updates the delimiter ids of the targeted statements
	  * @param newDelimiterId A new delimiter id to assign
	  * @return Whether any statement was affected
	  */
	def delimiterIds_=(newDelimiterId: Int)(implicit connection: Connection) = 
		putColumn(model.delimiterId.column, newDelimiterId)
}

