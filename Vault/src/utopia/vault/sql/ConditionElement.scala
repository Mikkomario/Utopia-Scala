package utopia.vault.sql

import utopia.flow.generic.ValueConversions._
import utopia.vault.model.enumeration.ComparisonOperator
import Extensions._

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
    
    
    // OPERATORS    -----------------------------
    
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
    
    
    // OTHER METHODS    ---------------------
    
    /**
     * Creates a between condition that returns true when this element value is between the two 
     * provided values (inclusive)
     */
    def isBetween(min: ConditionElement, max: ConditionElement) = Condition(toSqlSegment + "BETWEEN" + 
            min.toSqlSegment + "AND" + max.toSqlSegment)
    
    /**
     * Creates an in condition that returns true if one of the provided element values matches 
     * this element's value
     */
    def in(elements: IterableOnce[ConditionElement]) =
    {
        val rangeSegment = SqlSegment.combine(elements.iterator.map { _.toSqlSegment }.toSeq, { _ + ", " + _ })
        val inSegment = rangeSegment.copy(sql = "(" + rangeSegment.sql + ")")
        
        Condition(toSqlSegment + "IN" + inSegment)
    }
    
    /**
      * @param elements Values accepted for this element
      * @param transform An implicit transformation between provided values and condition elements
      * @tparam V Type of value
      * @return A condition that accepts any of the provided value in this condition element
      */
    def in[V](elements: IterableOnce[V])(implicit transform: V => ConditionElement): Condition =
        in(elements.iterator.map(transform))
    
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
      * Creates a simple condition based on two condition elements. This element is used as the first operand.
      * @param operator A comparison operator used
      * @param other Another condition element
      * @return A condition that compares these two elements using specified operator
      */
    def makeCondition(operator: ComparisonOperator, other: ConditionElement): Condition = makeCondition(operator.toSql, other)
    
    private def makeCondition(operator: String, other: ConditionElement) = Condition(toSqlSegment + operator + other.toSqlSegment)
}