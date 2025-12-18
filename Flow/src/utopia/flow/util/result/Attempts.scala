package utopia.flow.util.result

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq

import scala.collection.mutable

/**
 * Common trait for collections / collection-extensions that provide functions
 * for dealing with multiple possibly failed attempts at once.
 * @tparam A Type of successfully acquired results
 * @tparam T Type of the contained result-wrappers (e.g. Try[A])
 * @tparam R Type of the generic result-containers (e.g. Try)
 * @author Mikko Hilpinen
 * @since 17.12.2025, v2.8
 */
trait Attempts[+A, T, +R[_]] extends Any
{
	// ABSTRACT --------------------------
	
	/**
	 * @return An iterator that yields all values in this collection
	 */
	protected def iterator: Iterator[T]
	
	/**
	 * Wraps a result as a [[MayHaveFailed]]
	 * @param result Result to wrap
	 * @return A MayHaveFailed wrapping that result
	 */
	protected def wrap(result: T): MayHaveFailed[A]
	/**
	 * Converts a [[MayHaveFailed]] back to a result
	 * @param result Result to unwrap
	 * @tparam B Type of the contained value, on success
	 * @return An unwrapped result
	 */
	protected def unwrap[B](result: MayHaveFailed[B]): R[B]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return The first success value in this collection.
	 *         None if this collection was empty or only contained failures.
	 */
	def firstSuccess = iterator.findMap { wrap(_).success }
	/**
	 * @return The first failure in this collection.
	 *         None if this collection was empty or only contained successes.
	 */
	def firstFailure = iterator.findMap { wrap(_).failure }
	
	/**
	 * @return The first success in this collection.
	 *         All failures before that success are collected as partial failures, and included in the result.
	 *         If this collection was empty, yields TryCatch.Success(None).
	 *         If this collection only contained failures, yields TryCatch.Failure,
	 *         with the error from the first encountered failure.
	 */
	def firstSuccessCatching: TryCatch[Option[A]] = {
		val iter = iterator
		if (iter.hasNext) {
			// Collects failures while looking for a success value
			val failuresBuilder = OptimizedIndexedSeq.newBuilder[Throwable]
			var success: Option[A] = None
			
			do {
				val nextResult = wrap(iter.next())
				nextResult.success match {
					case Some(value) => success = Some(value)
					case None => failuresBuilder ++= nextResult.failures
				}
				
			} while (success.isEmpty && iter.hasNext)
			
			// Generates the final result
			val failures = failuresBuilder.result()
			success match {
				// Case: A successful value found => Success(Some)
				case Some(value) => TryCatch.Success(Some(value), failures)
				// Case: Only failures found => Failure
				case None =>
					failures.headOption match {
						case Some(error) => TryCatch.Failure(error)
						// Exception: If not failures were collected for some reason, yields Success(None)
						case None => TryCatch.Success(None, failures)
					}
			}
		}
		// Case: Empty collection => Yields Success(None)
		else
			TryCatch.Success(None)
	}
	
	/**
	 * @return An iterator that yields the successful values in this collection
	 */
	def successesIterator = iterator.flatMap { wrap(_).success }
	/**
	 * @return An iterator that yields the failures contained within this collection
	 */
	def failuresIterator = iterator.flatMap { wrap(_).failure }
	
	/**
	 * Divides this collection into successes and failures
	 * @return Returns 2 collections:
	 *              1. All encountered failures (including partial failures, if applicable)
	 *              1. All successfully acquired values
	 */
	def divided = iterator.splitFlatMap { result =>
		val r = wrap(result)
		r.failures -> r.success
	}
	
	/**
	 * Converts this collection into either a success or a failure
	 * @return If this collection *only* contained successes,
	 *         yields a successfully flattened version of this collection.
	 *
	 *         However, if this collection contained even a single failure, yields that failure instead.
	 *
	 * @see [[toTryCatch]], if you don't want this process to fail on individual failures
	 */
	def tryFlatten[B >: A] = tryFlattenTo(OptimizedIndexedSeq.newBuilder[B])
	/**
	 * Converts this collection into either a success or a failure.
	 * Flattens the successful values into a single collection.
	 * @return If this collection *only* contained successes,
	 *         yields a successfully flattened version of this collection.
	 *
	 *         However, if this collection contained even a single failure, yields that failure instead.
	 *
	 * @see [[flattenCatching]], if you don't want this process to fail on individual failures
	 */
	def tryFlattenEach[B](implicit ev: A <:< IterableOnce[B]) =
		tryFlattenEachTo(OptimizedIndexedSeq.newBuilder[B])
	
