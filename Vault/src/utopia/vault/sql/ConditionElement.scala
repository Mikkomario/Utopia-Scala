package utopia.vault.sql

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.model.enumeration.ComparisonOperator
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value

import scala.language.implicitConversions

object ConditionElement
{
    // IMPLICIT ------------------------
    
    implicit def valueToConditionElement(v: Value): ConditionElement = new ValueAsElement(v)
    implicit def indirectValueToConditionElement[V](v: V)(implicit f: V => Value): ConditionElement =
        valueToConditionElement(f(v))
    
    
    // OTHER    -----------------------
    
    /**
      * @param sqlSegment An sql segment that represents a condition element
      * @return A condition element that wraps that sql segment
      */
    def apply(sqlSegment: SqlSegment): ConditionElement = new SegmentAsElement(sqlSegment)
    
    
    // NESTED   ------------------------
    
    private class ValueAsElement(val value: Value) extends ConditionElement {
        override def toSqlSegment = SqlSegment("?", Vector(value))
    }
    
    private class SegmentAsElement(segment: SqlSegment) extends ConditionElement {
        override def toSqlSegment: SqlSegment = segment
    }
}

/**
 * ConditionElements are elements used in logical conditions. Usually two or more elements are 
 * compared in some way
 * @author Mikko Hilpinen
 * @since 25.5.2017
 */
trait ConditionElement
{
    // ABSTRACT METHODS    ----------------------
    
    /**
     * Converts this condition element into an sql segment
     */
    def toSqlSegment: SqlSegment
    
    
    // COMPUTED -----------------------------
    
    /**
      * @return a condition element that represents the time of this element value.
      *         Intended to be used for Timestamp and DateTime condition elements.
      */
    def time: ConditionElement = ConditionElement(toSqlSegment.mapSql { sql => s"TIME($sql)" })
    
    
    // OTHER    -----------------------------
    
    /**
     * Creates an equality condition between two elements
     */
    def <=>(other: ConditionElement) = makeCondition("<=>", other)
    /**
     * Creates a not equals condition between two elements
     */
    def <>(other: ConditionElement) = makeCondition("<>", other)
    /**
     * Creates a larger than condition
     */
    def >(other: ConditionElement) = makeCondition(">", other)
    /**
     * Creates a larger than or equals condition
     */
    def >=(other: ConditionElement) = makeCondition(">=", other)
    /**
     * Creates a smaller than condition
     */
    def <(other: ConditionElement) = makeCondition("<", other)
    /**
     * Creates a smaller than or equals condition
     */
    def <=(other: ConditionElement) = makeCondition("<=", other)
    
    /**
     * Creates a between condition that returns true when this element value is between the two 
     * provided values (inclusive)
     */
    def isBetween(min: ConditionElement, max: ConditionElement) =
        Condition(toSqlSegment + "BETWEEN" + min.toSqlSegment + "AND" + max.toSqlSegment)
    /**
      * @param minMax Minimum and maximum allowed values (inclusive), as a pair
      * @return A condition that returns true if the tested value is
      *         1) Equal or larger than the first value, and
      *         2) Smaller or equal than the second value
      */
    def isBetween(minMax: Pair[ConditionElement]): Condition = isBetween(minMax.first, minMax.second)
    
    /**
     * Creates an in condition that returns true if one of the provided element values matches 
     * this element's value
     */
    def in(elements: Iterable[ConditionElement]) = {
        // Uses simpler conditions if they are more suitable
        if (elements.isEmpty)
            Condition.alwaysFalse
        else if (elements.size == 1)
            this <=> elements.head
        else {
            val rangeSegment = SqlSegment.combine(elements.map { _.toSqlSegment }.toSeq, { (a, b) => s"$a, $b" })
            val inSegment = rangeSegment.copy(sql = s"(${ rangeSegment.sql })")
    
            Condition(toSqlSegment + "IN" + inSegment)
        }
    }
    /**
      * @param elements Values accepted for this element
      * @param transform An implicit transformation between provided values and condition elements
      * @tparam V Type of value
      * @return A condition that accepts any of the provided value in this condition element
      */
    def in[V](elements: Iterable[V])(implicit transform: V => ConditionElement): Condition = in(elements.map(transform))
    /**
      * Creates an in condition that returns true if NONE of the provided element values matches
      * this element's value
      */
    def notIn(elements: Iterable[ConditionElement]) = {
        // Uses simpler conditions if they are more suitable
        if (elements.isEmpty)
            Condition.alwaysTrue
        else if (elements.size == 1)
            this <> elements.head
        else {
            val rangeSegment = SqlSegment.combine(elements.map { _.toSqlSegment }.toSeq, { (a, b) => s"$a, $b" })
            val inSegment = rangeSegment.copy(sql = s"(${ rangeSegment.sql })")
            
            Condition(toSqlSegment + "NOT IN" + inSegment)
        }
    }
    /**
      * @param elements Values not accepted for this element
      * @param transform An implicit transformation between provided values and condition elements
      * @tparam V Type of value
      * @return A condition that accepts NONE of the provided value in this condition element
      */
    def notIn[V](elements: Iterable[V])(implicit transform: V => ConditionElement): Condition =
        notIn(elements.map(transform))
    
    /**
      * @param matchString A string match where % is a placeholder for any string.
      * @return A condition where this element must match the specified expression
      */
    def like(matchString: ConditionElement) = Condition(toSqlSegment + "LIKE" + matchString.toSqlSegment)
    /**
      * @param matchString A string
      * @return A condition where this element must start with the specified string
      */
    def startsWith(matchString: String) = like(matchString + "%")
    /**
      * @param matchString A string
      * @return A condition where this element must end with the specified string
      */
    def endsWith(matchString: String) = like("%" + matchString)
    /**
      * @param matchString A string
      * @return A condition where this element must contain the specified string
      */
    def contains(matchString: String) = like(s"%$matchString%")
    /**
      * @param string A string
      * @return A condition that returns true if this element appears within the specified string
      */
    def appearsWithin(string: String) = {
        val myStatement = toSqlSegment
        myStatement.copy(sql = s"? LIKE CONCAT('%', ${myStatement.sql}, '%')", values = string +: myStatement.values)
    }
    
    /**
      * Creates a simple condition based on two condition elements. This element is used as the first operand.
      * @param operator A comparison operator used
      * @param other Another condition element
      * @return A condition that compares these two elements using specified operator
      */
    def makeCondition(operator: ComparisonOperator, other: ConditionElement): Condition = makeCondition(operator.toSql, other)
    
    private def makeCondition(operator: String, other: ConditionElement) = Condition(toSqlSegment + operator + other.toSqlSegment)
}