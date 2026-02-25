package utopia.echo.model.enumeration

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.Steppable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign

/**
 * An enumeration for different reasoning effort levels, based on Open AI's system.
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
sealed trait ReasoningEffort
	extends SelfComparable[ReasoningEffort] with Steppable[ReasoningEffort] with ValueConvertible
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Key used for this effort level by Open AI
	 */
	def key: String
	
	/**
	 * @return 0-based index of this reasoning effort, from least to most
	 */
	protected def orderIndex: Int
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Whether reasoning is enabled when using this effort level
	 */
	def reasons = orderIndex >= 0
	
	
	// IMPLEMENTED  --------------------
	
	override def self: ReasoningEffort = this
	override def toValue: Value = key
	
	override def compareTo(o: ReasoningEffort): Int = orderIndex.compareTo(o.orderIndex)
	
	override def next(direction: Sign): ReasoningEffort = {
		if (is(direction.extreme))
			this
		else
			ReasoningEffort.values(orderIndex + direction.modifier)
	}
	override def is(extreme: Extreme): Boolean = extreme match {
		case Min => orderIndex == 0
		case Max => orderIndex == ReasoningEffort.values.size - 1
	}
}

object ReasoningEffort
{
	// ATTRIBUTES   -----------------
	
	val values = Vector[ReasoningEffort](SkipReasoning, Minimal, Low, Medium, High, ExtraHigh)
	
	
	// OTHER    ---------------------
	
	/**
	 * @param key Searched reasoning effort key
	 * @return A value that matches the specified key. None if no value matched.
	 */
	def findForKey(key: String) = {
		val lower = key.toLowerCase
		values.find { _.key == lower }
	}
	
	
	// VALUES   ---------------------
	
	/**
	 * Reasoning is not performed, if possible
	 */
	case object SkipReasoning extends ReasoningEffort
	{
		override val key: String = "none"
		override protected val orderIndex: Int = 0
	}
	/**
	 * Performs no reasoning, or very short reasoning
	 */
	case object Minimal extends ReasoningEffort
	{
		override val key: String = "minimal"
		override protected val orderIndex: Int = 1
	}
	case object Low extends ReasoningEffort
	{
		override val key: String = "low"
		override protected val orderIndex: Int = 2
	}
	case object Medium extends ReasoningEffort
	{
		override val key: String = "medium"
		override protected val orderIndex: Int = 3
	}
	case object High extends ReasoningEffort
	{
		override val key: String = "high"
		override protected val orderIndex: Int = 4
	}
	case object ExtraHigh extends ReasoningEffort
	{
		override val key: String = "xhigh"
		override protected val orderIndex: Int = 5
	}
}
