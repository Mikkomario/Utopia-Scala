package utopia.flow.collection.mutable.iterator

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.{Process, ProcessState}
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.util.logging.Logger

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

/**
 * An iterator that asynchronously polls a number of items before they are called.
 * Useful in situations where calling source.next() takes a long time.
 * @author Mikko Hilpinen
 * @since 18.10.2023, v2.3
 * @param source Iterator where items are pulled from
 * @param prePollCount How many items should be asynchronously polled and cached in advance (at maximum)?
 * @param exc Implicit execution context utilized in asynchronous queueing
 * @param logger Logger used for logging non-critical failures
 * @tparam A Type of iterated items
 */
class PrePollingIterator[A](source: Iterator[A], prePollCount: Int = 1)(implicit exc: ExecutionContext, logger: Logger)
	extends Iterator[A]
{
	if (prePollCount <= 0)
		throw new IllegalArgumentException(s"PrePollCount must be positive; Specified $prePollCount")
	
	// ATTRIBUTES   -------------------------
	
	// Stores pre-polled items
	private val queue = VolatileList[A]()
	// Asynchronous process for filling the queue. Stops once the queue has been (re)filled.
	private val queueProcess = Process() { _ =>
		while (queue.size < prePollCount && source.hasNext) {
			queue :+= source.next()
		}
	}
	
	
	// INITIAL CODE -------------------------
	
	// Starts queueing immediately
	if (source.hasNext) {
		queueProcess.runAsync()
		// Waits until actually running
		queueProcess.statePointer.futureWhere { _ != ProcessState.NotStarted }.waitFor().get
	}
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasNext: Boolean = queue.nonEmpty || queueProcess.state.isRunning
	
	@tailrec
	override final def next(): A = {
		// Looks for a pre-queued item
		queue.pop() match {
			// Case: Queued item is available => Returns it
			case Some(queued) =>
				// Makes sure the queueing process is active
				if (queueProcess.state.isNotRunning && source.hasNext)
					queueProcess.runAsync()
				queued
			// Case: No queued item is available => Waits for the queueing to complete (may throw)
			case None =>
				if (queueProcess.state.isRunning) {
					queue.futureWhere { _.nonEmpty }.waitFor().get
					// Yields the queued item using recursion
					next()
				}
				else
					throw new IllegalStateException("next() called on an empty iterator")
		}
	}
}
