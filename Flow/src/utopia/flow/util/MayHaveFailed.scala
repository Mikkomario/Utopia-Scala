package utopia.flow.util

import utopia.flow.view.immutable.View

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
 * An enumeration for different types of operation results:
 *      1. Try (Success or Failure)
 *      1. TryCatch (success, failure or partial failure)
 *      1. Success (always)
 * @author Mikko Hilpinen
 * @since 11.12.2025, v2.8
 */
sealed trait MayHaveFailed[+A]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Whether this represents a successful operation result
	 */
	def isSuccess: Boolean
	/**
	 * @return Whether this represents a failure
	 */
	def isFailure: Boolean
	
	/**
	 * @return If this is a success, yields the resolved value. Yields None if this is a failure.
	 */
	def success: Option[A]
	/**
	 * @return If this is a failure, yields the cause of this failure. Yields None if this is a success.
	 */
	def failure: Option[Throwable]
	
	/**
	 * @return A Try based on this result
	 */
	def toTry: Try[A]
	/**
	 * @return A TryCatch based on this result
	 */
	def toTryCatch: TryCatch[A]
	
	/**
	 * @return The wrapped success value.
	 *         Throws if this is a failure.
	 */
	def get: A
	
	/**
	 * @param f A mapping function to apply, if this is a success
	 * @tparam B Type of mapping results
	 * @return A mapped copy of this result
	 */
	def map[B](f: A => B): MayHaveFailed[B]
	/**
	 * @param f A mapping function to apply, if this is a success.
	 *          May yield a failure, in which case a failure will be returned.
	 * @tparam B Type of mapping results, if successful
	 * @return A mapped copy of this result
	 */
	def tryMap[B](f: A => Try[B]): MayHaveFailed[B]
	/**
	 * @param f A mapping function to apply, if this is a success.
	 *          May yield a full or a partial failure.
	 *          If a full failure is returned, this returns a failure.
	 * @tparam B Type of mapping results, if successful
	 * @return A mapped copy of this result
	 */
	def tryMapCatching[B](f: A => TryCatch[B]): MayHaveFailed[B]
}

object MayHaveFailed
{
	// IMPLICIT ----------------------------
	
	// implicitly wraps Try and TryCatch
	implicit def apply[A](result: Try[A]): MayHaveFailed[A] = WrapTry(result)
	implicit def apply[A](result: TryCatch[A]): MayHaveFailed[A] = WrapTryCatch(result)
	
	
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
	def failure(error: Throwable) = WrapTry(Failure(error))
	
	
	// VALUES   ----------------------------
	
	/**
	 * Wraps a Try
	 * @param wrapped The wrapped Try
	 * @tparam A Type of the wrapped value, if successful
	 */
	case class WrapTry[+A](wrapped: Try[A]) extends MayHaveFailed[A]
	{
		override def isSuccess: Boolean = wrapped.isSuccess
		override def isFailure: Boolean = wrapped.isFailure
		
		override def success: Option[A] = wrapped.toOption
		override def failure: Option[Throwable] = wrapped match {
			case Failure(error) => Some(error)
			case _ => None
		}
		
		override def toTry: Try[A] = wrapped
		override def toTryCatch: TryCatch[A] = TryCatch.wrap(wrapped)
		
		override def get: A = wrapped.get
		
		override def map[B](f: A => B): MayHaveFailed[B] = apply(wrapped.map(f))
		override def tryMap[B](f: A => Try[B]): MayHaveFailed[B] = apply(wrapped.flatMap(f))
		override def tryMapCatching[B](f: A => TryCatch[B]): MayHaveFailed[B] = wrapped match {
			case Success(value) => apply(f(value))
			case Failure(error) => MayHaveFailed.failure(error)
		}
	}
	
	/**
	 * Wraps a TryCatch
	 * @param wrapped The wrapped TryCatch
	 * @tparam A Type of the wrapped value, if successful
	 */
	case class WrapTryCatch[+A](wrapped: TryCatch[A]) extends MayHaveFailed[A]
	{
		override def isSuccess: Boolean = wrapped.isSuccess
		override def isFailure: Boolean = wrapped.isFailure
		
		override def success: Option[A] = wrapped.success
		override def failure: Option[Throwable] = wrapped.failure
		
		override def toTry: Try[A] = wrapped.toTry
		override def toTryCatch: TryCatch[A] = wrapped
		
		override def get: A = wrapped.get
		
		override def map[B](f: A => B): MayHaveFailed[B] = apply(wrapped.map(f))
		override def tryMap[B](f: A => Try[B]): MayHaveFailed[B] =
			wrapped match {
				case TryCatch.Success(value, partialFailures) =>
					if (partialFailures.isEmpty)
						apply(f(value))
					else
						f(value) match {
							case Success(mapped) => apply(TryCatch.Success(mapped, partialFailures))
							case Failure(error) => MayHaveFailed.failure(error)
						}
				case TryCatch.Failure(error) => MayHaveFailed.failure(error)
			}
		override def tryMapCatching[B](f: A => TryCatch[B]): MayHaveFailed[B] = apply(wrapped.flatMap(f))
	}
	
	/**
	 * Wraps a value. Represents a success.
	 * @param value Value to wrap
	 * @tparam A Type of value viewable though this wrapper
	 */
	case class AlwaysSuccess[+A](value: A) extends MayHaveFailed[A] with View[A]
	{
		override def isSuccess: Boolean = true
		override def isFailure: Boolean = false
		
		override def success: Option[A] = Some(value)
		override def failure: Option[Throwable] = None
		
		override def toTry: Try[A] = Success(value)
		override def toTryCatch: TryCatch[A] = TryCatch.Success(value)
		
		override def get: A = value
		
		override def map[B](f: A => B): MayHaveFailed[B] = AlwaysSuccess(f(value))
		override def tryMap[B](f: A => Try[B]): MayHaveFailed[B] = apply(f(value))
		override def tryMapCatching[B](f: A => TryCatch[B]): MayHaveFailed[B] = apply(f(value))
	}
}
