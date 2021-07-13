package utopia.vault.nosql.access.template

import utopia.vault.database.Connection
import utopia.vault.nosql.view.View
import utopia.vault.sql.{Condition, OrderBy}

/**
  * A common trait for all DB access points that provide data reading
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  * @tparam A The type of search results this access point produces
  */
trait Access[+A] extends View
{
	/**
	  * Performs the actual data read + possible wrapping
	  * @param condition  Final search condition used when reading data (None if no condition should be applied)
	  * @param order      The ordering applied to the data read (None if no ordering)
	  * @param connection Database connection used (implicit)
	  * @return Read data
	  */
	protected def read(condition: Option[Condition], order: Option[OrderBy] = None)(implicit connection: Connection): A
}
