package utopia.vault.nosql.access.template.model

import utopia.flow.collection.immutable.Empty
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.template.Access
import utopia.vault.nosql.view.FactoryView
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, JoinType, OrderBy}

/**
  * Common trait for access points that return parsed model data
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  * @tparam M Type of model returned
  * @tparam A The format in which model data is returned (E.g. a list of models)
  * @tparam V Format in which column values are returned (E.g. A single value or a vector of values)
  */
trait ModelAccess[+M, +A, +V] extends Access[A] with FactoryView[M]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Ordering to use when no other ordering has been specified.
	  */
	protected def defaultOrdering: Option[OrderBy]
	
	/**
	  * Reads the value / values of an individual column
	  * @param column              Column to read
	  * @param additionalCondition Additional search condition to apply (optional)
	  * @param order               Ordering to use (optional).
	  *                            If omitted, uses this access point's default ordering, if one has been defined.
	  * @param joins Joins to apply (to use in search) (default = empty)
	  * @param joinType Join type to use in joins (default = Inner)
	  * @param connection          DB Connection (implicit)
	  * @return Value / values of that column (empty value(s) included)
	  */
	protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                         order: Option[OrderBy] = None, joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)
	                        (implicit connection: Connection): V
	
	
	// OTHER	-------------------------
	
	/**
	  * Reads the value of an individual column
	  * @param column     Column to read
	  * @param condition  Search condition to apply (will be added to the global condition)
	  * @param order      Ordering to use (optional).
	  *                   If omitted, uses this access point's default ordering, if one has been defined.
	  * @param joins Joins to apply (to use in search) (default = empty)
	  * @param joinType Join type to use in joins (default = Inner)
	  * @param connection DB Connection (implicit)
	  * @return Value of that column (may be empty)
	  */
	def findColumn(column: Column, condition: Condition, order: Option[OrderBy] = None,
	               joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)
	              (implicit connection: Connection) =
		readColumn(column, Some(condition), order, joins, joinType)
	/**
	  * Reads the value of an individual attribute / column
	  * @param attributeName Name of the attribute to read
	  * @param condition     Search condition to apply (will be added to the global condition)
	  * @param order         Ordering to use (optional).
	  *                      If omitted, uses this access point's default ordering, if one has been defined.
	  * @param joins Joins to apply (to use in search) (default = empty)
	  * @param joinType Join type to use in joins (default = Inner)
	  * @param connection    DB Connection (implicit)
	  * @return Value of that attribute (may be empty)
	  */
	// TODO: Rename to findProperty
	def findAttribute(attributeName: String, condition: Condition, order: Option[OrderBy],
	                  joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)
	                 (implicit connection: Connection) =
		findColumn(table(attributeName), condition, order, joins, joinType)
}
