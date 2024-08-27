package utopia.flow.view.mutable.async

import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.flow.view.template.eventful.Flag

object VolatileFlag
{
	/**
	  * @param initialState Initial state to assign to this flag (default = false)
	  * @param log Implicit logging implementation used for handling errors throw by assigned listeners
	  * @return A new thread-safe resettable flag
	  */
	def apply(initialState: Boolean = false)(implicit log: Logger): VolatileFlag = new VolatileFlag(initialState)
}

/**
  * A thread-safe [[ResettableFlag]] implementation
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
class VolatileFlag(initialState: Boolean = false)(implicit listenerLogger: Logger)
	extends EventfulVolatile[Boolean] with VolatileSwitch with ResettableFlag
{
	// ATTRIBUTES   -----------
	
	@volatile private var _value: Boolean = initialState
	
	override lazy val view: Flag = new FlagView(this)
	
	
	// IMPLEMENTED  -----------
	
	override def value: Boolean = _value
	
	override protected def assignWithoutEvents(newValue: Boolean): Unit = _value = newValue
	
	
	// OTHER    ---------------
	
	/**
	  * Unless this flag is currently set, performs the specified operation.
	  * Locks this container during this operation.
	  *
	  * Use with caution, as careless locking / synchronization may lead to deadlocks.
	  *
	  * @param operation Function called (synchronously), if this flag is currently not set
	  * @tparam U Arbitrary function result type
	  */
	def lockWhileIfNotSet[U](operation: => U): Unit = lockWhile { v => if (!v) operation }
	
	/**
	  * If this flag is not currently set, runs the specified function and then sets this switch
	  * @param f Function to call, if this switch is not currently set
	  * @tparam A function result type
	  * @return If this flag was set, returns None, otherwise returns the specified function's return value.
	  */
	def lockAndSetIfNotSet[A](f: => A) = mutate { isSet =>
		val result = if (isSet) None else Some(f)
		result -> true
	}
	
	/**
	  * If this flag is not set, performs the operation. Locks this flag while the operation runs.
	  */
	@deprecated("Renamed to lockWhileIfNotSet", "v2.5")
	def doIfNotSet[U](action: => U) = lockWhileIfNotSet(action)
	/**
	  * If this flag is not set, performs the operation and then sets this flag.
	  * Locks this flag during the operation.
	  */
	@deprecated("Renamed to lockAndSetIfNotSet(...) (notice the different return value)", "v2.5")
	def runAndSet[U](action: => U) = lockAndSetIfNotSet(action).isDefined
	/**
	  * Sets this flag and performs the provided function, but only if this flag is not yet set
	  * @param f A function for producing a result
	  * @tparam B Result type
	  * @return Result if this flag was not set. None otherwise.
	  */
	@deprecated("Renamed to lockAndSetIfNotSet(...)", "v2.5")
	def mapAndSet[B](f: => B) = lockAndSetIfNotSet(f)
}
