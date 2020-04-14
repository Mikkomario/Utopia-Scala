package utopia.vault.nosql.access

import scala.language.implicitConversions
import utopia.vault.database.Connection
import utopia.vault.sql.{Condition, OrderBy}

object ManyAccess
{
	// IMPLICIT	--------------------------
	
	/**
	  * Auto-accesses the contents of an access point
	  * @param accessor Accessor being accessed
	  * @param connection DB Connection (implicit)
	  * @tparam A Type of accessed model
	  * @return All items that can be accessed through the specified access point
	  */
	implicit def autoAccess[A](accessor: ManyAccess[A, _])(implicit connection: Connection): Vector[A] = accessor.all
}

/**
 * A common trait for access points that return multiple items
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyAccess[+A, +Repr] extends FilterableAccess[Vector[A], Repr]
{
	// COMPUTED	--------------------
	
	/**
	 * @param connection Implicit database connection
	 * @return All items that can be accessed through this access point
	 */
	def all(implicit connection: Connection) = read(globalCondition)
	
	/**
	 * Finds all items that satisfy the specified search condition
	 * @param condition A search condition
	 * @param ordering Ordering applied (optional, None by default)
	 * @param connection Implicit database connection
	 * @return Read items
	 */
	def find(condition: Condition, ordering: Option[OrderBy] = None)(implicit connection: Connection) =
		read(Some(mergeCondition(condition)), ordering)
}
