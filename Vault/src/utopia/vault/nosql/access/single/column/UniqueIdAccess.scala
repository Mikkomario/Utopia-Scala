package utopia.vault.nosql.access.single.column

import utopia.vault.database.Connection
import utopia.vault.sql.Condition

import scala.language.implicitConversions

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
	
	override def accessCondition = Some(condition)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param connection Implicit database connection
	  * @return The unique id accessed through this access point. None if no id was found.
	  */
	def pull(implicit connection: Connection) = read(accessCondition)
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there exists an id accessible from this access point
	  */
	def isDefined(implicit connection: Connection) = nonEmpty
}
