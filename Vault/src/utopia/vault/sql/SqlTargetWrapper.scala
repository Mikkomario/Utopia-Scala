package utopia.vault.sql

import utopia.vault.model.immutable.Table

/**
 * This class is used for wrapping an sql segment as a sql target. It is the responsibility of the 
 * creator of the wrapper to make sure only valid sql segments are used.
 * @author Mikko Hilpinen
 * @since 1.6.2017
 */
case class SqlTargetWrapper(private val segment: SqlSegment, databaseName: String, tables: Vector[Table])
    extends SqlTarget
{
    // IMPLEMENTED METHODS    --------------
    
    def toSqlSegment = segment
    
    override def toString = toSqlSegment.toString
}