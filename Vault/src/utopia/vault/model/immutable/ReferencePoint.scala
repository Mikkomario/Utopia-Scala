package utopia.vault.model.immutable

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
    
    override def toJoinFrom(originTables: Vector[Table], joinType: JoinType) =
        References.to(this).find { ref => originTables.contains(ref.table) } match {
            case Some(reference) => Success(Join(reference.column, table, column, joinType))
            case None =>
                if (originTables.contains(table))
                    References.from(this) match {
                        case Some(target) => Success(Join(column, target.table, target.column, joinType))
                        case None => Failure(new NoReferenceFoundException(s"$this doesn't refer to any table"))
                    }
                else
                    Failure(new NoReferenceFoundException(s"No reference was found between tables ${
                        originTables.map { _.name }.mkString(", ")} and $this."))
        }
}