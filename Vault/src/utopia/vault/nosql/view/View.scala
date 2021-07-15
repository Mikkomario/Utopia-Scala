package utopia.vault.nosql.view

import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Storable, Table}
import utopia.vault.sql.{Condition, Delete, Exists, SqlTarget, Where}

/**
  * A template trait for all access points. Doesn't specify anything about the read content but specifies the
  * settings necessary for facilitating read processes
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait View
{
	// ABSTRACT	----------------------
	
	/**
	  * @return The primary table viewed through this access point
	  */
	def table: Table
	
	/**
	  * @return The target segment (table + possible joins) viewed through this view
	  */
	def target: SqlTarget
	
	/**
	  * @return Condition applied to all searches that use this access point. None if no condition should be applied.
	  */
	def globalCondition: Option[Condition]
	
	
	// COMPUTED ----------------------
	
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
	
	
	// OTHER	----------------------
	
	/**
	  * Merges an additional condition with the existing global condition
	  * @param additional An additional condition
	  * @return A combination of the additional and global conditions
	  */
	def mergeCondition(additional: Condition) = globalCondition.map { _ && additional }.getOrElse(additional)
	/**
	  * Merges an additional condition with the existing global condition
	  * @param additional An additional condition (optional)
	  * @return A combination of the additional and global conditions. None if there was neither.
	  */
	def mergeCondition(additional: Option[Condition]): Option[Condition] = additional match {
		case Some(cond) => Some(mergeCondition(cond))
		case None => globalCondition
	}
	/**
	  * Merges an additional condition with the existing global condition
	  * @param conditionModel A model representing the additional condition to apply
	  * @return A combination of these conditions
	  */
	def mergeCondition(conditionModel: Storable): Condition = mergeCondition(conditionModel.toCondition)
	
	/**
	  * @param where      A search condition
	  * @param connection Implicit database connection
	  * @return Whether this view provides access to at least item (row) where the specified condition is met
	  */
	def exists(where: Condition)(implicit connection: Connection) = Exists(target, mergeCondition(where))
	
	/**
	  * @param column A column
	  * @param connection Implicit DB Connection
	  * @return Whether there exists a row visible through this view where that column is not null
	  */
	def contains(column: Column)(implicit connection: Connection) = exists(column.isNotNull)
	/**
	  * @param column A column
	  * @param connection Implicit DB Connection
	  * @return Whether there exists a row visible through this view where that column is null
	  */
	def containsNull(column: Column)(implicit connection: Connection) = exists(column.isNull)
	
	/**
	  * @param attributeName A name of a table property
	  * @param connection Implicit DB Connection
	  * @return Whether there exists a row visible through this view where that property is not null
	  */
	def containsAttribute(attributeName: String)(implicit connection: Connection) =
		contains(table(attributeName))
	/**
	  * @param attributeName A name of a table property
	  * @param connection Implicit DB Connection
	  * @return Whether there exists a row visible through this view where that property is null
	  */
	def containsNullAttribute(attributeName: String)(implicit connection: Connection) =
		containsNull(table(attributeName))
	
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
