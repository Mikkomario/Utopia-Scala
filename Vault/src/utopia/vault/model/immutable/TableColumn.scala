package utopia.vault.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.view.template.Extender
import utopia.vault.database.References
import utopia.vault.model.template.Joinable
import utopia.vault.sql.{Join, JoinType}

import scala.util.Success

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
			// Option 1: Attempts to find a direct reference to this point
			References.referencing(this).find { ref => originTables.contains(ref.table) } match {
				// Case: Direct reference found => Joins
				case Some(reference) => Success(Single(Join(reference.column, this, joinType)))
				// Case: No direct reference
				//       => Looks whether this column references a column that's related to the origin tables
				case None =>
					References.findReferencedFrom(this).flatMap { myReferenced =>
						// Case: Reference originates from the specified tables => Includes the referenced table
						if (originTables.contains(table))
							Some(Join(column, myReferenced, joinType))
						// Case: Reference points to one of the specified tables => Includes this point's table
						else if (originTables.contains(myReferenced.table))
							Some(Join(myReferenced.column, this, joinType))
						// Case: Neither of the above
						//       => Looks for cases where this column references a column
						//          already referenced from the origin tables
						else
							References.referencing(myReferenced).findMap { referencing =>
								if (originTables.contains(referencing.table))
									Some(Join(referencing.column, this, joinType))
								else
									None
							}
					} match {
						case Some(result) => Success(Single(result))
						// Case: No direct references to or from this column
						//       => Attempts to join this column's table instead
						case None => table.toJoinsFrom(originTables, joinType)
					}
			}
	}
}
