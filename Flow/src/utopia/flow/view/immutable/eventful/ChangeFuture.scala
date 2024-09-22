package utopia.flow.view.immutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
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
	  * @param log Implicit logging implementation used for handling failures thrown by listeners assigned to this pointer
	  * @tparam A Type of stored value
	  * @tparam F Type of future result, when successful
	  * @return A new change future
	  */
	def merging[A, F](placeHolder: A, future: Future[F])(merge: (A, Try[F]) => A)
	                 (implicit exc: ExecutionContext, log: Logger) =
		new ChangeFuture[A, F](placeHolder, future)(merge)
	
	/**
	  * Creates a new change future. Failed future resolves are logged.
	  * @param placeHolder A placeholder value returned until the future resolves (successfully)
	  * @param future      A future
	  * @param exc         Implicit execution context
	  * @param log A logging implementation for recording cases where the future fails
	  *            or where an assigned listener throws an exception
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
}

/**
  * An asynchronously completing change from one value to another
  * @author Mikko Hilpinen
  * @since 9.12.2020, v1.9
  */
class ChangeFuture[A, F](placeHolder: A, val future: Future[F])(mergeResult: (A, Try[F]) => A)
                        (implicit exc: ExecutionContext, log: Logger)
	extends AbstractChanging[A]
{
	// ATTRIBUTES	------------------------------
	
	private val resultPointer = Volatile.optional[A]()
	
	private var stopListeners: Seq[ChangingStoppedListener] = Empty
	
	
	// INITIAL CODE	------------------------------
	
	// Changes value when future completes
	future.onComplete { result =>
		val v = mergeResult(placeHolder, result)
		// Updates local value
		resultPointer.setOne(v)
		
		// Generates change events, if needed
		if (v != placeHolder)
			fireEvent(ChangeEvent(placeHolder, v)).foreach { effect => Try { effect() }.log }
		
		// Informs the stop listeners
		stopListeners.foreach { _.onChangingStopped() }
		
		// Forgets about the listeners and dependencies afterwards
		clearListeners()
		stopListeners = Empty
	}
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return Whether this future has completed
	  */
	def isCompleted = future.isCompleted
	
	
	// IMPLEMENTED	------------------------------
	
	override def value = resultPointer.value.getOrElse(placeHolder)
	override def destiny: Destiny = if (isCompleted) Sealed else MaySeal
	
	override def readOnly = this
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit =
		stopListeners :+= listener
	
	override def findMapNextFuture[B](f: A => Option[B]) =
		if (isCompleted) Future.never else findMapFuture(f)
	
	override def map[B](f: A => B) = {
		if (isCompleted)
			Fixed(f(value))
		else
			new ChangeFuture[B, F](f(placeHolder), future)((_, result) => f(mergeResult(value, result)))
	}
}
