package utopia.vault.nosql.access.many.model

import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.OrderBy
import utopia.vault.sql.SqlExtensions._

/**
 * Used for accessing multiple models based on their ids
 * @author Mikko Hilpinen
 * @since 2.4.2021, v1.6.1
 * @param ids Row ids to target
 * @param factory Factory used when reading model data
 */
class ManyIdModelAccess[+A](ids: Iterable[Value], override val factory: FromResultFactory[A],
                            override val defaultOrdering: Option[OrderBy] = None)
	extends ManyModelAccess[A]
{
	// COMPUTED --------------------------
	
	/**
	 * @return Search condition used in this access point
	 */
	def condition = table.primaryColumn.get.in(ids)
	
	
	// IMPLICIT --------------------------
	
	override def globalCondition = Some(condition)
}
