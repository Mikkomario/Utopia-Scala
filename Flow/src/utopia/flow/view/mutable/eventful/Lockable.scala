package utopia.flow.view.mutable.eventful

import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.view.template.eventful.{Changing, MayStopChanging}

object Lockable
{
	/**
	  * Provides a lockable view into another pointer
	  * @param other Another pointer
	  * @tparam A Type of the changing values in the other pointer
	  * @return A view into the other pointer which may be locked,
	  *         after which the changes within 'other' are no longer reflected in this resulting pointer.
	  */
	def view[A](other: Changing[A]) = LockableBridge(other)
}

/**
  * Common trait for changing items that may be "locked".
  * After this item has been locked, it must not be modified anymore,
  * and is considered static in terms of change-listener handling.
  * @author Mikko Hilpinen
  * @since 21.11.2023, v2.3
  */
trait Lockable[+A] extends MayStopChanging[A]
{
	// COMPUTED -----------------------------
	
	/**
	  * @return Whether this pointer has been locked and won't change anymore
	  */
	def locked: Boolean
	
	/**
	  * Locks this pointer, so that it can't be changed anymore
	  */
	def lock(): Unit
	
	
	// IMPLEMENTED  -------------------------
	
	override def destiny: Destiny = if (locked) Sealed else MaySeal
}
