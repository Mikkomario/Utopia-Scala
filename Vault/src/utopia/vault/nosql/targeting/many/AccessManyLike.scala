package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Column
import utopia.vault.model.mutable.ResultStream
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.OrderDirection.{Ascending, Descending}
import utopia.vault.sql._

/**
  * An interface used for accessing multiple items with each search query
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait AccessManyLike[+A, +Repr] extends TargetingManyLike[A, Repr, TargetingOne[Option[A]]]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return A selection target applied to queries by default
	  */
	def selectTarget: SelectTarget
	/**
	  * @return Ordering applied to queries by default
	  */
	def ordering: Option[OrderBy]
	
	/**
	  * Finalizes an SQL statement, before it is executed.
	  * Used for allowing subclasses to modify executed statements by adding more features (like limit and offset),
	  * for example.
	  * @param statement A statement to finalize
	  * @return A finalized version of the statement
	  */
	protected def finalizeStatement(statement: SqlSegment): SqlSegment
	
	/**
	  * Parses an acquired result stream into the desired data type.
	  * May assume that the result contains the specified [[selectTarget]].
	  * @param result Acquired result stream
	  * @return Items parsed from the result
	  */
	protected def parse(result: ResultStream): Seq[A]
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return Condition applied to the executed queries by default.
	  *         Basically [[accessCondition]], but excluding entries that need not be applied.
	  */
	protected def appliedCondition = accessCondition.filterNot { _.isAlwaysTrue }
	
	/**
	  * @return A SELECT statement executed by default when pulling data
	  */
	protected def toSelect = selectTarget.toSelect(target)
	
	/**
	  * @return Primary key columns included in the default [[selectTarget]].
	  */
	protected def keys = {
		val selection = selectTarget
		target.tables.view.flatMap { _.primaryColumn }.filter(selection.contains).toOptimizedSeq
	}
	
	
	// IMPLEMENTED  ------------------
	
	override def pull(implicit connection: Connection): Seq[A] = pullManyWith[A](toSelect)(parse)
	
	override def apply(column: Column, distinct: Boolean)(implicit connection: Connection) =
		pullManyWith(Select.distinctIf(target, column, distinct)) { _.rowValuesIterator.toOptimizedSeq }
	override def apply(columns: Seq[Column])(implicit connection: Connection) =
		pullManyWith(Select(target, columns)) { _.rowsIterator.map { row => columns.map(row.apply) }.toOptimizedSeq }
	
	override def apply(column: Column, extreme: Extreme)(implicit connection: Connection): Value = {
		val condition = appliedCondition
		// Case: No rows may be targeted => Skips the DB interaction
		if (condition.exists { _.isAlwaysFalse })
			Value.empty
		else {
			// Determines the ordering direction
			val direction = extreme match {
				case Max => Descending
				case Min => Ascending
			}
			// Pulls the most extreme accessible column value from the DB
			connection.stream(Select(target, column) + condition.map(Where.apply) +
				OrderBy(column, direction) + Limit(1)) { _.nextValue }
		}
	}
	
	override def streamColumn[B](column: Column, distinct: Boolean)(f: Iterator[Value] => B)
	                            (implicit connection: Connection) =
		pullWith(Select.distinctIf(target, column, distinct),
			f(Iterator.empty)) { result => f(result.rowValuesIterator) }
	override def streamColumns[B](columns: Seq[Column])(f: Iterator[Seq[Value]] => B)
	                             (implicit connection: Connection) =
		pullWith(Select(target, columns), f(Iterator.empty)) { result =>
			f(result.rowsIterator.map { row => columns.map(row.apply) })
		}
	
	override def update(column: Column, value: Value)(implicit connection: Connection): Boolean = {
		val condition = appliedCondition
		if (condition.exists { _.isAlwaysFalse })
			false
		else
			connection.stream(Update(target, column, value) + condition.map(Where.apply)) { _.updatedRows }
	}
	// WET WET
	override def update(assignments: IterableOnce[(Column, Value)])(implicit connection: Connection): Boolean = {
		val condition = appliedCondition
		if (condition.exists { _.isAlwaysFalse })
			false
		else
			assignments.toOptimizedSeq.notEmpty match {
				case Some(assignments) =>
					connection
						.stream(Update.columns(target, assignments) + condition.map(Where.apply)) { _.updatedRows }
				case None => false
			}
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * A generic 'pullWith' implementation
	  * @param statement Statement to execute - condition, ordering & finalization will be applied, also
	  * @param emptyResult A result to yield in situations where no execution is needed
	  *                    (i.e. when limited to a condition that's always false)
	  * @param condition Where condition to apply. Default = current [[appliedCondition]].
	  * @param ordering Ordering to apply. Default = This access point's [[ordering]].
	  * @param parse A function which parses the acquired result into the desired data type
	  * @param connection Implicit DB connection
	  * @tparam B Type of parsed results
	  * @return Parsed results
	  */
	protected def pullWith[B](statement: SqlSegment, emptyResult: => B, condition: Option[Condition] = appliedCondition,
	                          ordering: Option[OrderBy] = this.ordering)
	                         (parse: ResultStream => B)
	                         (implicit connection: Connection) =
	{
		if (condition.exists { _.isAlwaysFalse })
			emptyResult
		else
			connection.stream(completeStatement(statement, condition, ordering))(parse)
	}
	/**
	  * A generic 'pullManyWith' implementation
	  * @param statement Statement to execute - condition, ordering & finalization will be applied, also
	  * @param condition Where condition to apply. Default = current [[appliedCondition]].
	  * @param ordering Ordering to apply. Default = This access point's [[ordering]].
	  * @param parse A function which parses the acquired result into a sequence of items
	  * @param connection Implicit DB connection
	  * @tparam B Type of parsed results
	  * @return Parsed results
	  */
	protected def pullManyWith[B](statement: SqlSegment, condition: Option[Condition] = appliedCondition,
	                              ordering: Option[OrderBy] = this.ordering)
	                             (parse: ResultStream => Seq[B])
	                             (implicit connection: Connection) =
		pullWith[Seq[B]](statement, Empty, condition, ordering)(parse)
	
	/**
	  * Completes an SQL statement by including the specified condition and ordering,
	  * plus subclass-specific finalization logic (e.g. adding a limit)
	  * @param statement The statement to complete & finalize
	  * @param condition Condition to apply (default = currently defined [[appliedCondition]])
	  * @param ordering Ordering to apply (default = this access' [[ordering]])
	  * @return A completed SQL statement
	  */
	protected def completeStatement(statement: SqlSegment, condition: Option[Condition] = appliedCondition,
	                                ordering: Option[OrderBy] = this.ordering) =
		finalizeStatement(statement + condition.map(Where.apply) + ordering)
}
