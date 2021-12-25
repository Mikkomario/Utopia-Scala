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
	  * @return A join statement applicable to that context. Failure if joining is impossible.
	  */
	def toJoinFrom(originTables: Vector[Table], joinType: JoinType = Inner): Try[Join]
}