	/**
	 * Converts this collection into a TryCatch by separating the successful & failed results
	 * @return If this collection contained one or more successes, yields TryCatch.Success,
	 *         including encountered failures as partial failures.
	 *         If this collection was empty, yields an empty TryCatch.Success.
	 *         If this collection only contained failures, yields TryCatch.Failure.
	 */
	def toTryCatch = toTryCatchUsing(OptimizedIndexedSeq.newBuilder)
	/**
	 * Converts this collection into a TryCatch by separating the successful & failed results.
	 * Flattens the successful values into a single collection.
	 * @return If this collection contained one or more successes, yields TryCatch.Success,
	 *         including encountered failures as partial failures.
	 *         If this collection was empty, yields an empty TryCatch.Success.
	 *         If this collection only contained failures, yields TryCatch.Failure.
	 */
	def flattenCatching[B](implicit ev: A <:< IterableOnce[B]) =
		flattenCatchingTo(OptimizedIndexedSeq.newBuilder[B])
	
	
	// OTHER    ---------------------------
	
	/**
	 * Converts this collection into either a success or a failure
	 * @param builder Builder for collecting successfully acquired values
	 * @tparam To Type of the collection built from successful values
	 * @return If this collection *only* contained successes,
	 *         yields a successfully flattened version of this collection.
	 *
	 *         However, if this collection contained even a single failure, yields that failure instead.
	 *
	 * @see [[toTryCatchUsing]], if you don't want this process to fail on individual failures
	 */
	def tryFlattenTo[To](builder: mutable.Builder[A, To]) = _tryFlattenTo(builder) { builder ++= _ }
	/**
	 * Converts this collection into either a success or a failure while flattening the successful values
	 * @param builder Builder for collecting successfully acquired values
	 * @tparam To Type of the collection built from successful values
	 * @return If this collection *only* contained successes,
	 *         yields a successfully flattened version of this collection.
	 *
	 *         However, if this collection contained even a single failure, yields that failure instead.
	 *
	 * @see [[flattenCatchingTo]], if you don't want this process to fail on individual failures
	 */
	def tryFlattenEachTo[B, To](builder: mutable.Builder[B, To])(implicit ev: A <:< IterableOnce[B]) =
		_tryFlattenTo(builder) { _.foreach { builder ++= _ } }
	
	/**
	 * Converts this collection into a TryCatch by separating the successful & failed results
	 * @param builder A builder for the collection consisting of the successfully acquired values
	 * @tparam To Type of the built success-collection
	 * @return If this collection contained one or more successes, yields TryCatch.Success,
	 *         including encountered failures as partial failures.
	 *         If this collection was empty, yields an empty TryCatch.Success.
	 *         If this collection only contained failures, yields TryCatch.Failure.
	 */
	def toTryCatchUsing[To <: Iterable[_]](builder: mutable.Builder[A, To]) = {
		val resultBuilder = TryCatch.builder.wrap(builder)
		resultBuilder.generic ++= iterator.map(wrap)
		resultBuilder.result()
	}
	/**
	 * Converts this collection into a TryCatch by separating the successful & failed results.
	 * Flattens the successful values into a single collection.
	 * @param builder A builder for the collection consisting of the successfully acquired values
	 * @tparam To Type of the built success-collection
	 * @return If this collection contained one or more successes, yields TryCatch.Success,
	 *         including encountered failures as partial failures.
	 *         If this collection was empty, yields an empty TryCatch.Success.
	 *         If this collection only contained failures, yields TryCatch.Failure.
	 */
	def flattenCatchingTo[B, To <: Iterable[_]](builder: mutable.Builder[B, To])(implicit ev: A <:< IterableOnce[B]) = {
		val resultBuilder = TryCatch.builder.wrap(builder)
		iterator.foreach { result =>
			val r = wrap(result)
			r.success.foreach { resultBuilder.fromSuccesses ++= _ }
			resultBuilder.fromFailures ++= r.failures
		}
		resultBuilder.result()
	}
	
	/**
	 * Maps all successfully acquired values in this collection
	 * @param f A mapping function to apply
	 * @tparam B Type of mapping results
	 * @return An iterator of this collection where all successful values have been mapped
	 */
	def mapSuccessesIterator[B](f: A => B) = iterator.map { map(_) { _.map(f) } }
	
	private def _tryFlattenTo[B, To](builder: mutable.Builder[B, To])(append: Option[A] => Unit) = {
		// Iterates & builds until a failure is encountered
		val iter = iterator
		var failure: Option[R[To]] = None
		
		while (failure.isEmpty && iter.hasNext) {
			val nextResult = wrap(iter.next())
			if (nextResult.isFailure)
				failure = Some(mapFailure[A, To](nextResult))
			else
				append(nextResult.success)
		}
		
		// If a failure was encountered, yields it; Otherwise builds the success collection.
		failure.getOrElse { unwrap(MayHaveFailed.success(builder.result())) }
	}
	
	private def map[B](result: T)(f: MayHaveFailed[A] => MayHaveFailed[B]) = unwrap(f(wrap(result)))
	private def mapFailure[B >: A, C](failureResult: MayHaveFailed[B]): R[C] =
		unwrap(failureResult.map { _ => throw new IllegalStateException("Trying to apply mapFailure to a success") })
}
