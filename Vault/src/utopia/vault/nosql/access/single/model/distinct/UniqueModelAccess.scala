package utopia.vault.nosql.access.single.model.distinct

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
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
	
	
	// IMPLEMENTED  ---------------------
	
	override def accessCondition = Some(condition)
}
