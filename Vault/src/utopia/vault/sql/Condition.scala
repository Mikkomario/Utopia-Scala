package utopia.vault.sql

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.util.UncertainBoolean
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
	val alwaysTrue = Condition(SqlSegment("TRUE"), knownResult = true)
	/**
	  * A condition that always returns false
	  */
	val alwaysFalse = Condition(SqlSegment("FALSE"), knownResult = false)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param result Predetermined end result of this condition
	  * @return A condition that always yields 'result'
	  */
	def always(result: Boolean) = if (result) alwaysTrue else alwaysFalse
	
	/**
	  * @param conditions A set of conditions
	  * @param resultOnEmpty Whether any (all) rows should be returned if this list is empty (default = false)
	  * @return A combination of those conditions using OR
	  */
	def or(conditions: Seq[Condition], resultOnEmpty: => Boolean = false) =
		conditions.find { _.isAlwaysTrue }.getOrElse {
			combine(conditions.filterNot { _.isAlwaysFalse }, "OR", always(resultOnEmpty))
		}
	/**
	  * @param conditions A set of conditions
	  * @param resultOnEmpty Whether any (all) rows should be returned if this list is empty (default = true)
	  * @return A combination of those conditions using AND
	  */
	def and(conditions: Seq[Condition], resultOnEmpty: => Boolean = true) =
		conditions.find { _.isAlwaysFalse }.getOrElse {
			combine(conditions.filterNot { _.isAlwaysTrue }, "AND", always(resultOnEmpty))
		}
	
	private def combine(conditions: Seq[Condition], separator: => String, resultOnEmpty: => Condition) = {
		conditions.emptyOneOrMany match {
			case None => resultOnEmpty
			case Some(Left(only)) => only
			case Some(Right(conditions)) =>
				val actualSeparator = s" $separator "
				val noParentheses = SqlSegment.combine(conditions.map { _.segment }) { _.mkString(actualSeparator) }
				Condition(noParentheses.copy(sql = s"(${ noParentheses.sql })"))
		}
	}
}

/**
  * Conditions can be combined with each other logically and converted to sql where clauses.
  * A where clause is often used after a join or a basic operation.
  * @author Mikko Hilpinen
  * @since 22.5.2017
  * @param segment The sql segment that forms this condition. Doesn't include the "WHERE" -part.
  * @param knownResult Known end result of this condition.
  *                    Unknown by default (when dependent on actual database data).
  */
case class Condition(segment: SqlSegment, knownResult: UncertainBoolean = UncertainBoolean)
{
	// COMPUTED --------------------------
	
	/**
	  * @return Whether this condition always yields true
	  */
	def isAlwaysTrue = knownResult.isCertainlyTrue
	/**
	  * @return Whether this condition always yields false
	  */
	def isAlwaysFalse = knownResult.isCertainlyFalse
	
	/**
	  * Converts this condition into a real sql segment that can function as a where clause
	  */
	def toWhereClause = segment prepend "WHERE"
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = toWhereClause.toString
	
	
	// OTHER    -------------------------
	
	/**
	  * Combines the conditions together using a logical AND. All of the conditions are wrapped in
	  * single parentheses '()' and performed together, from left to right.
	  */
	def &&(others: Seq[Condition]): Condition = Condition.and(this +: others)
	/**
	  * Combines this and another condition together using a logical AND. The conditions are wrapped in
	  * single parentheses '()' and performed together, from left to right.
	  */
	def &&(other: Condition): Condition = Condition.and(Pair(this, other))
	/**
	  * Combines the conditions together using a logical AND. All of the conditions are wrapped in
	  * single parentheses '()' and performed together, from left to right.
	  */
	def &&(first: Condition, second: Condition, more: Condition*): Condition = this && (Pair(first, second) ++ more)
	/**
	  * @param other Another condition
	  * @return A combination (using AND) of these conditions, wrapped in parenthesis '()'
	  */
	def &&(other: Option[Condition]): Condition = this && other.emptyOrSingle
	
	/**
	  * Combines the conditions together using a logical OR. All of the conditions are wrapped in
	  * single parentheses '()' and performed together, from left to right.
	  */
	def ||(others: Seq[Condition]) = Condition.or(this +: others)
	/**
	  * Combines this and another condition together using a logical OR. The conditions are wrapped in
	  * single parentheses '()' and performed together, from left to right.
	  */
	def ||(other: Condition): Condition = Condition.or(Pair(this, other))
	/**
	  * Combines the conditions together using a logical OR. All of the conditions are wrapped in
	  * single parentheses '()' and performed together, from left to right.
	  */
	@deprecated("Deprecated for removal. Please use .or(...) instead", "v1.20")
	def ||(first: Condition, second: Condition, more: Condition*): Condition = this || (Pair(first, second) ++ more)
	/**
	  * @param other Another condition
	  * @return A combination of these conditions (using OR) wrapped in parenthesis '()'
	  */
	def ||(other: Option[Condition]): Condition = this || other.emptyOrSingle
	
	/**
	  * Applies a logical NOT operator on this condition, reversing any logical outcome
	  */
	def unary_! = Condition(segment.copy(sql = s"NOT (${ segment.sql })"), !knownResult)
	
	/**
	  * Combines this and another condition together using a logical XOR. The logical value is true
	  * when both of the conditions have different values
	  */
	def xor(other: Condition) = Condition.combine(Pair(this, other), "XOR", this)
	
	/**
	  * Combines this condition with other conditions using specified operator
	  * @param others Other conditions
	  * @param operator An operator used for combining these two conditions
	  * @return A combination of these conditions
	  */
	def combineWith(others: Seq[Condition], operator: BasicCombineOperator) =
		Condition.combine(this +: others, operator.toSql, this)
	/**
	  * Combines these two conditions with each other using specified operator
	  * @param other Another condition
	  * @param operator Operator used when combining these conditions
	  * @return A combination of these two conditions
	  */
	def combineWith(other: Condition, operator: BasicCombineOperator): Condition = combineWith(Single(other), operator)
}