package utopia.vault.nosql.access.single.model

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.single.SingleAccess
import utopia.vault.nosql.access.single.model.SingleModelAccess.FactoryWrapper
import utopia.vault.nosql.access.template.model.ModelAccess
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.{Condition, Limit, OrderBy, Select, Where}

object SingleModelAccess
{
	// OTHER	-----------------------
	
	/**
	 * Creates a new single model access
	 * @param factory Wrapped model factory
	 * @tparam A Type of model returned
	 * @return An access point that uses the specified factory
	 */
	def apply[A](factory: FromResultFactory[A]): SingleModelAccess[A] = new FactoryWrapper(factory, None)
	
	/**
	 * Creates a new single model access
	 * @param factory Wrapped model factory
	 * @param condition A search condition
	 * @tparam A Type of model returned
	 * @return An access point that uses the specified factory and search condition
	 */
	def conditional[A](factory: FromResultFactory[A], condition: Condition): SingleModelAccess[A] =
		new FactoryWrapper(factory, Some(condition))
	
	
	// NESTED	-----------------------
	
	private class FactoryWrapper[+A](val factory: FromResultFactory[A], condition: Option[Condition]) extends SingleModelAccess[A]
	{
		override def globalCondition = condition
	}
}

/**
 * Used for accessing individual models from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait SingleModelAccess[+A] extends SingleAccess[A, SingleModelAccess[A]] with ModelAccess[A, Option[A], Value]
{
	// IMPLEMENTED  -------------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy])(implicit connection: Connection) =
	{
		condition match
		{
			case Some(condition) => factory.get(condition, order)
			case None =>
				factory match
				{
					case rowFactory: FromRowFactory[A] => rowFactory.getAny()
					case _ => factory.getAll().headOption // This is not recommended
				}
		}
	}
	
	override def filter(additionalCondition: Condition): SingleModelAccess[A] =
		new FactoryWrapper[A](factory, Some(mergeCondition(additionalCondition)))
	
	/**
	 * Reads the value of an individual column
	 * @param column Column to read
	 * @param additionalCondition Additional search condition to apply (optional)
	 * @param order Ordering to use (optional)
	 * @param connection DB Connection (implicit)
	 * @return Value of that column (may be empty)
	 */
	protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                         order: Option[OrderBy] = None)(implicit connection: Connection) =
	{
		// Forms the query first
		val baseQuery = Select(factory.target, column)
		val conditionedQuery = mergeCondition(additionalCondition) match
		{
			case Some(condition) => baseQuery + Where(condition)
			case None => baseQuery
		}
		val query = (order match
		{
			case Some(order) => conditionedQuery + order
			case None => conditionedQuery
		}) + Limit(1)
		// Applies the query and parses results
		connection(query).firstValue
	}
}