package utopia.vault.nosql.access.many.model

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.view.RowFactoryView
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Count, JoinType, OrderBy, Where}

/**
  * Used for accessing multiple models at once, each model occupying exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait ManyRowModelAccess[+A] extends ManyModelAccess[A] with RowFactoryView[A]
{
	// COMPUTED -----------------------------
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Number of items accessible from this accessor
	  */
	def size(implicit connection: Connection) =
		connection(Count(target) + accessCondition.map { Where(_) }).firstValue.getInt
	
	
	// OTHER    -----------------------------
	
	/**
	 * Reads the first n accessible items
	 * @param order Ordering to use
	 * @param maxSize Maximum number of items returned
	 * @param connection Implicit DB Connection
	 * @return The first n accessible items
	 */
	def take(order: OrderBy, maxSize: Int)(implicit connection: Connection) =
		factory.take(maxSize, order, accessCondition)
	
	/**
	  * @param column Column to include in the results
	  * @param joins Joins to apply. Default = Empty = Automatically join the targeted column.
	  * @param joinType Type of joins to apply. Default = Inner.
	  * @param parse A function for parsing the column value into the desired data type
	  * @param connection Implicit DB connection
	  * @tparam B Type of column value parse results
	  * @return Accessible items, coupled together with the parsed column value
	  */
	def pullWithColumn[B](column: Column, joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)(parse: Value => B)
	                     (implicit connection: Connection) =
	{
		// Skips this query if it cannot yield a single row
		val condition = accessCondition
		if (condition.exists { _.isAlwaysFalse })
			Empty
		else {
			// Joins the column, unless a custom join has been specified already
			val appliedJoins = if (joins.isEmpty) Single(column) else joins
			factory.getWithColumn(column, appliedJoins, joinType, condition.filterNot { _.isAlwaysTrue })(parse)
		}
	}
	/**
	  * @param column Column to group the results by
	  * @param joins Joins to apply. Default = Empty = Automatically join the targeted column.
	  * @param joinType Type of joins to apply. Default = Inner.
	  * @param parse A function for parsing the column value into the desired data type
	  * @param connection Implicit DB connection
	  * @tparam B Type of column value parse results
	  * @return Accessible items, grouped together based on the linked column value
	  */
	def groupByColumn[B](column: Column, joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)(parse: Value => B)
	                    (implicit connection: Connection) =
		pullWithColumn(column, joins, joinType)(parse).groupMap { _._2 } { _._1 }
}
