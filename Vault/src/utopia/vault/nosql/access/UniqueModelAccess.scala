package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.sql.Condition

/**
 * Common trait for access points which target an individual and unique model.
 * E.g. When targeting a model based on the primary row id
 * @author Mikko Hilpinen
 * @since 31.3.2021, v1.6.1
 */
trait UniqueModelAccess[+A] extends SingleModelAccess[A] with DistinctModelAccess[A, Option[A], Value]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Condition defined by this access point
	 */
	def condition: Condition
	
	
	// COMPUTED -------------------------
	
	/**
	 * @param connection Database connection (implicit)
	 * @return The index of this model in database (may be empty)
	 */
	def index(implicit connection: Connection) = table.primaryColumn match
	{
		case Some(primaryColumn) => pullColumn(primaryColumn)
		case None => Value.empty
	}
	
	
	// IMPLEMENTED  ---------------------
	
	override def globalCondition = Some(condition)
}
