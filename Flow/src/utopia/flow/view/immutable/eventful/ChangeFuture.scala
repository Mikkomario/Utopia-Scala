package utopia.flow.view.immutable.eventful

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.model.ChangeEvent
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.view.template.eventful.AbstractChanging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object ChangeFuture
{
	/**
	  * Creates a new future with change events
	  * @param placeHolder A placeholder value stored until this future completes
	  * @param future A future
	  * @param merge A merge function that accepts the placeholder value and the future result (as a Try),
	  *              yielding a new value to store.
	  *              Will be called on both successful and failed resolves.
	  * @param exc Implicit execution context.
	  * @tparam A Type of stored value
	  * @tparam F Type of future result, when successful
	  * @return A new change future
	  */
	def merging[A, F](placeHolder: A, future: Future[F])(merge: (A, Try[F]) => A)(implicit exc: ExecutionContext) =
		new ChangeFuture[A, F](placeHolder, future)(merge)
	
	/**
	  * Creates a new change future. Failed future resolves are logged.
	  * @param placeHolder A placeholder value returned until the future resolves (successfully)
	  * @param future      A future
	  * @param exc         Implicit execution context
	  * @param log A logging implementation for recording cases where the future fails
	  * @tparam A Type of held value
	  * @return A new change future.
	  *         This future will continue to contain the placeholder value if the future fails to resolve.
	  */
	def apply[A](placeHolder: A, future: Future[A])(implicit exc: ExecutionContext, log: Logger) =
		merging[A, A](placeHolder, future) { (p, result) =>
			result.getOrMap { error =>
				log(error)
				p
			}
		}
	
	/**
	  * Wraps a future into a change future
	  * @param future A future to wrap
	  * @param placeHolder A placeholder value to use while the future is not completed (call by name)
	  * @param exc Implicit execution context
	  * @tparam A Type of future content
	  * @return A new change future (or wrapped future result)
	  */
	@deprecated("Please use Changing.wrapFuture(...) instead", "v2.0")
	def wrap[A](future: Future[A], placeHolder: => A)(implicit exc: ExecutionContext) =
		future.current match {
			case Some(v) => Fixed(v)
			case None => merging[A, A](placeHolder, future) { (p, result) => result.getOrElse(p) }
		}
	
	/**
	  * Wraps a future, containing None until that future is completed
	  * @param future A future
	  * @param exc Implicit execution context
	  * @tparam A Type of item this future will eventually contain
	  * @return A new change future
	  */
	@deprecated("Please use Changing.wrapFutureToOption(...) instead", "v2.0")
	def noneUntilCompleted[A](future: Future[A])(implicit exc: ExecutionContext) =
		wrap(future.map { Some(_) }, None)
}

/**
  * An asynchronously completing change from one value to another
  * @author Mikko Hilpinen
  * @since 9.12.2020, v1.9
  */
class ChangeFuture[A, F](placeHolder: A, val future: Future[F])(mergeResult: (A, Try[F]) => A)
                        (implicit exc: ExecutionContext)
	extends AbstractChanging[A]
{
	// ATTRIBUTES	------------------------------
	
	private val resultPointer = VolatileOption[A]()
	
	
	// INITIAL CODE	------------------------------
	
	// Changes value when future completes
	future.onComplete { result =>
		val v = mergeResult(placeHolder, result)
		// Updates local value
		resultPointer.setOne(v)
		
		// Generates change events, if needed
		if (v != placeHolder)
			fireEvent(ChangeEvent(placeHolder, v)).foreach { _() }
		
		// Forgets about the listeners and dependencies afterwards
		clearListeners()
	}
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return Whether this future has completed
	  */
	def isCompleted = future.isCompleted
	
	
	// IMPLEMENTED	------------------------------
	
	override def value = resultPointer.value.getOrElse(placeHolder)
	override def isChanging = !isCompleted
	
	override def findMapNextFuture[B](f: A => Option[B]) =
		if (isCompleted) Future.never else findMapFuture(f)
	
	override def map[B](f: A => B) = {
		if (isCompleted)
			Fixed(f(value))
		else
			new ChangeFuture[B, F](f(placeHolder), future)((_, result) => f(mergeResult(value, result)))
	}
}
