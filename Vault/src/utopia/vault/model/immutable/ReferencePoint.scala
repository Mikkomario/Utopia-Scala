package utopia.vault.model.immutable

import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType

@deprecated("Deprecated for removal. Replaced with TableColumn", "v1.22")
object ReferencePoint
{
	/**
	  * Finds a reference point from a table
	  * @param table the table for the reference point
	  * @param propertyName the property name for the associated column
	  */
	def apply(table: Table, propertyName: String): Option[TableColumn] =
		table.find(propertyName).map { ReferencePoint(table, _) }
		
	def apply(table: Table, column: Column) = TableColumn(table, column)
}

/**
  * A reference point is simply information about a certain column in a table that contains or
  * is targeted by a reference
  * @author Mikko Hilpinen
  * @since 21.5.2018
  * @param table The table of this reference point
  * @param column The column of this reference point
  **/
@deprecated("Deprecated for removal. Replaced with TableColumn", "v1.22")
case class ReferencePoint(table: Table, column: Column) extends Joinable
{
	override def toString = s"${table.name}(${column.name})"
	
	override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType) =
		TableColumn(table, column).toJoinsFrom(originTables, joinType)
}