package utopia.vault.sql

import scala.language.implicitConversions
import utopia.vault.model.immutable.Column
import utopia.vault.sql.OrderDirection.{Ascending, Descending}

/**
 * This object is used for generating sql segments that determine how the results will be ordered
 * @author Mikko Hilpinen
 * @since 27.5.2017
 */
object OrderBy
{
    // IMPLICITS    -----------------------
    
    /**
     * Implicitly converts an order by to an sql segment
     * @param order An order by object
     * @return An sql segment based on provided ordering
     */
    implicit def orderToSql(order: OrderBy): SqlSegment = order.toSqlSegment
    
    
    // OTHER    ---------------------------
    
    /**
     * Creates a new sql segment that orders by a single column either ascending or descending
     * @param column the column by which the results are ordered
     * @param direction Whether the results should be ascending or descending
     */
    def apply(column: Column, direction: OrderDirection): OrderBy = apply(Vector((column, direction)))
    
    def apply(firstPair: (Column, OrderDirection), secondPair: (Column, OrderDirection),
              morePairs: (Column, OrderDirection)*): OrderBy = apply(Vector(firstPair, secondPair) ++ morePairs)
    
    /**
     * Creates a new sql segment that orders by multiple columns using a either ascending or 
     * descending order for each
     */
    def apply(direction: OrderDirection, first: Column, second: Column, more: Column*): OrderBy =
            apply((Vector(first, second) ++ more).map { (_, direction) })
    
    /**
      * Orders by specified column(s), ascending (= smallest to largest)
      * @param first The first order column
      * @param more More order columns
      * @return An order by segment
      */
    def ascending(first: Column, more: Column*) = apply((first +: more).toVector.map { _ -> Ascending })
    
    /**
      * Orders by specified column(s), descending (= largest to smallest)
      * @param first The first order column
      * @param more More order columns
      * @return An order by segment
      */
    def descending(first: Column, more: Column*) = apply((first +: more).toVector.map { _ -> Descending })
}

/**
 * Represents ordering in sql query
 * @param keys The keys ordering happens by. Most important order keys come first and less important later.
 */
case class OrderBy(keys: Vector[(Column, OrderDirection)])
{
    // COMPUTED -----------------------
    
    /**
     * @return This ordering as an sql segment
     */
    def toSqlSegment = {
        if (keys.isEmpty)
            SqlSegment.empty
        else {
            val sqlParts = keys.map{ case (column, direction) => s"${column.columnNameWithTable} ${direction.toSql}" }
            SqlSegment(s"ORDER BY ${ sqlParts.mkString(", ") }")
        }
    }
    
    /**
      * @return A copy of this order that ascends (i.e. goes from the smallest to the greatest)
      */
    def ascending = withDirection(Ascending)
    /**
      * @return A copy of this order that descends (i.e. goes from the greatest to the smallest)
      */
    def descending = withDirection(Descending)
    
    
    // OTHER    -----------------------
    
    /**
     * Adds a new ordering to this order by. The new order will be less important than the existing orderings.
     * @param newOrderPair Column direction pair
     * @return A new order by with specified order key included
     */
    def +(newOrderPair: (Column, OrderDirection)) = if (keys.exists { _._1 == newOrderPair._1 }) this else
        copy(keys = keys :+ newOrderPair)
    /**
     * Adds a new ordering to this order by. The new order will be less important than the existing orderings
     * @param column Column the ordering is based on
     * @param direction Direction of ordering for that column
     * @return A new order by with specified order key included
     */
    def +(column: Column, direction: OrderDirection): OrderBy = this + (column -> direction)
    
    /**
     * @param column A column to remove from this ordering
     * @return A copy of this ordering without specified key used
     */
    def -(column: Column) = copy(keys = keys.filterNot { _._1 == column })
    /**
     * Combines two orderings. This ordering is considered determining
     * @param other Another ordering
     * @return A conbination of these two orderings
     */
    def ++(other: OrderBy) = copy(keys = keys ++ other.keys.filterNot { o => keys.exists { _._1 == o._1 } })
    
    /**
      * @param direction Direction to apply to ALL ordering columns
      * @return A copy of this ordering that uses the specified direction for all targeted columns
      */
    def withDirection(direction: OrderDirection) = OrderBy(keys.map { _._1 -> direction })
}