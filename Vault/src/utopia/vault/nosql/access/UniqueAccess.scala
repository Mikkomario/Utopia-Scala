package utopia.vault.nosql.access

import scala.language.implicitConversions
import utopia.vault.database.Connection
import utopia.vault.sql.{Condition, Limit, Select, SqlTarget, Where}

/**
 * A common trait for access points that provide access to an individual unique item. Eg. in searches based on a
 * unique key or primary key.
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait UniqueAccess[+A] extends Access[Option[A]]
{
	// ABSTRACT	---------------------
	
	/**
	 * @return The search condition used by this access point globally. Should limit the results to a single row in DB.
	 */
	def condition: Condition
	
	/**
	 * @return The whole range of selection & condition targets used in this access
	 */
	def target: SqlTarget
	
	
	// IMPLEMENTED	-----------------
	
	override def globalCondition: Some[Condition] = Some(condition)
	
	
	// COMPUTED	---------------------
	
	/**
	 * @param connection Implicit database connection
	 * @return The unique item accessed through this access point. None if no item was found.
	 */
	def get(implicit connection: Connection) = read(globalCondition)
	
	/**
	 * @param connection DB Connection (implicit)
	 * @return Whether there exists an item accessible from this access point
	 */
	def isDefined(implicit connection: Connection) = connection(Select.nothing(target) + Where(condition) + Limit(1)).nonEmpty
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there doesn't exist a single row accessible from this access point
	  */
	def isEmpty(implicit connection: Connection) = !isDefined
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether the item accessible from this accessor satisfies the specified condition
	  */
	def exists(additionalCondition: Condition)(implicit connection: Connection) = connection(
		Select.nothing(target) + Where(mergeCondition(additionalCondition)) + Limit(1)).nonEmpty
}

object UniqueAccess
{
	// IMPLICITS	---------------------------
	
	/**
	  * Auto-accesses specified accessor's unique result
	  * @param accessor An accessor
	  * @param connection DB Connection (implicit)
	  * @tparam A Type of accessed item
	  * @return Accessor's unique result from DB. None if no result was found.
	  */
	def autoAccess[A](accessor: UniqueAccess[A])(implicit connection: Connection): Option[A] = accessor.get
}