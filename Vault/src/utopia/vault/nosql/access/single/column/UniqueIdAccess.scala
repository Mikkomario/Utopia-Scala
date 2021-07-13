package utopia.vault.nosql.access.single.column

import utopia.vault.database.Connection
import utopia.vault.sql.{Condition, Exists}

import scala.language.implicitConversions

object UniqueIdAccess
{
	// Implicitly accesses the unique content
	implicit def autoAccess[A](idAccess: UniqueIdAccess[A])(implicit connection: Connection): Option[A] = idAccess.pull
}

/**
  * Common trait for id access points which point to a unique id
  * @author Mikko Hilpinen
  * @since 9.4.2021, v1.7
  * @tparam ID Type of the id accessed through this access point
  */
trait UniqueIdAccess[+ID] extends SingleIdAccess[ID]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Search condition used in this access point
	  */
	def condition: Condition
	
	
	// IMPLEMENTED   -------------------------
	
	override def globalCondition = Some(condition)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param connection Implicit database connection
	  * @return The unique id accessed through this access point. None if no id was found.
	  */
	def pull(implicit connection: Connection) = read(globalCondition)
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there exists an id accessible from this access point
	  */
	def isDefined(implicit connection: Connection) = Exists(target, condition)
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there doesn't exist a single row accessible from this access point
	  */
	def isEmpty(implicit connection: Connection) = !isDefined
}
