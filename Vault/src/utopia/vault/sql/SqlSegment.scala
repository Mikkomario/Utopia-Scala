package utopia.vault.sql

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.NotEmpty
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Result, Table, TableUpdateEvent}

object SqlSegment
{
    /**
     * An empty sql segment. Some functions may return this in case of no-op
     */
    val empty = SqlSegment("")
    
    /**
     * Combines a number of sql segments together, forming a single longer sql segment
     * @param segments the segments that are combined
     * @param sqlReduce the reduce function used for appending the sql strings together.
      *                  By default just adds a whitespace between the strings
     */
    def combine(segments: Seq[SqlSegment])(sqlReduce: Pair[String] => String = { _.mkString(" ") }) = {
        if (segments.isEmpty)
            SqlSegment.empty
        else {
            val sql = segments.view.map { _.sql }.reduceLeft { (l, r) => sqlReduce(Pair(l, r)) }
            val databaseName = segments.view.flatMap { _.databaseName }.headOption
            val eventFunctions = segments.flatMap { _.events }
            val eventsFunction = {
                if (eventFunctions.isEmpty)
                    None
                else if (eventFunctions.size == 1)
                    Some(eventFunctions.head)
                else
                    Some({ result: Result => eventFunctions.flatMap { _(result) } })
            }
            
            SqlSegment(sql, segments.flatMap { _.values }, databaseName, 
                    segments.flatMap { _.targetTables }.toSet, eventsFunction, segments.exists { _.isSelect },
                    segments.exists { _.generatesKeys })
        }
    }
}

/**
 * Sql Segments can be combined to form sql statements. Some may contain value assignments too.
 * @author Mikko Hilpinen
 * @since 12.3.2017
  * @constructor Creates a new sql segment from specified sql with proper metadata (values, database name,
  *              tables and whether this sql represents a selection statement)
 * @param sql The sql string representing the segment. Each of the segment's values is indicated with a 
 * '?' character.
 * @param values The values that will be inserted to this segment when it is used. Each '?' in the sql will 
 * be replaced with a single value. Empty values will be interpreted as NULL.
  * @param databaseName The name of the targeted database
  * @param targetTables The tables data is <b>read</b> from
  * @param events A function for generating table update events once this segment has been executed.
  *               None if no events need to be generated.
  * @param isSelect Whether this segment represents a select query
  * @param generatesKeys Whether this statement will generate new keys (= rows with auto-increment index) to the database
 */
case class SqlSegment(sql: String, values: Seq[Value] = Empty, databaseName: Option[String] = None,
                      targetTables: Set[Table] = Set(), events: Option[Result => Seq[TableUpdateEvent]] = None,
                      isSelect: Boolean = false, generatesKeys: Boolean = false)
{
    // COMPUTED PROPERTIES    -----------
    
    override def toString = sql
    
    /**
     * A textual description of this segment. Contains the sql string as well as the included values
     */
    def description = {
        val valuesPart = NotEmpty(values) match {
            case Some(values) => s"\nValues: ${ values.view.map { _.description }.mkString(", ") }"
            case None => ""
        }
        s"Sql: $toString$valuesPart"
    }
    
    /**
     * Whether the segment is considered to be empty (no-op)
     */
    def isEmpty = sql.isEmpty
    
    
    // OTHER    ---------------------
    
    /**
      * @param f A mapping function for this statement's sql string
      * @return A mapped copy of this segment
      */
    def mapSql(f: String => String) = copy(sql = f(sql))
    
    /**
     * Combines two sql segments to create a single, larger segment. A whitespace character is 
     * added between the two sql segments.
     */
    def +(other: SqlSegment) = mergeWith(other) { (my, their) => s"$my $their" }
    /**
     * Appends this sql segment with an sql string. 
     * The new string will be added to the end of this segment. Adds a whitespace character 
     * between the two sql segments.
     */
    def +(sql: String) = copy(sql = s"${ this.sql } $sql")
    /**
     * Combines these two segments, but if the other segment is empty, skips it
     * @param other Another sql segment which may also be empty
     * @return A combination of these two sql segments
     */
    def +(other: Option[SqlSegment]): SqlSegment = other.map { this + _ }.getOrElse(this)
    /**
     * Combines these two segments, but if the other segment is empty, skips it
     * @param other Another segment (converted implicitly)
     * @param convertToSegment An implicit conversion to segment
     * @tparam S Type of converted item
     * @return A combination of these two sql segments
     */
    def +[S](other: Option[S])(implicit convertToSegment: S => SqlSegment): SqlSegment =
        this + other.map(convertToSegment)
    
    /**
      * @param others A number of new sql segments to append
      * @return Copy of this segment with the specified segments appended to it
      */
    def ++(others: IterableOnce[SqlSegment]) = others.iterator.foldLeft(this) { _ + _ }
    
    /**
     * Prepends this sql segment with an sql string. The new string will be added to the beginning 
     * of this segment. A whitespace character is added between the two segments.
     */
    def prepend(sql: String) = copy(sql = s"$sql ${ this.sql }")
    
    /**
      * Combines this segment with another sql segment
      * @param other Another sql segment
      * @param mergeSql A function for merging the sql statement strings together.
      *                 Accepts the sql from this statement and the sql from the other statement.
      *                 Returns a single sql statement.
      * @return A merged sql statement
      */
    def mergeWith(other: SqlSegment)(mergeSql: (String, String) => String) = {
        if (other.isEmpty)
            this
        else if (isEmpty)
            other
        else {
            val eventFunction = events match {
                case Some(myFunction) =>
                    other.events match {
                        case Some(theirFunction) =>
                            Some({ result: Result => myFunction(result) ++ theirFunction(result) })
                        case None => Some(myFunction)
                    }
                case None => other.events
            }
            SqlSegment(mergeSql(sql, other.sql), values ++ other.values,
                databaseName orElse other.databaseName, targetTables ++ other.targetTables, eventFunction,
                isSelect || other.isSelect, generatesKeys || other.generatesKeys)
        }
    }
    
    /**
     * Runs this sql segment / statement over a database connection
     */
    def execute()(implicit connection: Connection) = connection(this)
}