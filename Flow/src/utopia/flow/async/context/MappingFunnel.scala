package utopia.flow.async.context

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.{EventfulVolatile, Volatile}
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions

object MappingFunnel
{
	// OTHER    -----------------------
	
	/**
	 * Creates a new funnel
	 * @param capacity Capacity available for parallel mapping operations
	 * @param ordered Whether this funnel is ordered,
	 *                meaning that the items must be processed in the same order in which they were queued.
	 *                Default = false = items will be processed in an order which maximizes the utilized capacity.
	 * @param costOf A function which determines the mapping cost of an individual item.
	 *               Not expected to yield negative values.
	 * @param f A function which accepts an item and processes it asynchronously. Not expected to block.
	 * @param exc Implicit execution context used
	 * @param log Implicit logging implementation used
	 * @tparam A Type of the accepted items
	 * @tparam B Type of mapping output
	 * @return A new mapping funnel
	 */
	def apply[A, B](capacity: Double, ordered: Boolean = false)(costOf: A => Double)(f: A => Future[B])
	               (implicit exc: ExecutionContext, log: Logger) =
		new MappingFunnel[A, B](capacity, ordered)(costOf)(f)
}

/**
  * Used for running a limited number mapping functions in parallel, queueing the remaining tasks.
 * Supports variable mapping cost.
  * @tparam A Type of the mapped items
 * @tparam B Type of the acquired mapping results
 * @author Mikko Hilpinen
  * @since 17.03.2026, v2.8.1
  */
