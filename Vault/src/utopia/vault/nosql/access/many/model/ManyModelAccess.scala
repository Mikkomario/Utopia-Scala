package utopia.vault.nosql.access.many.model

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.many.ManyAccess
import utopia.vault.nosql.access.many.model.ManyModelAccess.FactoryWrapper
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.{Condition, OrderBy, Select, Where}

object ManyModelAccess
{
	// OTHER	--------------------------
	
	/**
	 * Wraps a model factory
	 * @param factory A model factory
	 * @param order Order by used in the queries by default (optional)
	 * @tparam A Type of model read from DB
	 * @return An access point
	 */
	def apply[A](factory: FromResultFactory[A], order: Option[OrderBy] = None): ManyModelAccess[A] =
		new FactoryWrapper(factory, None, order)
	
	/**
	 * Wraps a model factory
	 * @param factory A model factory
	 * @param condition A search condition used
	 * @param order Order by used in the queries by default (optional)
	 * @tparam A Type of model read from DB
	 * @return An access point
	 */
	def conditional[A](factory: FromResultFactory[A], condition: Condition,
	                   order: Option[OrderBy] = None): ManyModelAccess[A] =
		new FactoryWrapper(factory, Some(condition), order)
	
	
	// NESTED	--------------------------
	
	private class FactoryWrapper[A](override val factory: FromResultFactory[A], condition: Option[Condition],
	                                override val defaultOrdering: Option[OrderBy])
		extends ManyModelAccess[A]
	{
		override def globalCondition = condition
	}
}

/**
 * Used for accessing multiple models at a time from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyModelAccess[+A] extends ManyAccess[A]
	with DistinctModelAccess[A, Vector[A], Vector[Value]] with FilterableView[ManyModelAccess[A]]
{
	// COMPUTED --------------------------------
	
	/**
	 * @param connection DB Connection (implicit)
	 * @return An iterator to all models accessible through this access point. The iterator is valid
	 *         only while the connection is kept open.
	 */
	def iterator(implicit connection: Connection) = factory.iterator(globalCondition)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy])(implicit connection: Connection) = condition match
	{
		case Some(condition) => factory.getMany(condition, order)
		case None => factory.getAll(order)
	}
	
	override def filter(additionalCondition: Condition): ManyModelAccess[A] = new FactoryWrapper[A](factory,
		Some(mergeCondition(additionalCondition)), defaultOrdering)
	
	/**
	 * Reads the values of an individual column
	 * @param column Column to read
	 * @param additionalCondition Additional search condition to apply (optional)
	 * @param order Ordering to use (optional)
	 * @param connection DB Connection (implicit)
	 * @return Values of that column (may be empty and may contain empty values)
	 */
	override protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                                  order: Option[OrderBy] = None)(implicit connection: Connection) =
	{
		// Forms the query first
		val baseQuery = Select(factory.target, column)
		val conditionedQuery = mergeCondition(additionalCondition) match
		{
			case Some(condition) => baseQuery + Where(condition)
			case None => baseQuery
		}
		val query = order match
		{
			case Some(order) => conditionedQuery + order
			case None => conditionedQuery
		}
		// Applies the query and parses results
		connection(query).rowValues
	}
	
	
	// OTHER    -------------------------------
	
	/**
	 * @param order Order to use in the results
	 * @param connection DB Connection (implicit)
	 * @return An iterator to all models accessible from this access point. The iterator is usable
	 *         only while the connection is kept open.
	 */
	def orderedIterator(order: OrderBy)(implicit connection: Connection) =
		factory.iterator(globalCondition, Some(order))
}