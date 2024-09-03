package utopia.vault.util

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.vault.database.{Connection, ConnectionPool}
import utopia.vault.util.DatabaseActionQueue.Action

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

object DatabaseActionQueue
{
	// NESTED   ------------------------
	
	private class Action[A](act: Connection => Try[A])
	{
		// ATTRIBUTES   ----------------
		
		private val promise = Promise[Try[A]]()
		
		val future = promise.future
		
		
		// OTHER  ----------------------
		
		def apply()(implicit connection: Connection): Unit = promise.success(act(connection))
	}
}

/**
  * Queues actions back-to-back instead of performing them in parallel.
  * Uses a shared database connection for the back-to-back actions.
  * @author Mikko Hilpinen
  * @since 22.5.2023, v1.16.1
  */
case class DatabaseActionQueue()(implicit exc: ExecutionContext, cPool: ConnectionPool, log: Logger)
{
	// ATTRIBUTES   ------------------------
	
	private val queue = Volatile.seq[Action[_]]()
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param act An action to perform asynchronously. Accepts a database connection.
	  * @tparam A Type of action result.
	  * @return Future that resolves once the action has been completed.
	  *         May contain a failure.
	  */
	def push[A](act: Connection => A) = _push { c => Try { act(c) } }
	def pushTry[A](act: Connection => Try[A]) = _push { c => Try { act(c) }.flatten }
	
	private def _push[A](act: Connection => Try[A]) = {
		val action = new Action(act)
		// Queues the action and checks whether the queue-emptying process should be initiated
		val shouldStart = queue.mutate { pending =>
			// Case: No actions are pending at this time => Starts the emptying process
			if (pending.isEmpty)
				true -> Single(action)
			// Case: There are already actions pending => Adds to the list of waiting actions
			else
				false -> (pending :+ action)
		}
		// Case: Starts the background process (async)
		if (shouldStart)
			Future {
				// Uses a single connection during this process
				cPool.tryWith { implicit c =>
					// Retrieves new actions as long as there are some available
					// Only removes the actions from the list once they've been completed
					(action +: OptionsIterator.continually {
						queue.mutate { q =>
							// Case: Last action was just completed => Completes
							if (q.hasSize < 2)
								None -> Empty
							// Case: There are more actions to complete => Starts the next one
							else {
								val remaining = q.tail
								Some(remaining.head) -> remaining
							}
						}
					}).foreach { _() }
				}
			}.foreachFailure { error =>
				// TODO: Remove test prints
				println("Encountered an error in DB action queue")
				error.printStackTrace()
				log(error)
			}
		// Returns completion future
		action.future
	}
}