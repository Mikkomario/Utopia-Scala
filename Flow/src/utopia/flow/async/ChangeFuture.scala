package utopia.flow.async

import AsyncExtensions._
import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.event.{ChangeDependency, ChangeEvent, ChangeListener, ChangingLike, Fixed, LazyMergeMirror, LazyMirror, MergeMirror, TripleMergeMirror}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

object ChangeFuture
{
	/**
	  * Wraps a future into a change future
	  * @param future A future to wrap
	  * @param placeHolder A placeholder value to use while the future is not completed (call by name)
	  * @param exc Implicit execution context
	  * @tparam A Type of future content
	  * @return A new change future (or wrapped future result)
	  */
	def wrap[A](future: Future[A], placeHolder: => A)(implicit exc: ExecutionContext) =
		future.current match {
			case Some(v) => Fixed(v)
			case None => new ChangeFuture[A](placeHolder, future)
		}
	
	/**
	  * Wraps a future, containing None until that future is completed
	  * @param future A future
	  * @param exc Implicit execution context
	  * @tparam A Type of item this future will eventually contain
	  * @return A new change future
	  */
	def noneUntilCompleted[A](future: Future[A])(implicit exc: ExecutionContext) =
		wrap(future.map { Some(_) }, None)
}

/**
  * An asynchronously completing change from one value to another
  * @author Mikko Hilpinen
  * @since 9.12.2020, v1.9
  */
class ChangeFuture[A](placeHolder: A, val future: Future[A])(implicit exc: ExecutionContext) extends ChangingLike[A]
{
	// ATTRIBUTES	------------------------------
	
	private var dependencies = Vector[ChangeDependency[A]]()
	private var listeners = Vector[ChangeListener[A]]()
	private var _value = placeHolder
	
	
	// INITIAL CODE	------------------------------
	
	// Changes value when future completes
	future.foreach { v =>
		if (v != _value)
		{
			val event = ChangeEvent(_value, v)
			// Updates local value
			_value = v
			
			// Informs the dependencies first
			val afterEffects = dependencies.flatMap { _.beforeChangeEvent(event) }
			// Then the listeners
			listeners.foreach { _.onChangeEvent(event) }
			// Finally performs dependency after-effects
			afterEffects.foreach { _() }
			
			// Forgets about the listeners and dependencies afterwards
			listeners = Vector()
			dependencies = Vector()
		}
	}
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return Whether this future has completed
	  */
	def isCompleted = future.isCompleted
	
	
	// IMPLEMENTED	------------------------------
	
	override def isChanging = !isCompleted
	
	override def addListener(changeListener: => ChangeListener[A]) = if (isChanging) listeners :+= changeListener
	
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
	{
		val listener = changeListener
		if (isChanging)
			listeners :+= listener
		simulateChangeEventFor(listener, simulatedOldValue)
	}
	
	override def removeListener(changeListener: Any) = listeners = listeners.filterNot { _ == changeListener }
	
	override def addDependency(dependency: => ChangeDependency[A]) = if (isChanging) dependencies :+= dependency
	override def removeDependency(dependency: Any) = dependencies = dependencies.filterNot { _ == dependency }
	
	override def futureWhere(valueCondition: A => Boolean) =
		future.flatMap { v => if (valueCondition(v)) Future.successful(v) else Future.never }
	
	override def map[B](f: A => B) =
		if (isCompleted) Fixed(f(value)) else new ChangeFuture[B](f(placeHolder), future.map(f))
	
	override def lazyMap[B](f: A => B) =
		if (isCompleted) Lazy.listenable { f(value) } else new LazyMirror(this)(f)
	
	override def mergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
	{
		if (other.isChanging)
			MergeMirror.of(this, other)(f)
		else
			map { f(_, other.value) }
	}
	
	override def mergeWith[B, C, R](first: ChangingLike[B], second: ChangingLike[C])(merge: (A, B, C) => R) =
		TripleMergeMirror.of(this, first, second)(merge)
	
	override def lazyMergeWith[B, R](other: ChangingLike[B])(f: (A, B) => R) =
	{
		if (other.isChanging)
			LazyMergeMirror.of(this, other)(f)
		else
			lazyMap { f(_, other.value) }
	}
	
	override def delayedBy(threshold: Duration)(implicit exc: ExecutionContext) =
		DelayedView.of(this, threshold)
	
	override def value = _value
}
