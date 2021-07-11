package utopia.vault.nosql.view

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.{Condition, Delete, Exists, Where}

/**
  * A common trait for database views that utilize a factory class
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  * @tparam A Type of items accessible through the factory
  */
trait FactoryView[+A] extends View
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The factory used by this view
	  */
	def factory: FromResultFactory[A]
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Sql target viewed through this view
	  */
	def target = factory.target
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there exists an item/row visible through this view
	  */
	def nonEmpty(implicit connection: Connection) = globalCondition match {
		case Some(condition) => Exists(target, condition)
		case None => Exists.any(target)
	}
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there doesn't exist a single row visible through this view
	  */
	def isEmpty(implicit connection: Connection) = !nonEmpty
	
	
	// IMPLEMENTED  -------------------------
	
	override def table = factory.table
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param where      A search condition
	  * @param connection Implicit database connection
	  * @return Whether this view provides access to at least item (row) where the specified condition is met
	  */
	def exists(where: Condition)(implicit connection: Connection) = factory.exists(where)
	
	/**
	  * Deletes all items accessible from this access points (only primary table is targeted)
	  * @param connection Database connection (implicit)
	  */
	def delete()(implicit connection: Connection): Unit =
		connection(Delete(target, table) + globalCondition.map { Where(_) })
	
	/**
	  * Deletes items which are accessible from this access point and fulfill the specified condition
	  * (only primary table is targeted)
	  * @param condition  Deletion condition (applied in addition to the global condition)
	  * @param connection DB Connection (implicit)
	  */
	def deleteWhere(condition: Condition)(implicit connection: Connection): Unit =
		connection(Delete(target, table) + Where(mergeCondition(condition)))
}
