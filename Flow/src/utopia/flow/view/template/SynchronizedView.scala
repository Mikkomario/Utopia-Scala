package utopia.flow.view.template

import utopia.flow.view.immutable.View

/**
 * Common trait for views that can provide synchronized access to their values
 * @author Mikko Hilpinen
 * @since 09.12.2025, v2.8
 */
trait SynchronizedView[+A] extends View[A]
{
	// ABSTRACT ---------------------------
	
	/**
	 * Locks the value in this item from being modified by other threads during a specific operation.
	 *
	 * Use with caution, as careless synchronization may lead to deadlocks.
	 *
	 * @param operation Operation during which this item's value is locked for other threads.
	 *
	 * @tparam B The result type of 'operation'
	 * @return the result of 'operation'
	 */
	def lockWhile[B](operation: => B): B
	
	
	// OTHER    --------------------------
	
	/**
	 * Locks the value in this item from being modified by other threads during a specific operation.
	 *
	 * Use with caution, as careless synchronization may lead to deadlocks.
	 *
	 * @param operation Operation during which this item's value is locked for other threads.
	 *                  Accepts the current value of this item.
	 *
	 * @tparam B The result type of 'operation'
	 * @return the result of 'operation'
	 */
	def viewLocked[B](operation: A => B): B = lockWhile { operation(value) }
}
