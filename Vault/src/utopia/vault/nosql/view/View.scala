package utopia.vault.nosql.view

import utopia.flow.collection.CollectionExtensions._
import utopia.vault.database.{Connection, References}
import utopia.vault.model.immutable.{Column, Storable, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.{Condition, Delete, Exists, Join, SqlTarget, Where}

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
	  * Deletes all items accessible from this access points
	  * @param table The table in which deletion occurs. Should be part of this view's target
	  *              (i.e. primary table or one of the joined tables).
	  *              Default = this view's primary table.
	  * @param connection Database connection (implicit)
	  */
	def delete(table: Table = table)(implicit connection: Connection): Unit = _delete(table)
	/**
	  * Deletes items which are accessible from this access point and fulfill the specified condition
	  * @param condition  Deletion condition (applied in addition to the global condition)
	  * @param joins Joins that should be applied to this query (optional)
	  * @param table The table in which deletion occurs. Should be part of this view's target
	  *              (i.e. primary table or one of the joined tables).
	  *              Default = this view's primary table.
	  * @param connection DB Connection (implicit)
	  */
	def deleteWhere(condition: Condition, joins: Vector[Joinable] = Vector(), table: Table = table)
	               (implicit connection: Connection): Unit =
		_delete(table, Some(condition), joins)
	/**
	  * Deletes items which are not linked to the specified table in some way
	  * @param table Targeted link table
	  * @param linkCondition Condition on which links should be considered valid (optional)
	  * @param deletedTable Table to target with the deletion (default = primary table of this view)
	  * @param connection Implicit DB Connection
	  */
	def deleteNotLinkedTo(table: Table, linkCondition: Option[Condition] = None, deletedTable: Table = table)
	                     (implicit connection: Connection) =
		forNotLinkedTo(table, linkCondition) { (c, join) =>
			deleteWhere(c, Vector(join), deletedTable)
		} { delete(deletedTable) }
	
	/**
	  * Performs a database operation for cases that can't be linked to a specific table
	  * @param table Targeted table
	  * @param linkCondition Condition that must be true in order for the link to be considered (optional)
	  * @param onRefFound Function called in cases where a reference between these tables is found.
	  *                   Accepts the **additional** condition to apply to requests, and the join to apply.
	  * @param onNoRefFound Function called in cases where no reference exists between these tables
	  * @tparam A Function result type
	  * @return Function result
	  */
	protected def forNotLinkedTo[A](table: Table, linkCondition: Option[Condition] = None)
	                               (onRefFound: (Condition, Join) => A)
	                               (onNoRefFound: => A) =
	{
		// Finds the reference between these tables, if possible
		target.tables.findMap { t => References.between(t, table).headOption } match {
			// Case: Reference found => Converts the reference to a join
			case Some(ref) =>
				val directionalRef = if (ref.from.table == this.table) ref else ref.reverse
				val baseJoin = directionalRef.toLeftJoin
				val join = linkCondition match {
					case Some(c) => baseJoin.where(c)
					case None => baseJoin
				}
				// Performs the specified function
				onRefFound(directionalRef.to.column.isNull, join)
			// Case: No reference found => Performs the alternative function
			case None => onNoRefFound
		}
	}
	
	private def _delete(table: Table, condition: Option[Condition] = None, joins: Vector[Joinable] = Vector())
	                   (implicit connection: Connection): Unit =
	{
		// Applies additional joins
		val baseTarget = joins.foldLeft(target) { _ join _ }
		val appliedTarget = {
			// Case: Targeting a table that's part of this view's target => Performs delete
			if (baseTarget.tables.contains(table))
				baseTarget
			// Case: Targeting a table outside of this view's target => Attempts join
			else
				baseTarget join table
		}
		connection(Delete(appliedTarget, table) + mergeCondition(condition).map { Where(_) })
	}
}
