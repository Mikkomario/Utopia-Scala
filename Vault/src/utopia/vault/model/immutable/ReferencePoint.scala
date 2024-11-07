package utopia.vault.model.immutable

import utopia.flow.collection.immutable.Single
import utopia.vault.database.References
import utopia.vault.model.error.NoReferenceFoundException
import utopia.vault.model.template.Joinable
import utopia.vault.sql.{Join, JoinType}

import scala.util.{Failure, Success}

object ReferencePoint
{
	/**
	  * Finds a reference point from a table
	  * @param table the table for the reference point
	  * @param propertyName the property name for the associated column
	  */
	def apply(table: Table, propertyName: String): Option[ReferencePoint] =
		table.find(propertyName).map { ReferencePoint(table, _) }
}

/**
  * A reference point is simply information about a certain column in a table that contains or
  * is targeted by a reference
  * @author Mikko Hilpinen
  * @since 21.5.2018
  * @param table The table of this reference point
  * @param column The column of this reference point
  **/
case class ReferencePoint(table: Table, column: Column) extends Joinable
{
	override def toString = s"${table.name}(${column.name})"
	
	override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType) = {
		// Primarily attempts to find a direct reference to this point
		References.to(this).find { ref => originTables.contains(ref.table) } match {
			case Some(reference) => Success(Single(Join(reference.column, table, column, joinType)))
			case None =>
				// Secondarily looks for a reference from this point
				val secondaryResult = References.from(this).flatMap { referenced =>
					// Case: Reference originates from the specified tables => Includes the referenced table
					if (originTables.contains(table))
						Some(Join(column, referenced.table, referenced.column, joinType))
					// Case: Reference points to one of the specified tables => Includes this point's table
					else if (originTables.contains(referenced.table))
						Some(Join(referenced.column, table, column, joinType))
					// Case: Unrelated reference => Won't join to it
					else
						None
				}
				secondaryResult match {
					case Some(result) => Success(Single(result))
					case None =>
						// As a tertiary option, looks for an indirect reference to this point
						References.toBiDirectionalLinkGraphFrom(table).shortestRoutesIterator
							.find { case (route, end) =>
								originTables.contains(end.value) && route.head.value._1.points.contains(this)
							} match
						{
							case Some((route, _)) =>
								Success(route.view.reverse.map { edge =>
									val (reference, isReverse) = edge.value
									(if (isReverse) reference.reverse else reference).toJoin
								}.toVector)
							case None => Failure(new NoReferenceFoundException(
								s"There are no references between $this and ${ originTables.mkString(" or ") }"))
						}
				}
		}
	}
}