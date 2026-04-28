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
	// ABSTRACT -----------------------------
	
	/**
	  * @return Whether this pointer has been locked and won't change anymore
	  */
	def locked: Boolean
	
	/**
	  * Locks this pointer, so that it can't be changed anymore
	  */
	def lock(): Unit
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Whether this pointer has not yet been locked
	  */
	def unlocked = !locked
	
	
	// IMPLEMENTED  -------------------------
	
	override def destiny: Destiny = if (locked) Sealed else MaySeal
	
	
	// OTHER    ----------------------------
	
	/**
	 * Fails if this pointer has been locked
	 * @param f A function to call if this pointer is unlocked
	 * @tparam B Type of 'f' results
	 * @throws java.lang.IllegalStateException If this pointer has been locked
	 * @return Results of 'f'
	 */
	@throws[IllegalStateException]("If this pointer has been locked")
	protected def failIfLocked[B](f: => B) =
		if (locked) throw new IllegalStateException("This pointer has been locked") else f
	/**
	 * Runs the specified function if this pointer has not been locked
	 * @param f A function to run
	 * @tparam U Arbitrary 'f' result type
	 * @return Whether 'f' was run
	 */
	protected def ifUnlocked[U](f: => U) = {
		if (locked)
			false
		else {
			f
			true
		}
	}
}
