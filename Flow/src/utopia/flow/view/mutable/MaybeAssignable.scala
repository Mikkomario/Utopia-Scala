package utopia.flow.view.mutable

/**
  * Common trait for interfaces which allow assigning of new values under some circumstances,
  * but which don't allow assignments at other times.
  * @tparam A Type of values that may be assigned to this interface
  * @author Mikko Hilpinen
  * @since 31.01.2025, v2.6
  */
trait MaybeAssignable[-A] extends Assignable[A]
{
	/**
	  * @param value Value to assign to this item, if possible (call-by-name)
	  * @return Whether the state of this item was altered
	  */
	def trySet(value: => A): Boolean
}
