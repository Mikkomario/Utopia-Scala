package utopia.flow.view.immutable.eventful

import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.{EventfulPointer, LockablePointer}
import utopia.flow.view.template.eventful.Changing

object DividingMirror
{
	// OTHER    ---------------------------------
	
	/**
	  * Creates a new dividing mirror
	  * @param source Pointer that is divided / split
	  * @param initialLeft Initially assigned left value. Only called if the initial source value is right.
	  * @param initialRight Initially assigned right value. Only called if the initial source value is left.
	  * @param divide A function that divides the source value into either a left or a right value.
	  * @tparam O Type of source pointer's values
	  * @tparam L Type of left side results
	  * @tparam R Type of right side results
	  * @return A mirror which provides access to divided value pointers
	  */
	def apply[O, L, R](source: Changing[O], initialLeft: => L, initialRight: => R)
	                  (divide: O => Either[L, R]): DividingMirror[L, R] =
		new _DividingMirror[O, L, R](source, initialLeft, initialRight)(divide)
		
	
	// NESTED   --------------------------------
	
	private class _DividingMirror[-O, L, R](source: Changing[O], initialLeft: => L, initialRight: => R)
	                                       (divide: O => Either[L, R])
		extends DividingMirror[L, R]
	{
		// ATTRIBUTES   --------------------------
		
		private val (leftP, rightP, lastUpdatedSideP) = {
			// Forms the initial values for both sides
			val (l1, r1, lastUpdatedSide) = divide(source.value) match {
				case Left(l) => (l, initialRight, First)
				case Right(r) => (initialLeft, r, Last)
			}
			// Case: Source changes => Prepares to reflect those changes in the managed pointers
			if (source.mayChange) {
				val (lp, rp, sideP) = {
					// Case: Source may stop changing => Prepares to propagate that to the managed pointers, also
					if (source.destiny.isPossibleToSeal) {
						val lp = LockablePointer(l1)
						val rp = LockablePointer(r1)
						val sideP = LockablePointer[End](lastUpdatedSide)
						
						// Once/if the source stops changing, locks the managed pointers to signal it to potential listeners
						source.onceChangingStops {
							lp.lock()
							rp.lock()
							sideP.lock()
						}
						
						(lp, rp, sideP)
					}
					// Case: Source doesn't look like it will stop changing => Uses a more simplified approach
					else {
						val lp = EventfulPointer(l1)
						val rp = EventfulPointer(r1)
						val sideP = EventfulPointer[End](lastUpdatedSide)
						
						(lp, rp, sideP)
					}
				}
				
				// Updates the managed pointers when the origin changes
				source.addContinuousListener { change =>
					divide(change.newValue) match {
						case Left(l) =>
							lp.value = l
							sideP.value = First
						
						case Right(r) =>
							rp.value = r
							sideP.value = Last
					}
				}
				
				(lp, rp, sideP)
			}
			// Case: Source doesn't change => Uses fixed pointers with no reactivity
			else {
				val lp = Fixed(l1)
				val rp = Fixed(r1)
				(lp, rp, Fixed(lastUpdatedSide))
			}
		}
		/**
		  * A pointer that points to the pointer (left or right) which received the last value update.
		  */
		override lazy val lastUpdatedPointerPointer = lastUpdatedSideP.map {
			case First => Left(left)
			case Last => Right(right)
		}
		
		
		// COMPUTED -----------------------------
		
		private implicit def listenerLogger: Logger = source.listenerLogger
		
		override def left = leftP.readOnly
		override def right = rightP.readOnly
		
		override def lastUpdatedSidePointer: Changing[End] = lastUpdatedSideP.readOnly
	}
}

/**
  * An interface which takes a pointer and divides it into two separate pointers.
  * @tparam L Type of left division result values
  * @tparam R Type of right division result values
  * @author Mikko Hilpinen
  * @since 09.01.2025, v2.5.1
  */
trait DividingMirror[+L, +R]
{
	// ABSTRACT   --------------------------
	
	/**
	  * @return A pointer that contains the latest left side value
	  */
	def left: Changing[L]
	/**
	  * @return A pointer that contains the latest right side value
	  */
	def right: Changing[R]
	
	/**
	  * @return A pointer that shows the last updated side
	  */
	def lastUpdatedSidePointer: Changing[End]
	/**
	  * A pointer that points to the pointer (left or right) which received the last value update.
	  */
	def lastUpdatedPointerPointer: Changing[Either[Changing[L], Changing[R]]]
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Last updated pointer. May be on either side.
	  */
	def lastUpdated = lastUpdatedPointerPointer.value
	/**
	  * @return The side of this pointer that was last updated
	  */
	def lastUpdatedSide = lastUpdatedSidePointer.value
}
