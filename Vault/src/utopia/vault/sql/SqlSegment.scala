package utopia.vault.sql

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.NotEmpty
import utopia.flow.util.EitherExtensions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Result, Table, TableUpdateEvent}

object SqlSegment
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * An empty SQL segment. Some functions may return this in case of no-op
	 */
	val empty = SqlSegment("")
	
	
	// OTHER    ------------------------
	
	/**
	 * Combines n SQL segments together
	 * @param separator Separator placed between each SQL segment
	 * @param parts Parts that will form this segment.
	 *              Left if mere String SQL statements, Right if they contain other information.
	 * @return A combined SQL segment
	 */
	def combine(separator: String, parts: Either[String, SqlSegment]*) = {
		parts.emptyOneOrMany match {
			case None => SqlSegment.empty
			case Some(Left(only)) => only.rightOrMap { apply(_) }
			case Some(Right(parts)) =>
				val sql = parts.iterator.map { _.leftOrMap { _.sql } }.mkString(separator)
				val sqlParts = parts.flatMap { _.toOption }
				val databaseName = sqlParts.findMap { _.databaseName }
				
				val eventsFunction = sqlParts.flatMap { _.events }.emptyOneOrMany
					// Joins the event functions together, if appropriate
					.map { _.leftOrMap { functions => { result: Result => functions.flatMap { _(result) } } } }
				
				SqlSegment(sql, sqlParts.flatMap { _.values }, databaseName,
					sqlParts.iterator.flatMap { _.targetTables }.toSet, eventsFunction,
					isSelect = sqlParts.exists { _.isSelect }, generatesKeys = sqlParts.exists { _.generatesKeys })
		}
	}
	/**
	 * Combines n SQL segments together
	 * @param separator Separator placed between each SQL segment
	 * @param parts Parts that will form this segment.
	 * @return A combined SQL segment
	 */
	def concat(separator: String, parts: IterableOnce[SqlSegment]) =
		combine(separator, parts.iterator.map { Right(_) }.toOptimizedSeq: _*)
	
	/**
	 * Combines a number of SQL segments together, forming a single longer SQL segment
	 * @param segments the segments that are combined
	 * @param sqlReduce the reduce function used for appending the SQL strings together.
	 *                  By default, just adds a whitespace between the strings
	 */
	@deprecated("Deprecated for removal. Please use the new version of this function instead", "v2.1")
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
 * Sql Segments can be combined to form SQL statements. Some may contain value assignments too.
 * @author Mikko Hilpinen
 * @since 12.3.2017
 * @constructor Creates a new SQL segment from specified SQL with proper metadata (values, database name,
 *              tables and whether this SQL represents a selection statement)
 * @param sql The SQL string representing the segment. Each of the segment's values is indicated with a
 * '?' character.
 * @param values The values that will be inserted to this segment when it is used. Each '?' in the SQL will
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
	
	/**
	 * @return A copy of this segment placed within parentheses
	 */
	def withinParentheses = mapSql { sql => s"($sql)" }
	
	
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
	def +(sql: String) = mapSql { mySql => s"$mySql $sql" }
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
	 * @param other Another SQL segment
	 * @param separator A separator to place between these segments (including whitespaces)
	 * @return An appended copy of this segment
	 */
	def append(other: SqlSegment, separator: String) =
		mergeWith(other) { (my, their) => s"$my$separator$their" }
	/**
	 * Prepends this sql segment with an sql string. The new string will be added to the beginning
	 * of this segment. A whitespace character is added between the two segments.
	 */
	def prepend(sql: String) = mapSql { mySql => s"$sql $mySql" }
	
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