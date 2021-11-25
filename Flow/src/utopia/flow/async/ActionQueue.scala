package utopia.flow.async

import utopia.flow.collection.VolatileList

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

/**
  * ActionQueues are used for queueing actions back to back. Multiple actions can still be completed
  * at once
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1.4.1+
  */
class ActionQueue(val maxWidth: Int = 1)(implicit context: ExecutionContext)
{
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
	def push[A](operation: => A): Future[A] = push(new SureAction(() => operation))
	
	/**
	  * Pushes a new operation to the end of this queue
	  * @param operation An operation that returns a Try
	  * @tparam A Operation result type (inside try)
	  * @return A future of the eventual operation completion
	  */
	def pushTry[A](operation: => Try[A]) = push(new UnsureAction(() => operation))
	
	private def push[A](action: Action[A]) =
	{
		// Pushes the action to queue
		queue :+= action
		
		// Starts additional handlers if possible
		handleCompletions.update
		{
			current =>
				val incomplete = current.filterNot { _.isCompleted }
				if (incomplete.size < maxWidth)
					incomplete :+ Future { handle() }
				else
					incomplete
		}
		
		action.future
	}
	
	private def next() = queue.pop()
	
	private def handle() =
	{
		// Handles actions as long as there are some available
		var action = next()
		
		while (action.isDefined)
		{
			action.get.run()
			action = next()
		}
	}
}

private trait Action[A] extends Runnable
{
	def future: Future[A]
}

private class SureAction[A](private val getResult: () => A) extends Action[A]
{
	// ATTRIBUTES	------------------
	
	private val promise = Promise[A]()
	
	
	// IMPLEMENTED	------------------
	
	override def future = promise.future
	
	override def run() = promise.complete(Try(getResult()))
}

private class UnsureAction[A](private val getResult: () => Try[A]) extends Action[A]
{
	// ATTRIBUTES	-----------------
	
	private val promise = Promise[A]()
	
	
	// IMPLEMENTED	-----------------
	
	override def future = promise.future
	
	override def run() = promise.complete(getResult())
}
