package utopia.reach.focus

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.Positive
import utopia.flow.util.CollectionExtensions._

/**
  * A component manager which tracks multiple focusable components
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
trait ManyFocusableWrapper extends FocusRequestable with FocusTracking
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Items which can be focused within this wrapper
	  */
	protected def focusTargets: Seq[FocusRequestable with FocusTracking]
	
	
	// IMPLEMENTED	--------------------
	
	override def requestFocus(forceFocusLeave: Boolean, forceFocusEnter: Boolean) =
	{
		val targets = focusTargets
		// If one of the items already has focus, does nothing
		if (targets.exists { _.hasFocus })
			true
		else
			// Moves the focus to the first available item
			targets.headOption match
			{
				case Some(target) => target.requestFocus(forceFocusLeave, forceFocusEnter)
				case None => false
			}
	}
	
	override def hasFocus = focusTargets.exists { _.hasFocus }
	
	
	// OTHER	-----------------------
	
	/**
	  * Moves the focus inside this group
	  * @param direction Direction to which the focus is moved (default = Positive = forward)
	  * @param loop Whether focus should be allowed to loop inside this group (default = false)
	  * @param forceFocusLeave Whether focus leaving should be forced (default = false)
	  * @param forceFocusEnter Whether focus entering should be forced (default = false)
	  * @return Whether focus was changed or is likely to change
	  */
	def moveFocusInside(direction: Sign = Positive, loop: Boolean = false,
	                    forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false) =
	{
		val targets = focusTargets
		// Finds the current focus owner within this group (returns false if didn't have focus)
		targets.indexWhereOption { _.hasFocus }.exists { currentFocusIndex =>
			// Finds the next focus target within this group
			val nextFocusIndex = currentFocusIndex + direction.modifier
			targets.getOption(nextFocusIndex) match
			{
				// Case: New focus target is within this group
				case Some(newTarget) => newTarget.requestFocus(forceFocusLeave, forceFocusEnter)
				// Case: End of targets reached
				case None =>
					// Loops, if allowed
					if (loop && targets.size > 1)
					{
						if (nextFocusIndex < 0)
							targets.last.requestFocus(forceFocusLeave, forceFocusEnter)
						else
							targets.head.requestFocus(forceFocusLeave, forceFocusEnter)
					}
					else
						false
			}
		}
	}
}
