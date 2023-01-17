package utopia.vault.sql

import utopia.vault.model.enumeration.BasicCombineOperator
import utopia.vault.model.immutable.Storable

object Where
{
    /**
     * This is an alternative way of converting a condition into a where clause. Does the 
     * same as condition.toWhereClause
     */
    def apply(condition: Condition) = condition.toWhereClause
    
    /**
      * A utility method for converting a storable instance into a where clause. Same as calling
      * storable.toCondition.toWhereClause
      * @param conditionModel A storable model representing a condition
      * @return A where clause based on the model
      */
    def apply(conditionModel: Storable) = conditionModel.toCondition.toWhereClause
    
    /**
      * @param condition A condition
      * @return A where clause that only accepts rows that DON'T fulfill the condition
      */
    def not(condition: Condition) = (!condition).toWhereClause
}

object Condition
{
    // ATTRIBUTES   -------------------------
    
    /**
      * A condition that always returns true
      */
    val alwaysTrue = Condition(SqlSegment("TRUE"))
    /**
      * A condition that always returns false
      */
    val alwaysFalse = Condition(SqlSegment("FALSE"))
    
    
    // OTHER    -----------------------------
    
    /**
     * @param conditions A set of conditions
     * @param resultOnEmpty Whether any (all) rows should be returned if this list is empty (default = false)
     * @return A combination of those conditions using OR
     */
    def or(conditions: Seq[Condition], resultOnEmpty: Boolean = false) =
    {
        if (conditions.isEmpty)
        {
            if (resultOnEmpty) alwaysTrue else alwaysFalse
        }
        else
            conditions.head || conditions.tail
    }
    /**
     * @param conditions A set of conditions
     * @param resultOnEmpty Whether any (all) rows should be returned if this list is empty (default = true)
     * @return A combination of those conditions using AND
     */
    def and(conditions: Seq[Condition], resultOnEmpty: Boolean = true) =
    {
        if (conditions.isEmpty)
        {
            if (resultOnEmpty) alwaysTrue else alwaysFalse
        }
        else
            conditions.head && conditions.tail
    }
}

/**
 * Conditions can be combined with each other logically and converted to sql where clauses. 
 * A where clause is often used after a join or a basic operation.
 * @author Mikko Hilpinen
 * @since 22.5.2017
  * @param segment The sql segment that forms this condition. Doesn't include the "WHERE" -part.
 */
case class Condition(segment: SqlSegment)
{
    // COMPUTED PROPERTIES    ---------------
    
    override def toString = toWhereClause.toString()
    
    /**
     * Converts this condition into a real sql segment that can function as a where clause
     */
    def toWhereClause = segment prepend "WHERE"
    
    
    // OPERATORS    -------------------------
    
    /**
     * Combines the conditions together using a logical AND. All of the conditions are wrapped in 
     * single parentheses '()' and performed together, from left to right.
     */
    def &&(others: Seq[Condition]) = combine(others, "AND")
    
    /**
     * Combines this and another condition together using a logical AND. The conditions are wrapped in 
     * single parentheses '()' and performed together, from left to right.
     */
    def &&(other: Condition): Condition = this && Vector(other)
    
    /**
     * Combines the conditions together using a logical AND. All of the conditions are wrapped in 
     * single parentheses '()' and performed together, from left to right.
     */
    def &&(first: Condition, second: Condition, more: Condition*): Condition = this && (Vector(first, second) ++ more)
    
    /**
      * @param other Another condition
      * @return A combination (using AND) of these conditions, wrapped in parenthesis '()'
      */
    def &&(other: Option[Condition]): Condition = other.map { this && _ }.getOrElse(this)
    
    /**
     * Combines the conditions together using a logical OR. All of the conditions are wrapped in 
     * single parentheses '()' and performed together, from left to right.
     */
    def ||(others: Seq[Condition]) = combine(others, "OR")
    
    /**
     * Combines this and another condition together using a logical OR. The conditions are wrapped in 
     * single parentheses '()' and performed together, from left to right.
     */
    def ||(other: Condition): Condition = this || Vector(other)
    
    /**
     * Combines the conditions together using a logical OR. All of the conditions are wrapped in 
     * single parentheses '()' and performed together, from left to right.
     */
    def ||(first: Condition, second: Condition, more: Condition*): Condition = this || (Vector(first, second) ++ more)
    
    /**
      * @param other Another condition
      * @return A combination of these conditions (using OR) wrapped in parenthesis '()'
      */
    def ||(other: Option[Condition]): Condition = other.map { this || _ }.getOrElse(this)
    
    /**
     * Applies a logical NOT operator on this condition, reversing any logical outcome
     */
    def unary_! = Condition(segment.copy(sql = s"NOT (${ segment.sql })"))
    
    
    // OTHER METHODS    ---------------------
    
    /**
     * Combines this and another condition together using a logical XOR. The logical value is true 
     * when both of the conditions have different values
     */
    def xor(other: Condition) = combine(Vector(other), "XOR")
    
    /**
      * Combines this condition with other conditions using specified operator
      * @param others Other conditions
      * @param operator An operator used for combining these two conditions
      * @return A combination of these conditions
      */
    def combineWith(others: Seq[Condition], operator: BasicCombineOperator) = combine(others, operator.toSql)
    
    /**
      * Combines these two conditions with each other using specified operator
      * @param other Another condition
      * @param operator Operator used when combining these conditions
      * @return A combination of these two conditions
      */
    def combineWith(other: Condition, operator: BasicCombineOperator): Condition = combineWith(Vector(other), operator)
    
    private def combine(others: Seq[Condition], separator: String) = 
    {
        if (others.isEmpty)
            this
        else 
        {
            val noParentheses = SqlSegment.combine(segment +: others.map { _.segment },
                { case (first, second) => s"$first $separator $second" })
            Condition(noParentheses.copy(sql = s"(${ noParentheses.sql })"))
        }
    }
}