// TODO: Merge common properties between this and ActionQueue under a common trait
class MappingFunnel[-A, +B](capacity: Double, ordered: Boolean)(costOf: A => Double)(f: A => Future[B])
                         (implicit exc: ExecutionContext, log: Logger)
	extends MaybeEmpty[MappingFunnel[A, B]]
{
	// ATTRIBUTES   ------------------
	
	/**
	 * The main managed pointer.
	 * Contains 2 parts:
	 *      1. Queue for tasks that have not been started. Each contains:
	 *          1. The item to map
	 *          1. A promise that accepts the mapping result
	 *          1. Required mapping capacity
	 *      1. Mapping capacity reserved for the currently running mapping operations
	 *
	 * Variance is ignored, because usage is fully controlled / private, and items are only added via .push(A)
	 */
	private val queueAndCapacityP: EventfulVolatile[(Seq[(A @uncheckedVariance, Promise[B @uncheckedVariance], Double)], Double)] =
		Volatile.eventful(Empty -> 0.0)
	
	private val lazyQueueP = Lazy[Changing[Seq[(_, _, Double)]]] { queueAndCapacityP.lightMap { _._1 } }
	private val lazyQueueSizeP = Lazy {
		lazyQueueP.current match {
			case Some(queueP) => queueP.lightMap { _.size }
			case None => queueAndCapacityP.lightMap { _._1.size }
		}
	}
	private val lazyQueuedCapacityP = lazyQueueP.map { _.map { _.iterator.map { _._3 }.sum } }
	private val lazyUtilizationP = Lazy {
		queueAndCapacityP.map { case (queue, usedCapacity) => costOf(queue) + usedCapacity }
	}
	private val lazyPendingToActiveRatioP = Lazy {
		queueAndCapacityP.map { case (queue, usedCapacity) =>
			if (usedCapacity == 0)
				0.0
			else
				costOf(queue) / usedCapacity
		}
	}
	
	/**
	 * @return A flag that contains true while there are actions waiting to be started
	 */
	lazy val pendingFlag: Flag = lazyQueueSizeP.current match {
		case Some(sizeP) => sizeP.lightMap { _ > 0 }
		case None =>
			lazyQueueP.current match {
				case Some(queueP) => queueP.nonEmptyFlag
				case None => queueAndCapacityP.lightMap { _._1.nonEmpty }
			}
	}
	/**
	 * @return A flag that contains true while there are no actions waiting to be started.
	 *         Note: This is not the same as having no actions running.
	 * @see [[emptyFlag]]
	 */
	lazy val notPendingFlag: Flag = !pendingFlag
	
	private val lazyUsedCapacityP = Lazy { queueAndCapacityP.lightMap { _._2 } }
	/**
	 * @return A flag that contains true while this queue is empty
	 */
	lazy val emptyFlag: Flag = lazyUsedCapacityP.current match {
		case Some(capacityP) => capacityP.lightMap { _ <= 0 }
		case None => queueAndCapacityP.lightMap { _._2 <= 0 }
	}
	
	
	// COMPUTED  ----------------------
	
	/**
	 * @return The currently used funnel width / capacity
	 */
	def activeUtilization: Double = queueAndCapacityP.value._2
	/**
	 * @return A pointer that contains the currently used / active mapping capacity
	 */
	def activeUtilizationPointer = lazyUsedCapacityP.value
	/**
	 * @return The total capacity represented by the queued actions
	 */
	def queuedCapacity: Double = lazyQueuedCapacityP.current match {
		case Some(pointer) => pointer.value
		case None => queueAndCapacityP.value._1.iterator.map { _._3 }.sum
	}
	/**
	 * @return A pointer that contains the total capacity required for processing the queued actions
	 */
	def queuedCapacityPointer = lazyQueuedCapacityP.value
	/**
	 * @return The current total utilization, which consists of:
	 *              1. Active utilization (i.e. running processes)
	 *              1. Queued capacity (i.e. pending processes)
	 */
	def utilization = lazyUtilizationP.current match {
		case Some(p) => p.value
		case None =>
			val (queue, usedCapacity) = queueAndCapacityP.value
			costOf(queue) + usedCapacity
	}
	/**
	 * @return A pointer that contains the current total utilization. This consists of:
	 *              1. Active utilization (i.e. running processes)
	 *              1. Queued capacity (i.e. pending processes)
	 */
	def utilizationPointer = lazyUtilizationP.value
	/**
	 * @return The ratio between the cost of pending and active processes.
	 *         E.g. If 2 processes of cost 1 are pending, and one is running, this would yield 2.
	 *
	 *         Yields 0 if there are no pending processes.
	 */
	def pendingToActiveRatio = lazyPendingToActiveRatioP.current match {
		case Some(p) => p.value
		case None =>
			val (queue, usedCapacity) = queueAndCapacityP.value
			if (usedCapacity == 0)
				0.0
			else
				costOf(queue) / usedCapacity
	}
	/**
	 * @return A pointer that contains the ratio between the cost of pending and active processes.
	 *         E.g. If 2 processes of cost 1 are pending, and one is running, this pointer would contain 2.
	 *
	 *         Contains 0 while there are no pending processes.
	 */
	def pendingToActiveRatioPointer = lazyPendingToActiveRatioP.value
	
	/**
	 * @return Number of actions that are currently waiting to be processed, i.e. have not started yet.
	 */
	def pendingCount: Int = queueAndCapacityP.value._1.size
	/**
	 * A pointer that contains the number of queued (waiting) items in this queue at any time.
	 */
	def pendingCountPointer: Changing[Int] = lazyQueueSizeP.value
	
	/**
	 * @return Whether the queue part of this funnel contains at least one item
	 */
	def containsPendingActions = queueAndCapacityP.value._1.nonEmpty
	
	/**
	 * @return A future that resolves once this queue is completely empty,
	 *         i.e. once all the queued actions have fully resolved.
	 */
	def emptyFuture: Future[Any] = emptyFlag.future
	/**
	 * @return A future that resolves once no action in this queue is waiting to be started.
	 *         Note: This doesn't mean that all actions have completed yet.
	 * @see [[emptyFuture]]
	 */
	def notPendingFuture = notPendingFlag.future
	
	
	// IMPLEMENTED  ------------------
	
	override def self: MappingFunnel[A, B] = this
	
	override def isEmpty: Boolean = queueAndCapacityP.value._2 <= 0
	
	
	// OTHER	----------------------
	
	/**
	 * Pushes an item into this funnel
	 * @param item An item to push
	 * @return A future that resolves once that item has been processed
	 */
	def push(item: A): Future[B] = {
		// Calculates the mapping cost
		val requiredCapacity = costOf(item) min capacity
		// Queues or prepares this item for processing
		queueAndCapacityP.mutate { case (queue, usedCapacity) =>
			// Case: There's immediate capacity for processing this item => Reserves capacity for it
			if ((capacity - usedCapacity) >= requiredCapacity && (!ordered || queue.isEmpty))
				None -> (queue -> (usedCapacity + requiredCapacity))
			// Case: There's currently not enough capacity => Queues this item and yields a promise
			else {
				val promise = Promise[B]()
				val updatedQueue = {
					// Case: FIFO => Appends this item to the queue
					if (ordered)
						queue :+ (item, promise, requiredCapacity)
					// Case: Free ordering
					//       => Inserts this item, so that the queue remains ordered from highest to lowest cost
					else
						queue.findIndexWhere { _._3 < requiredCapacity } match {
							case Some(targetIndex) => queue.inserted((item, promise, requiredCapacity), targetIndex)
							case None => queue :+ (item, promise, requiredCapacity)
						}
				}
				Some(promise) -> (updatedQueue -> usedCapacity)
			}
		} match {
			// Case: Task queued => Yields a future from the prepared promise
			case Some(promise) => promise.future
			// Case: Task should be run immediately => Performs the mapping
			case None =>
				val resultFuture = f(item)
				// Once mapping completes, starts emptying the queue
				// (except for 0 cost tasks, which have no effect on the queue)
				if (requiredCapacity != 0)
					resultFuture.onComplete { _ => emptyQueue(requiredCapacity) }
				resultFuture
		}
	}
	
	/**
	 * Clears some used capacity and checks whether more tasks may be initiated.
	 * Should be called whenever a task completes.
	 * @param releasedCapacity Amount of capacity that now became available (due to task completion)
	 */
	private def emptyQueue(releasedCapacity: Double): Unit =
		queueAndCapacityP
			.mutate { case (queue, usedCapacity) =>
				val nowUsedCapacity = usedCapacity - releasedCapacity
				// Case: No more tasks queued => Finishes
				if (queue.isEmpty)
					Empty -> (queue -> nowUsedCapacity)
				else {
					// Checks which tasks may be run next
					val availableCapacity = capacity - nowUsedCapacity
					val (nextItems, remainingItems) = {
						// Case: FIFO => Takes as many queued tasks as possible, but doesn't allow skipping tasks
						if (ordered) {
							val takeCount = queue.foldLeftIterator(0.0) { _ + _._3 }
								.takeWhile { _ <= availableCapacity }.size - 1
							if (takeCount > 0)
								queue.splitAt(takeCount)
							else
								Empty -> queue
						}
						// Case: Free ordering => Takes as many tasks as can be fit into the current capacity
						else {
							val minCost = queue.last._3
							// Case: There's no capacity for any of the queued tasks => Finishes
							if (availableCapacity < minCost)
								Empty -> queue
							// Case: There's capacity for one or more task
							//       => Collects the next tasks, preferring those with the highest cost
							else {
								var remainingCapacity = availableCapacity
								val takenItemsBuilder = OptimizedIndexedSeq.newBuilder[(A, Promise[B], Double)]
								val remainingBuilder = OptimizedIndexedSeq.newBuilder[(A, Promise[B], Double)]
								val itemsIter = queue.iterator
								
								while (remainingCapacity >= minCost && itemsIter.hasNext) {
									val item @ (_, _, cost) = itemsIter.next()
									if (cost > remainingCapacity)
										remainingBuilder += item
									else {
										remainingCapacity -= cost
										takenItemsBuilder += item
									}
								}
								remainingBuilder ++= itemsIter
								
								takenItemsBuilder.result() -> remainingBuilder.result()
							}
						}
					}
					// Updates the queue and the used capacity, accordingly
					nextItems -> (remainingItems -> (nowUsedCapacity + nextItems.iterator.map { _._3 }.sum))
				}
			}
			// Starts all prepared tasks
			.foreach { case (item, promise, cost) =>
				val resultFuture = f(item)
				promise.completeWith(resultFuture)
				// Once each task completes, attempts to further clear the queue
				resultFuture.onComplete { _ => emptyQueue(cost) }
			}
			
	private def costOf(queue: Seq[(_, _, Double)]) = queue.iterator.map { _._3 }.sum
}
