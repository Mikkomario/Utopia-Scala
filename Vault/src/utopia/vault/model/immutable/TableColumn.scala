package utopia.vault.model.immutable

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.template.Extender
import utopia.vault.database.References
import utopia.vault.model.error.NoReferenceFoundException
import utopia.vault.model.template.Joinable
import utopia.vault.sql.{Join, JoinType}

import scala.util.{Failure, Success}

/**
  * Wraps a column, including all the parent table's information
  * @author Mikko Hilpinen
  * @since 30.06.2025, v1.22
  */
case class TableColumn(table: Table, column: Column) extends Extender[Column] with Joinable
{
	// IMPLEMENTED  ------------------------
	
	override def wrapped: Column = column
	
	override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType) = {
		// Case: This point is already included in the original tables => Checks whether an outward join is requested
		if (originTables.contains(table))
			References.findReferencedFrom(this) match {
				// Case: Outward join is possible
				case Some(referred) =>
					// Case: Other end is already joined => No additional joins are necessary
					if (originTables.contains(referred.table))
						Success(Empty)
					// Case: Other end not yet joined => Joins it
					else
						Success(Single(Join(column, referred, joinType)))
				
				// Case: This is not a referring column => No joins are needed
				case None => Success(Empty)
			}
		// Case: Not yet part of the target => Prepares a join, if possible
		else
			// Primarily attempts to find a direct reference to this point
			References.referencing(this).find { ref => originTables.contains(ref.table) } match {
				// Case: Direct reference found => Joins
				case Some(reference) => Success(Single(Join(reference.column, this, joinType)))
				// Case: No direct reference => Looks for a reference from this point
				case None =>
					val secondaryResult = References.findReferencedFrom(this).flatMap { referenced =>
						// Case: Reference originates from the specified tables => Includes the referenced table
						if (originTables.contains(table))
							Some(Join(column, referenced, joinType))
						// Case: Reference points to one of the specified tables => Includes this point's table
						else if (originTables.contains(referenced.table))
							Some(Join(referenced.column, this, joinType))
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
									originTables.contains(end.value) && route.head.value._1.ends.contains(this)
								} match
							{
								case Some((route, _)) =>
									Success(route.view.reverse
										.map { edge =>
											val (reference, isReverse) = edge.value
											(if (isReverse) reference.reverse else reference).toJoin
										}
										.toOptimizedSeq)
								
								// Case: Not possible to join to this reference
								case None => Failure(new NoReferenceFoundException(
									s"There are no references between $this and ${ originTables.mkString(" or ") }"))
							}
					}
			}
	}
}
