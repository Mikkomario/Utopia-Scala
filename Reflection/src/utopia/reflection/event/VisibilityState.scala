package utopia.reflection.event

import utopia.flow.operator.BinarySigned
import utopia.flow.util.SelfComparable
import utopia.reflection.event.Visibility.{Invisible, Visible}
import utopia.reflection.event.VisibilityChange.{Appearing, Disappearing}

/**
  * An enumeration for different visibility states
  * @author Mikko Hilpinen
  * @since 19.8.2020, v1.2
  */
sealed trait VisibilityState extends SelfComparable[VisibilityState] with BinarySigned[VisibilityState]
{
	// ABSTRACT ---------------------------------
	
	/**
	  * @return A visibility state opposite to this one
	  */
	def opposite: VisibilityState
	
	/**
	  * @return Whether components having this state should be drawn at least partially
	  */
	def isVisible: Boolean
	
	
	// COMPUTED ---------------------------------
	
	/**
	  * @return Whether components having this state are completely invisible
	  */
	def isNotVisible = !isVisible
	
	
	// IMPLEMENTED  -----------------------------
	
	override def unary_- = opposite
}

/**
  * An enumeration for static visibility states
  */
sealed trait Visibility extends VisibilityState with BinarySigned[Visibility]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return A visibility transition used when introducing this visibility state
	  */
	def transitionIn: VisibilityChange
	
	/**
	  * @return A visibility transition used when moving away from this visibility state
	  */
	def transitionOut = transitionIn.opposite
	
	/**
	  * @return A visibility opposite to this one
	  */
	override def opposite: Visibility
	
	
	// IMPLEMENTED  ---------------------------
	
	override def repr = this
	
	override def unary_- = opposite
	
	override def isPositive = isVisible
}

/**
  * An enumeration for transitive visibility states
  */
sealed trait VisibilityChange extends VisibilityState
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return The starting state for this transition
	  */
	def originalState: Visibility
	/**
	  * @return The final state of this transition
	  */
	def targetState: Visibility
	
	/**
	  * @return A change opposite to this one
	  */
	override def opposite: VisibilityChange
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Direction of this visibility change (same as sign)
	  */
	def direction = sign
	
	
	// IMPLEMENTED  -------------------------
	
	override def repr = this
	
	override def isVisible = true
}

object Visibility
{
	/**
	  * A state where an item is fully visible
	  */
	case object Visible extends Visibility
	{
		override def isVisible = true
		
		override def transitionIn = Appearing
		
		override def opposite = Invisible
		
		override def compareTo(o: VisibilityState) = o match
		{
			case Visible => 0
			case _ => 1
		}
	}
	
	/**
	  * A state where an item is invisible (not shown)
	  */
	case object Invisible extends Visibility
	{
		override def isVisible = false
		
		override def transitionIn = Disappearing
		
		override def opposite = Visible
		
		override def compareTo(o: VisibilityState) = o match
		{
			case Invisible => 0
			case _ => -1
		}
	}
}

object VisibilityChange
{
	/**
	  * A state where an item is becoming visible
	  */
	case object Appearing extends VisibilityChange
	{
		override def isPositive = true
		
		override def originalState = Invisible
		override def targetState = Visible
		
		override def opposite = Disappearing
		
		override def compareTo(o: VisibilityState) = o match
		{
			case Visible => -1
			case Appearing => 0
			case _ => 1
		}
	}
	
	/**
	  * A state where an item is becoming invisible
	  */
	case object Disappearing extends VisibilityChange
	{
		override def isPositive = false
		
		override def originalState = Visible
		override def targetState = Invisible
		
		override def opposite = Appearing
		
		override def compareTo(o: VisibilityState) = o match
		{
			case Invisible => 1
			case Disappearing => 0
			case _ => -1
		}
	}
}