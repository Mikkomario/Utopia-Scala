package utopia.flow.event.model

import utopia.flow.operator.Steppable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

/**
 * An enumeration for different priority levels for change listeners and change responses
 * @author Mikko Hilpinen
 * @since 14.12.2025, v2.8
 */
sealed trait ChangeResponsePriority
	extends SelfComparable[ChangeResponsePriority] with Steppable[ChangeResponsePriority]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return An index indicating the order in which these responses should be triggered,
	 *         relative to the other priority levels.
	 */
	protected def triggerOrderIndex: Int
	
	
	// IMPLEMENTED  -----------------------
	
	override def self: ChangeResponsePriority = this
	
	override def compareTo(o: ChangeResponsePriority): Int = o.triggerOrderIndex - triggerOrderIndex
}

object ChangeResponsePriority
{
	// ATTRIBUTES   -----------------------
	
	/**
	 * All change response -priorities in descending order
	 */
	val descending = Vector[ChangeResponsePriority](High, Normal, After)
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return An iterator that yields the priority values in ascending order
	 */
	def ascendingIterator = descending.reverseIterator
	
	
	// VALUES   ---------------------------
	
	/**
	 * Change response -priority used by default.
	 * Listeners and effects with this priority are applied after all high-priority effects have resolved,
	 * which usually means that the mapped pointers, etc. have already been updated.
	 */
	case object Normal extends ChangeResponsePriority
	{
		override protected val triggerOrderIndex: Int = 1
		
		override def next(direction: Sign): ChangeResponsePriority = direction match {
			case Positive => High
			case Negative => After
		}
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	 * The highest change response priority, specifying the listeners and effects that must be updated before
	 * anything else.
	 *
	 * Often used for mapped pointers where it's vital that the information is updated as fast as possible
	 * (and often before other pointers, etc. have been updated).
	 */
	case object High extends ChangeResponsePriority
	{
		override protected val triggerOrderIndex: Int = 0
		
		override def next(direction: Sign): ChangeResponsePriority = direction match {
			case Positive => this
			case Negative => Normal
		}
		override def is(extreme: Extreme): Boolean = extreme == Max
	}
	/**
	 * A low change response -priority used for triggering so-called after-effects.
	 *
	 * Listeners and effects using this priority are more interested in having access to updated information,
	 * rather than being the first ones to respond to a change.
	 *
	 * This is especially useful for responses which don't cause further changes, and don't expose values to others.
	 */
	case object After extends ChangeResponsePriority
	{
		override protected val triggerOrderIndex: Int = 2
		
		override def next(direction: Sign): ChangeResponsePriority = direction match {
			case Positive => Normal
			case Negative => this
		}
		override def is(extreme: Extreme): Boolean = extreme == Min
	}
}