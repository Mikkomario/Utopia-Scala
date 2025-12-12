package utopia.flow.collection.mutable.builder

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.MapParallel
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.async.context.{AccessQueue, ActionQueue}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.mutable.builder.BuilderExtensions._
import utopia.flow.util.TryCatch
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.Volatile

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

/**
 * Used for building collections using multiple parallel threads.
 * @param accessQueue [[AccessQueue]], which provides controlled access to the [[ActionQueue]] utilized
 * @param map Parallel mapping logic applied
 * @param bufferSize Maximum number of pending mapping operations within this queue. Must be positive.
 *                   The default append functions block if the buffer is full.
 *                   Using a larger value may use more memory, but may be (much) more effective otherwise.
 *
 * @tparam I Type of accepted items
 * @tparam A Type of mapped / processed items
 * @tparam To Type of built collections
 * @author Mikko Hilpinen
 * @since 07.12.2025, v2.8
 * @see [[MapParallel]], which are used for constructing these builders
 */
class ParallelBuilder[-I, A, +To](accessQueue: AccessQueue[ActionQueue], map: MapParallel[I, _, A, To], bufferSize: Int)
                                 (implicit exc: ExecutionContext)
	extends mutable.Builder[I, Future[TryCatch[To]]]
{
	// Throws if buffer size is negative
	if (bufferSize <= 0)
		throw new IllegalArgumentException("Buffer size must be positive")
	
	// ATTRIBUTES   --------------------------
	
	/**
	 * Contains a completion future for the last 'addOne' / 'addAll' call.
	 *
	 * The wrapped future resolves once all mapping actions have been queued (i.e. converted to QueuedActions),
	 * but not necessarily started.
	 */
	private val lastAppendFuture = Volatile(Future.successful[Any](()))
	
	/**
	 * A lazily initialized result-builder. Reset whenever this builder is cleared.
	 */
	private val lazyBuilder =
		Lazy.resettable { new VectorBuilder[Future[A]].mapInput { a: QueuedAction[A] => a.future } }
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return Access to append functions which don't block
	 */
	def async: mutable.Builder[I, Future[TryCatch[To]]] = WithoutBlocking
	
	private def _builder = lazyBuilder.value
	
	
	// IMPLEMENTED  --------------------------
	
	override def clear(): Unit = {
		lazyBuilder.reset()
		lastAppendFuture.value = Future.successful(())
	}
	override def result(): Future[TryCatch[To]] = {
		val builder = _builder
		// Waits for past append calls to complete.
		// After this, all mapping operations should be present in 'builder' as result-futures.
		lastAppendFuture.value.flatMap { _ =>
			// Builds the resulting collection from the collected mapping result -futures
			val futures = builder.result()
			builder.clear()
			map.flatten(futures)
		}
	}
	
	override def addOne(elem: I) = {
		// Adds the item and blocks until the append-operation has completed
		addOneAsync(elem).waitUntil()
		this
	}
	override def addAll(elems: IterableOnce[I]) = {
		// Adds all items from the specified collection. Blocks until the whole append process has completed.
		addAllAsync(elems.iterator, BuildNothing.unit).waitUntil()
		this
	}
	
	
	// OTHER    --------------------------
	
	/**
	 * Performs mapping on a single item, asynchronously
	 * @param elem Item to map
	 * @return A future that resolves into the mapping result
	 * @see [[addOne]] or [[async]] if you don't need access to the mapping result
	 */
	def push(elem: I) = addOneAsync(elem).flatMap { _.future }
	/**
	 * Performs mapping on n items, asynchronously
	 * @param elems Items to map
	 * @return A future that resolves into the mapping results, once all mapping operations have completed
	 * @see [[addOne]] or [[async]] if you don't need access to the mapping results
	 */
	def pushAll(elems: IterableOnce[I]) =
		addAllAsync(elems.iterator, OptimizedIndexedSeq.newBuilder).flatMap { _.iterator.map { _.future }.future }
	
	/**
	 * Queues the mapping of a single item
	 * @param elem Item to map
	 * @return A future that resolves once the mapping has been queued (although not necessarily started).
	 *         Yields an action that resolves once the mapping has completed (also tracking the mapping started -state)
	 */
	private def addOneAsync(elem: I) = {
		val builder = _builder
		// Won't start before the previous append operation finishes,
		// but updates the append completion -pointer immediately, so that the next call will de correctly delayed
		lastAppendFuture.mutate { appendFuture =>
			val newAppendFuture = appendFuture.flatMap { _ =>
				// Waits until the action queue is accessible
				accessQueue { queue =>
					// Case: This item may be appended without overfilling the buffer
					//       => queues the mapping immediately
					if (queue.queueSize < bufferSize) {
						val queuedAction = map.push(queue, elem)
						builder += queuedAction
						Future.successful(queuedAction)
					}
					// Case: The queue is currently full
					//       => Waits for the buffer to clear and then queues the mapping operation
					else
						queue.notPendingFuture.map { _ =>
							val queuedAction = map.push(queue, elem)
							builder += queuedAction
							queuedAction
						}
				}
			}
			// Yields the future of this mapping-queueing
			newAppendFuture -> newAppendFuture
		}
	}
	/**
	 * Queues the mapping of 0-n items
	 * @param elemsIter An iterator that yields the items to map
	 * @param resultBuilder A builder for collecting the mapping results
	 * @tparam R Type of the built mapping results
	 * @return A future that resolves once all mapping operations have been queued (although not necessarily started)
	 */
	private def addAllAsync[R](elemsIter: Iterator[I], resultBuilder: mutable.Builder[QueuedAction[A], R]) = {
		if (elemsIter.hasNext) {
			// Waits for the previous append process to finish.
			// Updates the pointer, so that the next call will wait for this operation to finish instead.
			val builder = _builder
			lastAppendFuture.mutate { appendFuture =>
				val newAppendFuture = appendFuture.flatMap { _ =>
					// Waits until the queue may be accessed, then adds the items to the queue in chunks
					accessQueue { queue => _addAllAsync(builder, queue, elemsIter, resultBuilder) }
				}
				newAppendFuture -> newAppendFuture
			}
		}
		// Case: No items to map => Resolves immediately
		else
			Future.successful(resultBuilder.result())
	}
	
	/**
	 * Queues the mapping of 1 or more items
	 * @param builder Builder for collecting the asynchronous mapping results
	 * @param queue Queue to which the mapping operations are placed in order to limit thread-usage
	 * @param elemsIter An iterator that yields the items that are yet to be mapped
	 * @param resultBuilder A builder for the collection built based on this operation
	 * @tparam R Type of the results built
	 * @return A future that resolves once all items from 'elemsIter' have been queued for mapping.
	 */
	private def _addAllAsync[R](builder: mutable.Growable[QueuedAction[A]], queue: ActionQueue, elemsIter: Iterator[I],
	                           resultBuilder: mutable.Builder[QueuedAction[A], R]): Future[R] =
	{
		// Checks how many items may be appended without waiting
		val immediateCapacity = bufferSize - queue.queueSize
		
		// Case: Items may be appended => Adds as many as possible
		if (immediateCapacity > 0) {
			val added = map.pushAll(queue, elemsIter.takeNext(immediateCapacity))
			builder ++= added
			resultBuilder ++= added
		}
		
		// Case: More items remain => Waits until the queue has capacity and continues recursively
		if (elemsIter.hasNext)
			queue.notPendingFuture.flatMap { _ => _addAllAsync(builder, queue, elemsIter, resultBuilder) }
		// Case: All items have now been appended => Resolves
		else
			Future.successful(resultBuilder.result())
	}
	
	
	// NESTED   ---------------------
	
	/**
	 * An interface that implements append operations without blocking
	 */
	private object WithoutBlocking extends mutable.Builder[I, Future[TryCatch[To]]]
	{
		override def addOne(elem: I): WithoutBlocking.this.type = {
			addOneAsync(elem)
			this
		}
		override def addAll(elems: IterableOnce[I]): WithoutBlocking.this.type = {
			addAllAsync(elems.iterator, BuildNothing.unit)
			this
		}
		
		override def result(): Future[TryCatch[To]] = ParallelBuilder.this.result()
		override def clear(): Unit = ParallelBuilder.this.clear()
	}
}
