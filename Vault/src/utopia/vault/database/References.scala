package utopia.vault.database

import utopia.flow.generic.EnvironmentNotSetupException
import utopia.vault.model.immutable.{Column, Reference, ReferencePoint, Table}

import scala.collection.immutable.{HashMap, HashSet}

/**
 * The references object keeps track of all references between different tables in a multiple
 * databases. The object is used for accessing the reference data. The actual data must be set
 * into the object before if can be properly used
 * @author Mikko Hilpinen
 * @since 28.5.2017
 */
object References
{
    // ATTRIBUTES    ---------------------
    
    private var referenceData = HashMap[String, Set[Reference]]()
    
    
    // IMPLEMENTED  ----------------------
    
    override def toString =
    {
        if (referenceData.isEmpty)
            "No references recorded"
        else
            referenceData.keys.map { dbName => s"$dbName: [${referenceData(dbName).mkString(", ")}]" }.mkString("\n")
    }
    
    
    // OTHER METHODS    ------------------
    
    /**
     * Sets up reference data for a single database. Existing data will be preserved.
     * @param databaseName the name of the database
     * @param references a set of references in the database
     */
    def setup(databaseName: String, references: Set[Reference]) =
    {
        if (referenceData.contains(databaseName))
            referenceData += (databaseName -> (referenceData(databaseName) ++ references))
        else
            referenceData += (databaseName -> references)
    }
    
    /**
      * Sets up reference data for a single database. Each pair should contain 4 elements:
      * 1) referencing table, 2) name of the referencing property, 3) referenced table,
      * 4) name of the referenced property.
      */
    def setup(sets: Traversable[(Table, String, Table, String)]): Unit =
    {
        // Converts the tuple data into a reference set
        val references = sets.flatMap { case (table1, name1, table2, name2) => Reference(table1, name1, table2, name2) }.toSet
        references.groupBy { _.from.table.databaseName }.foreach { case (dbName, refs) => setup(dbName, refs) }
    }
    
    /**
     * Sets up reference data for a single database. Each pair should contain 4 elements:
     * 1) referencing table, 2) name of the referencing property, 3) referenced table,
     * 4) name of the referenced property.
     */
    def setup(firstSet: (Table, String, Table, String), more: (Table, String, Table, String)*): Unit =
        setup(HashSet(firstSet) ++ more)
    
    /**
     * Finds a possible reference that is made from the provided reference point (table + column)
     * @param point the starting reference point
     * @return A reference from the provided reference point. None if the point doesn't reference
     * anything
     */
    def from(point: ReferencePoint) =
    {
        checkIsSetup(point.table.databaseName)
        referenceData(point.table.databaseName).find { _.from == point }.map { _.to }
    }
    
    /**
     * Finds a possible reference that is made from the provided reference point (table + column)
     * @param table The table that contains the column
     * @param column the referencing column
     * @return A reference from the provided reference point. None if the point doesn't reference
     * anything
     */
    def from(table: Table, column: Column): Option[ReferencePoint] = from(ReferencePoint(table, column))
    
    /**
     * Finds a possible reference that is made from the provided reference point (table + column)
     * @param table The table that contains the column
     * @param columnName the name of the referencing column
     * @return A reference from the provided reference point. None if the point doesn't reference
     * anything
     */
    def from(table: Table, columnName: String): Option[ReferencePoint] = ReferencePoint(table, columnName).flatMap(from)
    
    /**
     * Finds all places where the provided reference point is referenced
     * @param point the targeted reference point
     * @return All reference points that target the specified reference point
     */
    def to(point: ReferencePoint) =
    {
        checkIsSetup(point.table.databaseName)
        referenceData(point.table.databaseName).filter { _.to == point }.map { _.from }
    }
    
    /**
     * Finds all places where the provided reference point is referenced
     * @param table the table that contains the column
     * @param column the referenced column
     * @return All reference points that target the specified reference point
     */
    def to(table: Table, column: Column): Set[ReferencePoint] = to(ReferencePoint(table, column))
    
    /**
     * Finds all places where the provided reference point is referenced
     * @param table the table that contains the column
     * @param columnName the name of the referenced column
     * @return All reference points that target the specified reference point
     */
    def to(table: Table, columnName: String): Set[ReferencePoint] =
            table.find(columnName).map { ReferencePoint(table, _) }.map(to).getOrElse(HashSet())
    
    /**
     * Finds all references made from a specific table
     */
    def from(table: Table) =
    {
        checkIsSetup(table.databaseName)
        referenceData(table.databaseName).filter { _.from.table == table }
    }
    
    /**
     * Finds all references made into a specific table
     */
    def to(table: Table) =
    {
        checkIsSetup(table.databaseName)
        referenceData(table.databaseName).filter { _.to.table == table }
    }
    
    /**
     * Finds all references between the two tables. The results contain pairings of left side
     * columns matched with right side columns. The references may go either way
     */
    def columnsBetween(left: Table, right: Table) =
    {
        Set(left.databaseName, right.databaseName).foreach(checkIsSetup)
        
        val sameOrderMatches = referenceData(left.databaseName).filter {
            ref => ref.from.table == left && ref.to.table == right }.map { ref => ref.from.column -> ref.to.column }
        
        val oppositeOrderMatches = referenceData(right.databaseName).filter {
            ref => ref.from.table == right && ref.to.table == left }.map { ref => ref.to.column -> ref.from.column }
            
       sameOrderMatches ++ oppositeOrderMatches
    }
    
    /**
      * Finds a single connection between the two tables
      * @param left Left side table
      * @param right Right side table
      * @return Left side column -> right side column. None if there wasn't a connection between the two tables
      */
    def connectionBetween(left: Table, right: Table) =
    {
        checkIsSetup(left.databaseName)
        referenceData(left.databaseName).find { ref => ref.from.table == left && ref.to.table == right }.map {
            ref => ref.from.column -> ref.to.column }.orElse
            {
                if (right.databaseName != left.databaseName)
                    checkIsSetup(right.databaseName)
                
                referenceData(right.databaseName).find { ref => ref.from.table == right && ref.to.table == left }.map {
                    ref => ref.to.column -> ref.from.column }
            }
    }
    
    /**
     * Finds all references from the left table to the right table. Only one sided references
     * are included
     */
    def fromTo(left: Table, right: Table) = from(left) intersect to(right)
    
    /**
     * Finds all references between the two tables. The reference(s) may point to either direction
     */
    def between(a: Table, b: Table) = fromTo(a, b) ++ fromTo(b, a)
    
    /**
     * Finds all tables referenced from a certain table
     */
    def tablesReferencedFrom(table: Table) = from(table).map(_.to.table)
    
    /**
     * Finds all tables that contain references to the specified table
     */
    def tablesReferencing(table: Table) = to(table).map(_.from.table)
    
    /**
     * Clears all reference data concerning a single database
     * @param databaseName Name of the database whose references should be cleared
     */
    def clear(databaseName: String) = referenceData -= databaseName
    
    private def checkIsSetup(databaseName: String) =
    {
        if (!referenceData.contains(databaseName))
        {
            throw EnvironmentNotSetupException(
                    s"References for database '$databaseName' haven't been specified")
        }
    }
}
