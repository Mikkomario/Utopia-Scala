package utopia.logos.database.access.many.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView
import utopia.logos.database.storable.word.StatementModel

import java.time.Instant

/**
  * A common trait for access points which target multiple statements or similar instances at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v0.2 11.3.2024
  */
trait ManyStatementsAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
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
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = StatementModel
	
	/**
	 * @return Access to statements that don't specify any delimiter at the end
	 */
	def withoutDelimiter = filter(model.delimiterId.column.isNull)
	
	
	// OTHER	--------------------
	
	/**
	 * @param delimiterId Id of the targeted delimiter
	 * @return Access to statements that end with the specified delimiter
	 */
	def endingWith(delimiterId: Int) = filter(model.withDelimiterId(delimiterId).toCondition)
	/**
	 * @param delimiterId Id of the targeted delimiter.
	 *                    None if the targeted statements shouldn't end with any delimiter.
	 * @return Access to statements that end with the specified delimiter
	 */
	def endingWith(delimiterId: Option[Int]): Repr = delimiterId match {
		case Some(id) => endingWith(id)
		case None => withoutDelimiter
	}
	
	/**
	  * Updates the creation times of the targeted statements
	  * @param newCreated A new created to assign
	  * @return Whether any statement was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created.column, newCreated)
	
	/**
	  * Updates the delimiter ids of the targeted statements
	  * @param newDelimiterId A new delimiter id to assign
	  * @return Whether any statement was affected
	  */
	def delimiterIds_=(newDelimiterId: Int)(implicit connection: Connection) = 
		putColumn(model.delimiterId.column, newDelimiterId)
}

