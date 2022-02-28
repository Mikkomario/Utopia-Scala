package utopia.vault.nosql.access.template

import utopia.vault.database.Connection
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.view.View
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, JoinType, OrderBy}

/**
  * A common trait for all DB access points that provide data reading
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  * @tparam A The type of search results this access point produces
  */
trait Access[+A] extends View
{
	// ABSTRACT ---------------------------------
	
	/**
	  * Performs the actual data read + possible wrapping
	  * @param condition  Final search condition used when reading data (None if no condition should be applied)
	  * @param order      The ordering applied to the data read (None if no ordering)
	  * @param joins Targets to join to the query (not selected, however)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Database connection used (implicit)
	  * @return Read data
	  */
	protected def read(condition: Option[Condition] = None, order: Option[OrderBy] = None,
	                   joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	                  (implicit connection: Connection): A
	
	
	// OTHER    ----------------------------------
	
	/**
	  * Finds all items accessible through this access point that satisfy the specified (additional) search condition
	  * @param condition  A search condition
	  * @param order Ordering applied (optional, default = factory default ordering)
	  * @param joins Targets to join to the query (not selected, however)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Implicit database connection
	  * @return Read items
	  */
	def find(condition: Condition, order: Option[OrderBy] = None, joins: Seq[Joinable] = Vector(),
	         joinType: JoinType = Inner)
	        (implicit connection: Connection) =
		read(Some(mergeCondition(condition)), order, joins, joinType)
}
