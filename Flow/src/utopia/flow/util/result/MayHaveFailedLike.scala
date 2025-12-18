package utopia.flow.util.result

import utopia.flow.collection.immutable.{Empty, Single}

import scala.util.Try

/**
 * A common trait for result wrappers, which may represent either a success or a failure
 * @tparam A Type of the wrapped success value, when applicable
 * @tparam R Type of mapping result wrappers
 * @tparam TR Type of tryMap result wrappers
 * @tparam TCR Type of tryMapCatching result wrappers
 * @author Mikko Hilpinen
 * @since 17.12.2025, v2.8
 */
trait MayHaveFailedLike[+A, +R[_], +TR[_], +TCR[_]] extends Any
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
	 * @throws Exception If this is a failure
	 * @return The wrapped success value.
	 */
	@throws[Exception]("If this is a failure")
	def get: A
	
	/**
	 * @param partialFailures Partial failures to include
	 * @tparam B Type of the success result
	 * @return A copy of this result, including the specified partial failures
	 */
	def catching[B >: A](partialFailures: => IterableOnce[Throwable]): TCR[B]
	
	/**
	 * @param f A mapping function to apply, if this is a success
	 * @tparam B Type of mapping results
	 * @return A mapped copy of this result
	 */
	def map[B](f: A => B): R[B]
	/**
	 * @param f A mapping function to apply, if this is a success.
	 *          May yield a failure, in which case a failure will be returned.
	 * @tparam B Type of mapping results, if successful
	 * @return A mapped copy of this result
	 */
	def tryMap[B](f: A => Try[B]): TR[B]
	/**
	 * @param f A mapping function to apply, if this is a success.
	 *          May yield a failure, in which case a failure will be returned.
	 * @tparam B Type of mapping results, if successful
	 * @return A mapped copy of this result
	 */
	def mapOrFail[B](f: A => MayHaveFailed[B]): TR[B]
	/**
	 * @param f A mapping function to apply, if this is a success.
	 *          May yield a full or a partial failure.
	 *          If a full failure is returned, this returns a failure.
	 * @tparam B Type of mapping results, if successful
	 * @return A mapped copy of this result
	 */
	def tryMapCatching[B](f: A => TryCatch[B]): TCR[B]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return All failures (full & partial) associated with this result
	 */
	def failures: Seq[Throwable] = failure match {
		case Some(error) => Single(error)
		case None => Empty
	}
	
	/**
	 * @param ev Implicit evidence that this result contains another result
	 * @tparam B Type of the wrapped result's value on success
	 * @return A flattened version of this result
	 */
	def flatten[B](implicit ev: A <:< MayHaveFailed[B]) = mapOrFail { r => r }
	/**
	 * @param ev Implicit evidence that this result contains a TryCatch
	 * @tparam B Type of the wrapped result's value on success
	 * @return A flattened version of this result
	 */
	def flattenCatching[B](implicit ev: A <:< TryCatch[B]) = tryMapCatching { r => r }
}
