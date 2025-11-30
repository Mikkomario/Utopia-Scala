package utopia.vault.model.template

import utopia.flow.collection.CollectionExtensions._
import utopia.vault.model.immutable.Table
import utopia.vault.model.template.Joinable.{_ConditionalJoinable, RenamedJoinable}
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, Join, JoinType}

import scala.util.Try

object Joinable
{
	// NESTED   -------------------------
	
	private class _ConditionalJoinable(original: Joinable, condition: Condition) extends Joinable
	{
		override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType) =
			original.toJoinsFrom(originTables, joinType).map { _.mapHead { _.where(condition) } }
	}
	
	private class RenamedJoinable(original: Joinable, alias: String) extends Joinable
	{
		override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType): Try[Seq[Join]] =
			original.toJoinsFrom(originTables, joinType).map { _.mapHead { _.copy(rightAlias = alias) } }
	}
}

/**
  * Common class for items that may be joined into SqlTargets
  * @author Mikko Hilpinen
  * @since 19.12.2021, v1.12
  */
// TODO: Add a trait that specifies the .where(Condition) -function(s)
trait Joinable extends ConditionallyJoinable[Joinable]
{
	// ABSTRACT -------------------------
	
	/**
	  * @param originTables Tables from which this item is joined
	  * @param joinType Type of join to use (default = inner join)
	  * @return Join statements that connect this item to the specified origin tables.
	  *         May return an empty sequence if this item is already contained within the specified origin set.
	  *         Failure if joining is impossible.
	  */
	def toJoinsFrom(originTables: Seq[Table], joinType: JoinType = Inner): Try[Seq[Join]]
	
	
	// IMPLEMENTED  --------------------
	
	override def onlyJoinIf(condition: Condition): Joinable = new _ConditionalJoinable(this, condition)
	
	
	// OTHER    ------------------------
	
	/**
	 * @param condition A condition that must be met for this join to occur
	 * @return A copy of this joinable item, joined only when the specified condition is met
	 */
	 // TODO: After v2.1 has been released, move this function exclusively to Join and remove the deprecation
    @deprecated("Renamed to onlyJoinIf(Condition)", "v2.1")
	def where(condition: Condition): Joinable = onlyJoinIf(condition)
	
	/**
	 * @param alias An alias given to the joined table
	 * @return A copy of this joinable item, applying the specified table alias
	 */
	def as(alias: String): Joinable = new RenamedJoinable(this, alias)
}
