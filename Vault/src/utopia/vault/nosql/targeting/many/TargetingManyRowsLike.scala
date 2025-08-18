package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.{HasInclusiveEnds, NumericSpan}
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.enumeration.SelectTarget.{Columns, SingleColumn}
import utopia.vault.model.immutable.{Column, Row, Table, TableColumn}
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.read.parse.ParseRows
import utopia.vault.nosql.read.{DbReader, DbRowReader}
import utopia.vault.sql.JoinType
import utopia.vault.sql.JoinType.Inner

object TargetingManyRowsLike
{
	// EXTENSIONS   --------------------------
	
	implicit class RecursiveTargetingManyRows[T <: TargetingManyRowsLike[A, T, _], +A](val t: T) extends AnyVal
	{
		def slicesIterator(sliceLength: Int)(implicit connection: Connection) =
			Iterator.iterate(0) { _ + sliceLength }
				.map { start => t.slice(NumericSpan(start, start + sliceLength - 1)).pull }
				.takeTo { _.hasSize < sliceLength }
		
		def slicedIterator(sliceLength: Int)(implicit connection: Connection) =
			slicesIterator(sliceLength).flatten
	}
}

/**
  * Common trait for extendable & filterable access points which fetch multiple row-specific items with each query
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait TargetingManyRowsLike[+A, +Repr, +One] extends TargetingManyLike[A, Repr, One]
{
	// ABSTRACT --------------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return The only accessible item. None if no items were accessible, or if multiple items were accessible.
	 */
	def pullOnly(implicit connection: Connection): Option[A]
	
	/**
	  * @param n Maximum number of rows to include
	  * @return A copy of this access point, limited to that number of rows
	  */
	def take(n: Int): Repr
	/**
	  * @param n Number of rows to exclude from the beginning
	  * @return A copy of this access point, where 'n' first rows have been excluded
	  */
	def drop(n: Int): Repr
	/**
	  * @param range Targeted range of row-indices (0-based)
	  * @return A copy of this access, limited to the specified range of rows
	  */
	def slice(range: HasInclusiveEnds[Int]): Repr
	
	/**
	  * Accesses the accessible items in a streamed fashion
	  * @param f A function that receives an iterator of the accessible items.
	  *          The applicable result stream is kept open during this function call.
	  *          The iterator must not be used outside this function call.
	  * @param connection Implicit DB connection
	  * @tparam B Type of function results
	  * @return Result of 'f'
	  */
	def stream[B](f: Iterator[A] => B)(implicit connection: Connection): B
	
	/**
	  * Pulls all accessible data, including an additional column.
	  * If the specified column is not part of this access point's [[target]], joins to include it.
	  * Assumes that only 0-1 column value(s) map to any single row.
	  * @param column Column to include
	  * @param map A function for mapping individual pulled column values
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapped column values
	  * @return Accessible items, including mapped column values
	  */
	def pullWith[B](column: TableColumn)(map: Value => B)(implicit connection: Connection): Seq[(A, B)]
	/**
	  * Pulls all accessible data, including additional columns.
	  * If some of the specified columns are not part of this access point's [[target]], joins to include them.
	  * Assumes that only 0-1 column value(s) map to any single row.
	  * @param columns Columns to include
	  * @param map A function for mapping pulled column values.
	  *            Receives a sequence of values matching the specified columns, representing a single pulled row.
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapped column values
	  * @return Accessible items, including mapped column values
	  */
	def pullWith[B](columns: Seq[TableColumn])(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)]
	/**
	  * Pulls all accessible data, including additional column values.
	  * If the specified column is not part of this access point's [[target]], joins to include it.
	  * Assumes that multiple column value(s) may map/join to individual row (i.e. that one-to-many connection is used).
	  * @param column Column to include
	  * @param map A function for mapping column values.
	  *            Each set of values is connected to a single primary row / item.
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapped column values
	  * @return Accessible items, including mapped column values
	  */
	def pullWithMany[B](column: TableColumn)(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)]
	/**
	  * Pulls all accessible data, including values from multiple additional columns.
	  * If some of the specified columns are not part of this access point's [[target]], joins to include them.
	  * Assumes that multiple column value(s) may map/join to individual row (i.e. that one-to-many connection is used).
	  * @param columns Columns to include
	  * @param map A function for mapping column values.
	  *            Receives a sequence of value-sequences, where the length and ordering matches the specified
	  *            'columns' sequence.
	  *            Only receives values that are connected to an individual primary row / item.
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapped column values
	  * @return Accessible items, including mapped column values
	  */
	def pullWithMany[B](columns: Seq[TableColumn])(map: Seq[Seq[Value]] => B)(implicit connection: Connection): Seq[(A, B)]
	
	/**
	  * Extends this access point to include data from additional tables.
	  * Assumes that each accessed row / item only joins to 0-1 rows in the included tables
	  * (i.e. that only one-to-one connections are present)
	  * @param tables Tables from which data is *read*
	  * @param exclusiveColumns If the additionally read data should be limited to specific columns
	  *                         (for one or more tables), specify those columns here.
	  *                         If left empty, all columns in 'tables' are pulled.
	  * @param bridges Joins that should be performed to reach 'tables'.
	  *                      No data is read from these joined tables, but they are used for forming an
	  *                      integral target.
	  *                      Default = empty, which is appropriate when no joins beside 'tables' need to be performed.
	  * @param joinType Type of joins applied. Default = [[Inner]].
	  * @param f A function which receives a parsed item with a pulled row, and combines them to form a new item.
	  * @tparam B Type of mapping / extended parsing results.
	  * @return A copy of this access which performs the necessary joins, includes the extended read target,
	  *         and performs the specified mapping.
	  */
	def extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column] = Empty, bridges: Seq[Joinable] = Empty,
	                joinType: JoinType = Inner)
	               (f: (A, Row) => Option[B]): TargetingManyRows[B]
	/**
	  * Extends this access point to include data from additional tables.
	  * Assumes that a single primarily accessed row / item may be joined to multiple rows in the targeted tables,
	  * i.e. that one-to-many connections are present.
	  * @param tables Tables from which data is *read*
	  * @param exclusiveColumns If the additionally read data should be limited to specific columns
	  *                         (for one or more tables), specify those columns here.
	  *                         If left empty, all columns in 'tables' are pulled.
	  * @param bridges Joins that should be performed to reach 'tables'.
	  *                      No data is read from these joined tables, but they are used for forming an
	  *                      integral target.
	  *                      Default = empty, which is appropriate when no joins beside 'tables' need to be performed.
	  * @param joinType Type of joins applied. Default = [[Inner]].
	  * @param f A function which receives all pulled data, and combines it to form the extended data-set.
	  *          Each entry in the input represents a single primary item. All rows associated with that single item
	  *          are also included as input.
	  * @tparam B Type of mapping / extended parsing results.
	  * @return A copy of this access which performs the necessary joins, includes the extended read target,
	  *         and performs the specified mapping.
	  */
	def extendToMany[B](tables: Seq[Table], exclusiveColumns: Seq[Column] = Empty, bridges: Seq[Joinable] = Empty,
	                    joinType: JoinType = Inner)
	                   (f: Iterator[(A, Seq[Row])] => IterableOnce[B]): TargetingMany[B]
	
	
	// OTHER    --------------------------
	
	/**
	  * Pulls all accessible data, including additional columns.
	  * If some of the specified columns are not part of this access point's [[target]], joins to include them.
	  * Assumes that only 0-1 column value(s) map to any single row.
	  * @param map A function for mapping pulled column values.
	  *            Receives a sequence of values matching the specified columns, representing a single pulled row.
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapped column values
	  * @return Accessible items, including mapped column values
	  */
	def pullWith[B](firstColumn: TableColumn, secondColumn: TableColumn, moreColumns: TableColumn*)(map: Seq[Value] => B)
	               (implicit connection: Connection): Seq[(A, B)] =
		pullWith[B](Pair(firstColumn, secondColumn) ++ moreColumns)(map)
	/**
	  * Pulls all accessible data, including values from multiple additional columns.
	  * If some of the specified columns are not part of this access point's [[target]], joins to include them.
	  * Assumes that multiple column value(s) may map/join to individual row (i.e. that one-to-many connection is used).
	  * @param map A function for mapping column values.
	  *            Receives a sequence of value-sequences, where the length and ordering matches the specified
	  *            'columns' sequence.
	  *            Only receives values that are connected to an individual primary row / item.
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapped column values
	  * @return Accessible items, including mapped column values
	  */
	def pullWithMany[B](firstColumn: TableColumn, secondColumn: TableColumn, moreColumns: TableColumn*)(map: Seq[Seq[Value]] => B)
	                   (implicit connection: Connection): Seq[(A, B)] =
		pullWithMany[B](Pair(firstColumn, secondColumn) ++ moreColumns)(map)
	
	/**
	  * Pulls all accessible data, grouping it by values read from an additional column.
	  * If the specified column is not included in this access point's [[target]], joins it.
	  * @param column Column by which read items are grouped
	  * @param map A mapping function for parsing the specified column's individual values
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapping results
	  * @return All accessible items, grouped by parsed column values.
	 *         Specifies an empty default value.
	  */
	def groupBy[B](column: TableColumn)(map: Value => B)(implicit connection: Connection) =
		pullWith(column)(map).groupMap { _._2 } { _._1 }.withDefaultValue(Empty)
	/**
	  * Pulls all accessible data, grouping it by values read from additional columns.
	  * If any of the specified columns are not included in this access point's [[target]], joins them.
	  * @param columns Columns by which read items are grouped
	  * @param map A mapping function for parsing the specified columns' individual values.
	  *            Receives values representing a single row / item.
	  *            The value sequence matches 'columns' in order and length.
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapping results
	  * @return All accessible items, grouped by parsed column values. Specifies an empty default value.
	  */
	def groupBy[B](columns: Seq[TableColumn])(map: Seq[Value] => B)(implicit connection: Connection) =
		pullWith(columns)(map).groupMap { _._2 } { _._1 }.withDefaultValue(Empty)
	/**
	  * Pulls all accessible data, grouping it by values read from additional columns.
	  * If any of the specified columns are not included in this access point's [[target]], joins them.
	  * @param map A mapping function for parsing the specified columns' individual values.
	  *            Receives values representing a single row / item.
	  *            The value sequence matches 'columns' in order and length.
	  * @param connection Implicit DB connection
	  * @tparam B Type of mapping results
	  * @return All accessible items, grouped by parsed column values. Specifies an empty default value.
	  */
	def groupBy[B](firstColumn: TableColumn, secondColumn: TableColumn, moreColumns: TableColumn*)(map: Seq[Value] => B)
	              (implicit connection: Connection): Map[B, Seq[A]] =
		groupBy(Pair(firstColumn, secondColumn) ++ moreColumns)(map)
	
	/**
	 * Accesses additional data and combines it with the accessible items in a one-to-one fashion.
	 * @param reader Reader used for acquiring the right-side items
	 * @param bridges Bridging joins to apply before joining the 'reader's tables. Default = empty.
	 * @param combine A function which combines a primary item and a right-side item
	 * @tparam R Type of right-side items
	 * @tparam B Type of 'combine' results
	 * @return Access to combined items
	 */
	def combineWith[R, B](reader: DbRowReader[R], bridges: Seq[Joinable] = Empty)
	                     (combine: (A, R) => B) =
		extendTo[B](reader.tables, exclusiveColumnsFrom(reader.selectTarget), bridges) { (left, row) =>
			reader.tryParse(row).map { combine(left, _) }
		}
	/**
	 * Accesses additional data and combines it with the accessible items in a one-to- 0-1 fashion.
	 * @param reader Reader used for acquiring the right-side items
	 * @param bridges Bridging joins to apply before joining the 'reader's tables. Default = empty.
	 * @param combine A function which combines a primary item and a right-side item, if present.
	 * @tparam R Type of right-side items
	 * @tparam B Type of 'combine' results
	 * @return Access to combined items
	 */
	def combineWithIfPresent[R, B](reader: DbRowReader[R], bridges: Seq[Joinable] = Empty)
	                              (combine: (A, Option[R]) => B) =
		extendTo[B](reader.tables, exclusiveColumnsFrom(reader.selectTarget), bridges, JoinType.Left) { (left, row) =>
			Some(combine(left, reader.tryParse(row)))
		}
	/**
	 * Accesses additional data and combines it with the accessible items in a one-to-many fashion.
	 * @param reader Reader used for acquiring the right-side items
	 * @param bridges Bridging joins to apply before joining the 'reader's tables. Default = empty.
	 * @param neverEmpty Whether to assume that at least one right-side item will always be linked.
	 *                   If true, causes inner join to be applied, instead of a left join, which is the default.
	 * @param combine A function which combines a primary item and the matching right-side items
	 * @tparam R Type of right-side items
	 * @tparam B Type of 'combine' results
	 * @return Access to combined items
	 */
	def combineWithMany[R, B](reader: DbReader[_] with ParseRows[R], bridges: Seq[Joinable] = Empty,
	                          neverEmpty: Boolean = false)
	                         (combine: (A, R) => B) =
		extendToMany(reader.tables, exclusiveColumnsFrom(reader.selectTarget), bridges,
			if (neverEmpty) JoinType.Inner else JoinType.Left) {
			_.map { case (left, rows) => combine(left, reader(rows)) } }
	
	/**
	 * @param target A select target
	 * @return Columns which are exclusive to other table columns in that target
	 */
	protected def exclusiveColumnsFrom(target: SelectTarget) = target match {
		case SingleColumn(column) => Single(column)
		case Columns(columns) => columns
		case _ => Empty
	}
}
