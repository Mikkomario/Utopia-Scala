package utopia.flow.util

import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.util.TryExtensions.RichTry
import utopia.flow.view.immutable.View

import scala.language.implicitConversions
import scala.util.{Success, Try}

object MayHaveFailed
{
	// OTHER    ----------------------------
	
	/**
	 * @param value A successfully acquired value
	 * @tparam A Type of the specified value
	 * @return A success wrapper
	 */
	def success[A](value: A) = AlwaysSuccess(value)
	/**
	 * @param error Cause of this failure
	 * @return A failure wrapper
	 */
	def failure[A](error: Throwable): Failure = Failure(error)
	
	
	// NESTED   ---------------------------
	
	/**
	 * Common trait for MayHaveFailed failure representations
	 * @tparam Repr Type of the implementing class
	 */
	trait FailureLike[+Repr <: FailureLike[_]] extends MayHaveFailed[Nothing]
	{
		// ABSTRACT -----------------------
		
		/**
		 * @return The cause of this failure
		 */
		def cause: Throwable
		
		/**
		 * @return This instance
		 */
		def self: Repr
		
		
		// IMPLEMENTED  -------------------
		
		override def isSuccess: Boolean = false
		override def isFailure: Boolean = true
		
		override def success = None
		override def failure: Option[Throwable] = Some(cause)
		
		override def toTry = scala.util.Failure(cause)
		override def toTryCatch = TryCatch.Failure(cause)
		
		override def get: Nothing = throw cause
		
		override def map[B](f: Nothing => B) = self
		override def tryMap[B](f: Nothing => Try[B]) = self
		override def mapOrFail[B](f: Nothing => MayHaveFailed[B]) = self
	}
	
	object Failure
	{
		// OTHER    -----------------------
		
		/**
		 * @param cause The cause of this failure
		 * @return A failure based on the specified cause
		 */
		def apply(cause: Throwable): Failure = new _Failure(cause)
		
		
		// NESTED   -----------------------
		
		private class _Failure(override val cause: Throwable) extends Failure
		{
			override def self: Failure = this
			override def tryMapCatching[B](f: Nothing => TryCatch[B]): MayHaveFailed[B] = this
			override def catching[B >: Nothing](partialFailures: => IterableOnce[Throwable]): MayHaveFailed[B] = this
		}
	}
	/**
	 * Common trait for MayHaveFailed failure representations
	 */
	trait Failure extends FailureLike[Failure]
	
	/**
	 * Wraps a value. Represents a success.
	 * @param value Value to wrap
	 * @tparam A Type of value viewable though this wrapper
	 */
	case class AlwaysSuccess[+A](value: A)
		extends MayHaveFailed[A] with View[A] with MayHaveFailedLike[A, AlwaysSuccess, MayHaveFailed, TryCatch]
	{
		override def isSuccess: Boolean = true
		override def isFailure: Boolean = false
		
		override def success: Option[A] = Some(value)
		override def failure: Option[Throwable] = None
		
		override def toTry: Try[A] = Success(value)
		override def toTryCatch: TryCatch[A] = TryCatch.Success(value)
		
		override def get: A = value
		
		override def catching[B >: A](partialFailures: => IterableOnce[Throwable]): TryCatch[B] =
			TryCatch.Success(value, OptimizedIndexedSeq.from(partialFailures))
		
		override def map[B](f: A => B): AlwaysSuccess[B] = AlwaysSuccess(f(value))
		override def tryMap[B](f: A => Try[B]): RichTry[B] = f(value)
		override def mapOrFail[B](f: A => MayHaveFailed[B]) = f(value)
		override def tryMapCatching[B](f: A => TryCatch[B]): TryCatch[B] = f(value)
	}
}

/**
 * A common trait for result wrappers, which may represent either a success or a failure
 * @author Mikko Hilpinen
 * @since 11.12.2025, v2.8
 */
trait MayHaveFailed[+A] extends Any with MayHaveFailedLike[A, MayHaveFailed, MayHaveFailed, MayHaveFailed]