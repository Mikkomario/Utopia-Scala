package utopia.reflection.event

import utopia.flow.util.RichComparable
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.reflection.event.Visibility.{Invisible, Visible}
import utopia.reflection.event.VisibilityChange.{Appearing, Disappearing}

/**
  * An enumeration for different visibility states
  * @author Mikko Hilpinen
  * @since 19.8.2020, v1.2
  */
sealed trait VisibilityState extends RichComparable[Visibility]
{
	/**
	  * @return A visibility state opposite to this one
	  */
	def opposite: VisibilityState
	
	/**
	  * @return Whether components having this state should be drawn at least partially
	  */
	def isVisible: Boolean
	
	/**
	  * @return Whether components having this state are completely invisible
	  */
	def isNotVisible = !isVisible
}

/**
  * An enumeration for static visibility states
  */
sealed trait Visibility extends VisibilityState
{
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
}

/**
  * An enumeration for transitive visibility states
  */
sealed trait VisibilityChange extends VisibilityState
{
	/**
	  * @return The starting state for this transition
	  */
	def originalState: Visibility
	
	/**
	  * @return The final state of this transition
	  */
	def targetState: Visibility
	
	/**
	  * @return Direction of this transition, in terms of visibility
	  */
	def direction: Direction1D
	
	/**
	  * @return A change opposite to this one
	  */
	override def opposite: VisibilityChange
	
	override def isVisible = true
	
	override def compareTo(o: Visibility) = o match
	{
		case Visible => -1
		case Invisible => 1
	}
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
		
		override def compareTo(o: Visibility) = o match
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
		
		override def compareTo(o: Visibility) = o match
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
		override def originalState = Invisible
		
		override def targetState = Visible
		
		override def direction = Positive
		
		override def opposite = Disappearing
	}
	
	/**
	  * A state where an item is becoming invisible
	  */
	case object Disappearing extends VisibilityChange
	{
		override def originalState = Visible
		
		override def targetState = Invisible
		
		override def direction = Negative
		
		override def opposite = Appearing
	}
}