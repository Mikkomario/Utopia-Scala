package utopia.vault.nosql.view

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.{Condition, OrderBy}

/**
  * A common trait for views that utilize a FromRowFactory
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  * @tparam A Type of items/models returned by the factory
  */
trait RowFactoryView[+A] extends FactoryView[A]
{
	// ABSTRACT	-------------------------
	
	override def factory: FromRowFactory[A]
	
	
	// OTHER	-------------------------
	
	/**
	  * Performs an operation for each of the items accessible from this accessor
	  * @param f          Function performed for each targeted item
	  * @param connection DB Connection (implicit)
	  * @tparam U Arbitrary result type
	  */
	def foreach[U](f: A => U)(implicit connection: Connection) = factory.foreachWhere(accessCondition)(f)
	/**
	  * Performs an operation over a subset of items accessible from this accessor
	  * @param additionalCondition Additional targeting condition
	  * @param f                   Function performed for each targeted item
	  * @param connection          DB Connection (implicit)
	  * @tparam U Arbitrary result type
	  */
	def foreachWhere[U](additionalCondition: Condition)(f: A => U)(implicit connection: Connection) =
		factory.foreachWhere(mergeCondition(additionalCondition))(f)
	
	/**
	  * @param n Number of items to return (maximum)
	  * @param order Ordering to use
	  * @param c Implicit DB connection
	  * @return The first 'n' accessible items when using the specified ordering
	  */
	def take(n: Int, order: OrderBy)(implicit c: Connection) = factory.take(n, order, accessCondition)
}
