package utopia.flow.view.immutable.eventful

import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.LockablePointer
import utopia.flow.view.template.eventful.Changing

import scala.annotation.unchecked.uncheckedVariance

object DividingMirror
{
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
	def apply[O, L, R](source: Changing[O], initialLeft: => L, initialRight: => R)(divide: O => Either[L, R]) =
		new DividingMirror[O, L, R](source, initialLeft, initialRight)(divide)
}

/**
  * An interface which takes a pointer and divides it into two separate pointers.
  * @tparam O Type of origin pointer's values
  * @tparam L Type of left division result values
  * @tparam R Type of right division result values
  * @author Mikko Hilpinen
  * @since 09.01.2025, v2.5.1
  */
class DividingMirror[-O, +L, +R](source: Changing[O], initialLeft: => L, initialRight: => R)(divide: O => Either[L, R])
{
	// ATTRIBUTES   --------------------------
	
	// UncheckedVariance, because these pointers only receive results from divide (covariant),
	// and the outer interface still provides covariance
	private val (leftP: LockablePointer[L @uncheckedVariance], rightP: LockablePointer[R @uncheckedVariance], lastUpdatedSideP) = {
		val (l1, r1, lastUpdatedSide) = divide(source.value) match {
			case Left(l) => (l, initialRight, First)
			case Right(r) => (initialLeft, r, Last)
		}
		(LockablePointer(l1), LockablePointer(r1), LockablePointer[End](lastUpdatedSide))
	}
	
	/**
	  * A pointer that points to the pointer (left or right) which received the last value update.
	  */
	lazy val lastUpdatedPointer = lastUpdatedSideP.map {
		case First => Left(left)
		case Last => Right(right)
	}
	
	
	// COMPUTED -----------------------------
	
	private implicit def listenerLogger: Logger = source.listenerLogger
	
	/**
	  * @return A pointer that contains the latest left side value
	  */
	def left = leftP.readOnly
	/**
	  * @return A pointer that contains the latest right side value
	  */
	def right = rightP.readOnly
	
	
	// INITIAL CODE -------------------------
	
	// Updates the managed pointers when the origin changes
	source.addContinuousListener { change =>
		divide(change.newValue) match {
			case Left(l) =>
				leftP.value = l
				lastUpdatedSideP.value = First
				
			case Right(r) =>
				rightP.value = r
				lastUpdatedSideP.value = Last
		}
	}
	
	// Once the origin stops changing, locks the managed pointers
	source.onceChangingStops {
		leftP.lock()
		rightP.lock()
		lastUpdatedSideP.lock()
	}
}
