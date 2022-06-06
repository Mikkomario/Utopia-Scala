package utopia.vault.nosql.access.single.model

import utopia.vault.database.Connection
import utopia.vault.nosql.view.ChronoRowFactoryView

/**
  * Used for accessing individual models where each row represents a single item and where each row is timestamped
  * @author Mikko Hilpinen
  * @since 18.2.2022, v1.12.1
  * @tparam A Type of models read
  * @tparam Sub Type of the filtered copy of this view
  */
trait SingleChronoRowModelAccess[+A, +Sub] extends SingleRowModelAccess[A] with ChronoRowFactoryView[A, Sub]
{
	/**
	  * @param connection Implicit DB Connection
	  * @return The latest accessible item
	  */
	def latest(implicit connection: Connection) = maxBy(factory.creationTimeColumn)
	/**
	  * @param connection Implicit DB Connection
	  * @return The earliest accessible item
	  */
	def earliest(implicit connection: Connection) = minBy(factory.creationTimeColumn)
}
