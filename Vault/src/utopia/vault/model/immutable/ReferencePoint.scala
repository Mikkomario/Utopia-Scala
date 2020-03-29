package utopia.vault.model.immutable

object ReferencePoint
{
    /**
     * Finds a reference point from a table
     * @param table the table for the reference point
     * @param propertyName the property name for the associated column
     */
    def apply(table: Table, propertyName: String): Option[ReferencePoint] = table.find(propertyName).map { ReferencePoint(table, _) }
}

/**
* A reference point is simply information about a certain column in a table that contains or 
* is targeted by a reference
* @author Mikko Hilpinen
* @since 21.5.2018
  * @param table The table of this reference point
  * @param column The column of this reference point
**/
case class ReferencePoint(table: Table, column: Column)
{
    override def toString = s"${table.name}(${column.name})"
}