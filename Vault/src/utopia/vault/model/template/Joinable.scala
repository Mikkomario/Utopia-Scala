package utopia.vault.model.template

import utopia.vault.model.immutable.Table
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Join, JoinType}

import scala.util.Try

/**
  * Common class for items that may be joined into SqlTargets
  * @author Mikko Hilpinen
  * @since 19.12.2021, v1.12
  */
trait Joinable
{
	/**
	  * @param originTables Tables from which this item is joined
	  * @param joinType Type of join to use (default = inner join)
	  * @return Join statements that connect this item to the specified origin tables.
	  *         May return an empty vector if this item is already contained within the specified origin set.
	  *         Failure if joining is impossible.
	  */
	def toJoinsFrom(originTables: Vector[Table], joinType: JoinType = Inner): Try[Vector[Join]]
}
