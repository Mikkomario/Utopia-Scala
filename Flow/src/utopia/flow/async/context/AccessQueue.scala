package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
 * Provides access to an item for one caller at a time, while the other callers wait in a queue.
 * @param value The value to which processes are given access one at a time
 * @tparam A Type of the value that may be accessed by one viewer/process at a time
 * @author Mikko Hilpinen
 * @since 12.12.2025, v2.8
 */
class AccessQueue[+A](value: A)(implicit exc: ExecutionContext)
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * Queues all pending processes (i.e. completion futures) here.
	 * The completion futures are wrapped in Views,
	 * because they're sometimes initialized outside of this Volatile pointer.
	 */
	private val queue = Volatile.emptySeq[View[Future[_]]]
	
	
	// OTHER    --------------------------
	
	/**
	 * Queues access to the protected item
	 * @param f A function called once the wrapped item is accessible.
	 *          Yields a future that resolves once access may be given to the next item in queue.
	 *
	 *          Reference to this function's input value should not be stored anywhere
	 *          outside this function's lifecycle.
	 *
	 * @tparam B Type of the asynchronous result of 'f'
	 * @return A future that resolves once 'f' has fully completed
	 */
	def apply[B](f: A => Future[B]) =
		_apply[View[Future[B]]] {
			// Case: Immediately available => 'f' is called lazily, in order to not block access to this queue pointer
			val lazyFuture = Lazy { f(value) }
			lazyFuture -> lazyFuture
			
		} { previous =>
			// Case: Previous processes need to be resolved first => Call's 'f' once the previous future completes
			val accessPromise = Promise[A]()
			previous.onComplete { _ => accessPromise.success(value) }
			
			val completionFutureView = View.fixed(accessPromise.future.flatMap(f))
			completionFutureView -> completionFutureView
		}.value // Starts 'f', if not queued already
	
	/**
	 * Waits until the protected item is accessible, then calls the specified function.
	 *
	 * Note: This function may block for extended time periods.
	 *       In order to not block the threads, it is recommended to use [[apply]] instead.
	 *
	 * @param f A function called once this queue is empty.
	 *          Receives the protected value.
	 *          Expected to block while processing this value.
	 *          This function must not store this value anywhere else.
	 *
	 *          Exceptions thrown by 'f' will be thrown by this function.
	 *
	 * @tparam B Result type of 'f'
	 * @return The result of 'f'
	 */
	def blocking[B](f: A => B) = {
		// Prepares a promise that marks the completion of 'f'
		val completionPromise = Promise[Unit]()
		val completionFutureView = View.fixed(completionPromise.future)
		
		// Updates the queue, checking for the previous operation completion
		val previousCompletion =
			_apply[Option[Future[_]]] { None -> completionFutureView } { Some(_) -> completionFutureView }
		
		// Waits until the previous operation completes
		previousCompletion.foreach { _.waitFor() }
		
		// Runs 'f' and resolves the completion promise
		val result = Try { f(value) }
		completionPromise.success(())
		
		// Returns the result of 'f'
		result.get
	}
	
	// Updates the queue
	private def _apply[B](immediate: => (B, View[Future[_]]))(pending: Future[_] => (B, View[Future[_]])) =
		queue.mutate { queue =>
			// Discards processes that have already completed
			val remaining = queue.dropWhile { _.value.isCompleted }
			remaining.lastOption match {
				// Case: Processes are currently pending
				//       => Prepares to resolve once the previous process completes
				case Some(previous) =>
					val (result, completionView) = pending(previous.value)
					result -> (remaining :+ completionView)
				
				// Case: No processes are currently pending => Prepares to resolve immediately
				case None =>
					val (result, completionView) = immediate
					result -> Single(completionView)
			}
		}
}
