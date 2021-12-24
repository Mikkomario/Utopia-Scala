package utopia.vault.nosql.access.single.model.distinct

import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctReadModelAccess
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps

/**
  * Used for accessing the latest model within a table or a sub-group
  * @author Mikko Hilpinen
  * @since 17.6.2021, v1.8
  */
trait LatestModelAccess[+A] extends SingleRowModelAccess[A] with DistinctReadModelAccess[A, Option[A], Value]
{
	// COMPUTED ----------------------------
	
	// This access point requires a timestamp-based factory
	override def factory: FromRowFactoryWithTimestamps[A]
}
