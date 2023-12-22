package utopia.flow.async.context

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.VolatileList

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object ActionQueue
{
	// NESTED   ------------------------
	
	private trait Action[A] extends Runnable
	{
		def future: Future[A]
	}
	
	private class SureAction[A](action: => A) extends Action[A]
	{
		// ATTRIBUTES	------------------
		
		private val promise = Promise[A]()
		
		
		// IMPLEMENTED	------------------
		
		override def future = promise.future
		
		override def run() = promise.complete(Try(action))
	}
	private class UnsureAction[A](action: => Try[A]) extends Action[A]
	{
		// ATTRIBUTES	-----------------
		
		private val promise = Promise[A]()
		
		
		// IMPLEMENTED	-----------------
		
		override def future = promise.future
		
		override def run() = promise.complete(action)
	}
	private class AsyncAction[A](startFuture: => Future[A]) extends Action[A]
	{
		// ATTRIBUTES   ----------------
		
		private val promise = Promise[A]()
		
		
		// IMPLEMENTED  ---------------
		
		override def future = promise.future
		
		override def run() = promise.completeWith(startFuture)
	}
}

/**
  * ActionQueues are used for queueing actions back to back. Multiple actions can still be completed
  * at once
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1.4.1+
  */
class ActionQueue(val maxWidth: Int = 1)(implicit context: ExecutionContext)
{
	import ActionQueue._
	
	// ATTRIBUTES	------------------
	
	private val queue = VolatileList[Action[_]]()
	private val handleCompletions = VolatileList[Future[_]]()
	
	
	// OTHER	----------------------
	
	/**
	  * Pushes a new operation to the end of this queue
	  * @param operation An operation
	  * @tparam A Operation result type
	  * @return A future of the eventual operation completion
	  */
	def push[A](operation: => A): Future[A] = _push(new SureAction(operation))
	/**
	  * Pushes a new operation to the end of this queue
	  * @param operation An operation that returns a Try
	  * @tparam A Operation result type (inside try)
	  * @return A future of the eventual operation completion
	  */
	def pushTry[A](operation: => Try[A]) = _push(new UnsureAction(operation))
	/**
	  * Pushes a new operation to the end of this queue
	  * @param resolvesAsync An operation that completes asynchronously, returning a Future.
	  * @tparam A Type of future the operation resolves into
	  * @return A future that resolves once the specified operation has been performed,
	  *         and its resulting future has resolved.
	  */
	def pushAsync[A](resolvesAsync: => Future[A]) = _push(new AsyncAction[A](resolvesAsync))
	
	/**
	  * Pushes a new operation to this queue. Passes all other operations that have been queued.
	  * @param operation An operation
	  * @tparam A Operation result type
	  * @return A future of the eventual operation completion
	  */
	def prepend[A](operation: => A): Future[A] = _push(new SureAction[A](operation), prepend = true)
	/**
	  * Pushes a new operation to the end of this queue. Passes all other operations that have been queued.
	  * @param resolvesAsync An operation that completes asynchronously, returning a Future.
	  * @tparam A Type of future the operation resolves into
	  * @return A future that resolves once the specified operation has been performed,
	  *         and its resulting future has resolved.
	  */
	def prependAsync[A](resolvesAsync: => Future[A]): Future[A] =
		_push(new AsyncAction[A](resolvesAsync), prepend = true)
	
	private def _push[A](action: Action[A], prepend: Boolean = false) = {
		// Pushes or prepends the action to queue
		if (prepend) queue.update { action +: _ } else queue :+= action
		// Starts additional handlers if possible
		handleCompletions.update { current =>
			val incomplete = current.filterNot { _.isCompleted }
			if (incomplete.hasSize < maxWidth)
				incomplete :+ Future { handle() }
			else
				incomplete
		}
		
		action.future
	}
	
	private def next() = queue.pop()
	
	private def handle() = {
		// Handles actions as long as there are some available
		var action = next()
		while (action.isDefined) {
			action.get.run()
			action = next()
		}
	}
}
