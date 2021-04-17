package utopia.reflection.event

import utopia.flow.util.SelfComparable

/**
  * An enumeration for different general states of a transition
  * @author Mikko Hilpinen
  * @since 19.8.2020, v1.2
  */
sealed trait TransitionState extends SelfComparable[TransitionState]

object TransitionState
{
	/**
	  * State before a transition is initiated
	  */
	case object NotStarted extends TransitionState
	{
		override def repr = this
		
		override def compareTo(o: TransitionState) = o match
		{
			case NotStarted => 0
			case _ => -1
		}
	}
	
	/**
	  * State while the transition is active
	  */
	case object Ongoing extends TransitionState
	{
		override def repr = this
		
		override def compareTo(o: TransitionState) = o match
		{
			case NotStarted => 1
			case Ongoing => 0
			case Finished => -1
		}
	}
	
	/**
	  * State after the transition has been completed
	  */
	case object Finished extends TransitionState
	{
		override def repr = this
		
		override def compareTo(o: TransitionState) = o match
		{
			case Finished => 0
			case _ => 1
		}
	}
}
