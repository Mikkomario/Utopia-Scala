package utopia.vault.nosql.access.many

import utopia.vault.database.Connection
import utopia.vault.nosql.access.template.Access

/**
  * A common trait for access points that return multiple items
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait ManyAccess[+A] extends Access[Vector[A]]
{
	// COMPUTED	--------------------
	
	/**
	  * @param connection Implicit database connection
	  * @return All items that can be accessed through this access point
	  */
	def all(implicit connection: Connection) = read(globalCondition)
}
