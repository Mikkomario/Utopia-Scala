package utopia.flow.util.result

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.mutable.builder.{BuildNothing, TryCatchBuilder}
import utopia.flow.util.result.PossiblyFailingFutures._collectResults

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PossiblyFailingFutures
{
	// OTHER    --------------------------
	
	/**
	 * @param futures Futures to wrap
	 * @tparam A Type of successful future values
	 * @return An extender for those futures
	 */
	def wrap[A](futures: IterableOnce[Future[MayHaveFailed[A]]]): PossiblyFailingFutures[A, MayHaveFailed[A]] =
		new _PossiblyFailingFutures[A](futures)
	
	/**
	 * Collects the results of 0-n futures into a single collection
	 * @param futuresIter An iterator that yields the futures remaining to be collected
	 * @param builder A builder for constructing the resulting collection (and for capturing failures)
	 * @param append A function which appends an item into a result-builder, performing the necessary flattening, etc.
	 * @param exc Implicit execution context
	 * @tparam A Type of asynchronous results
	 * @tparam S Type of collected success values
	 * @tparam To Type of the built collection
	 * @return A future that resolves once all futures from 'futuresIter' have resolved,
	 *         containing their results built into a single collection.
	 *
	 *         Yields a partial failure if some of the futures failed.
	 *         Yields a full failure if all the futures failed (provided there was at least one future, originally).
	 */
	private def _collectResults[A, S, To](futuresIter: Iterator[Future[A]], builder: TryCatchBuilder[S, To])
	                                     (append: (TryCatchBuilder[S, To], Try[A]) => Unit)
	                                     (implicit exc: ExecutionContext): Future[TryCatch[To]] =
	{
		// Collects items until a pending item is encountered
		futuresIter.find { future =>
			// Case: This process was already completed => Adds the result to the appropriate buffer and moves on
			if (future.isCompleted) {
				append(builder, future.waitFor())
				false
			}
			// Case: This process is still pending => Moves to the next phase
			else
				true
		} match {
			// Case: A pending process found => Prepares a result future based on its result, using recursion
			case Some(pendingFuture) =>
				pendingFuture.toTryFuture.flatMap { result =>
					// Adds the resolved result to the appropriate buffer
					append(builder, result)
					// Continues using recursion
					_collectResults[A, S, To](futuresIter, builder)(append)
				}
			// Case: All items were processed => Builder the resulting collection
			case None => Future.successful(builder.result())
		}
	}
	
	
	// NESTED   ----------------------------
	
	private class _PossiblyFailingFutures[A](override protected val wrapped: IterableOnce[Future[MayHaveFailed[A]]])
		extends PossiblyFailingFutures[A, MayHaveFailed[A]]
	{
		override protected def wrap(result: MayHaveFailed[A]): MayHaveFailed[A] = result
		
		override protected def appendTo(builder: TryCatchBuilder[A, _], result: Try[MayHaveFailed[A]]): Unit =
			result match {
				case Success(result) => builder += result
				case Failure(error) => builder += error
			}
	}
}

/**
 * Common trait for collections that contain futures, which may yield failures.
 * Extended directly by collections that contains Try- or TryCatch-futures.
 * Extended indirectly by collections containing regular futures, in order to prioritize Try & TryCatch -use-cases.
 * @tparam A Type of the values acquired on success
 * @tparam T Type of the asynchronously acquired results (e.g. Try[A])
 * @author Mikko Hilpinen
 * @since 17.12.2025, v2.8
 */
trait PossiblyFailingFutures[+A, T] extends Any
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return The wrapped/extended collection
	 */
	protected def wrapped: IterableOnce[Future[T]]
	
	/**
	 * Wraps a result into a more generic form
	 * @param result Result to wrap
	 * @return A [[MayHaveFailed]] wrapper for that result
	 */
	protected def wrap(result: T): MayHaveFailed[A]
	
	/**
	 * Flattens and appends a result to a [[TryCatchBuilder]]
	 * @param builder Builder to which the result should be appended
	 * @param result Result to append to the builder, wrapped in a [[Try]]
	 */
	protected def appendTo(builder: TryCatchBuilder[A, _], result: Try[T]): Unit
	
	
	// COMPUTED ---------------------------
	
	/**
	 * Asynchronously collects the results of these futures into a single collection
	 * @param exc Implicit execution context
	 * @return A future that resolves into the collected items once all these futures have resolved.
	 *         Will yield a failure if all these futures failed.
	 *         Will yield a partial failure if some, but not all, of these futures failed.
	 */
	def future(implicit exc: ExecutionContext): Future[TryCatch[IndexedSeq[A]]] =
		futureUsing(OptimizedIndexedSeq.newBuilder[A])
	/**
	 * @param context Execution context
	 * @return A future that resolves once all these futures have resolved into either:
	 *              - Success(true): If one or more of these futures succeeded.
	 *                Contains partial failures for every future that failed.
	 *              - Success(false): If this collection was empty
	 *              - Failure: If all these futures failed
	 */
	def completionFuture(implicit context: ExecutionContext) =
		customFutureUsing(BuildNothing.nonEmptyFlag) { !_ }
	
	
	// OTHER    ------------------------
	
	// TODO: Add versions that support timeouts
	/**
	 * Waits until all the futures inside this Iterable item have completed
	 * @return The results of the waiting (each item as a try)
	 */
	def waitFor() = {
		val buffer = OptimizedIndexedSeq.newBuilder[Try[T]]
		buffer ++= wrapped.iterator.map { _.waitFor() }
		buffer.result()
	}
	/**
	 * Waits until all the futures inside this Iterable item have completed
	 * @return The successful results of the waiting (no failures will be included)
	 */
	def waitForResults() = {
		val builder = TryCatchBuilder[A]()
		wrapped.iterator.foreach { future =>
			future.waitFor() match {
				case Success(result) => builder.generic += wrap(result)
				case Failure(error) => builder += error
			}
		}
		builder.result()
	}
	
	/**
	 * Asynchronously collects the results of these futures into a single collection
	 * @param builder A builder for forming the resulting collection from successful results
	 * @param exc Implicit execution context
	 * @tparam To Type of the resulting collection
	 * @return A future that resolves into the collected items once all these futures have resolved.
	 *         Will yield a failure if all these futures failed.
	 *         Will yield a partial failure if some, but not all, of these futures failed.
	 */
	def futureUsing[To <: Iterable[_]](builder: mutable.Builder[A, To])
	                                  (implicit exc: ExecutionContext): Future[TryCatch[To]] =
		customFutureUsing[To](builder) { _.isEmpty }
	/**
	 * Asynchronously collects the results of these futures into a single collection
	 * @param builder A builder for forming the resulting collection from successful results
	 * @param isEmpty A function used for determining whether the built collection is empty.
	 *                May be used for determining whether the result should be a failure or a success.
	 * @param exc Implicit execution context
	 * @tparam To Type of the resulting collection
	 * @return A future that resolves into the collected items once all these futures have resolved.
	 *         Will yield a failure if all these futures failed.
	 *         Will yield a partial failure if some, but not all, of these futures failed.
	 */
	def customFutureUsing[To](builder: mutable.Builder[A, To])(isEmpty: To => Boolean)
	                         (implicit exc: ExecutionContext): Future[TryCatch[To]] =
		_collectResults[T, A, To](wrapped.iterator, new TryCatchBuilder(builder)(isEmpty))(appendTo)
}
