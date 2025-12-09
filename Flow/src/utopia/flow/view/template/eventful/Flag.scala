package utopia.flow.view.template.eventful

import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.Destiny
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.End
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed, LogicalMirror}
import utopia.flow.view.mutable.eventful.{LockableFlag, ResettableFlag, SettableFlag}
import utopia.flow.view.template.MaybeSet

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object Flag
{
	// COMPUTED    ------------------
	
	/**
	  * @return Access to settable flag constructors
	  */
	def settable = SettableFlag
	/**
	  * @return Access to resettable flag constructors
	  */
	def resettable = ResettableFlag
	/**
	  * @return Access to lockable flag constructors
	  */
	def lockable = LockableFlag
	
	
	// IMPLICIT ------------------
	
	// Wraps any Changing[Boolean] into a more specific FlagLike
	implicit def wrap(c: Changing[Boolean]): Flag = c match {
		case f: Flag => f
		case o =>
			o.fixedValue match {
				case Some(fixed) => if (fixed) AlwaysTrue else AlwaysFalse
				case None => new FlagWrapper(o)
			}
	}
	
	
	// OTHER    ------------------
	
	/**
	 * @param future Future for which a completion flag is constructed
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation. Used for handling failures during event-generation.
	 * @return A flag that will contain true once the specified future completes.
	 *         Will never change afterwards.
	 */
	def completionOf(future: Future[_])(implicit exc: ExecutionContext, log: Logger) = {
		// Case: Future already completed => No need to build advanced logic
		if (future.isCompleted)
			AlwaysTrue
		// Case: Future is still pending => Prepares a completion flag
		else {
			val flag = SettableFlag()
			// Once the future resolves (whether successfully or not), sets the flag
			future.onComplete { _ => flag.set() }
			flag.view
		}
	}
	
	
	// NESTED   ------------------
	
	private class FlagWrapper(override protected val wrapped: Changing[Boolean])
		extends Flag with ChangingWrapper[Boolean]
	{
		override implicit def listenerLogger: Logger = wrapped.listenerLogger
		override def toString = s"$wrapped.flag"
	}
}

/**
  * A common trait for items which resemble a boolean flag
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait Flag extends Changing[Boolean] with MaybeSet
{
	// COMPUTED	-----------------
	
	/**
	  * @return Whether this flag will always remain true
	  */
	def isAlwaysTrue = existsFixed(Identity)
	/**
	  * @return Whether this flag will always remain false
	  */
	def isAlwaysFalse = existsFixed { !_ }
	/**
	 * @return True if this flag might at some point contain true
	 */
	def maybeTrue = !isAlwaysFalse
	/**
	 * @return True if this flag might at some point contain false
	 */
	def maybeFalse = !isAlwaysTrue
	
	/**
	  * @return A reversed copy of this flag
	  */
	def unary_! : Flag = fixedValue match {
		case Some(fixed) => if (fixed) AlwaysFalse else AlwaysTrue
		case None => ReverseView
	}
	
	/**
	  * @return Future that resolves when this flag is set
	  */
	def future = findMapFuture { if (_) Some(()) else None }
	/**
	  * @return Future that resolves when this flag is set the next time
	  */
	def nextFuture = findMapNextFuture { if (_) Some(()) else None }
	
	
	// IMPLEMENTED  -------------
	
	override def isSet = value
	
	
	// OTHER	-----------------
	
	/**
	  * @param other Another flag
	  * @return A flag that contains true when both of these flags contain true
	  */
	def &&(other: Flag): Flag = LogicalMirror.and(this, other)
	/**
	  * @param other Another flag
	  * @return A flag that contains true when either one of these flags contains true
	  */
	def ||(other: Flag): Flag = LogicalMirror.or(this, other)
	
	/**
	  * @param falseState Function that returns the viewed value when this flag is not set
	  * @param trueState Function that returns the viewed value when this flag is set
	  * @tparam A Type of the viewed values
	  * @return A view that presents one of the specified values based on the state of this flag
	  */
	def lightSwitch[A](falseState: => A, trueState: => A) =
		lightMap { if (_) trueState else falseState }
	/**
	  * @param falseState Value when this flag is not set (lazily called)
	  * @param trueState Value when this flag is set (lazily called)
	  * @tparam A Type of the specified values
	  * @return A view that presents one of the specified values, based on the state of this flag
	  */
	def switch[A](falseState: => A, trueState: => A) = fixedValue match {
		case Some(fixed) => if (fixed) Fixed(trueState) else Fixed(falseState)
		case None =>
			if (value) {
				val f = Lazy(falseState)
				val t = trueState
				lightSwitch(f.value, t)
			}
			else {
				val f = falseState
				val t = Lazy(trueState)
				lightSwitch(f, t.value)
			}
	}
	
	/**
	  * Performs the specified function once this flag is set.
	  * If this flag is already set, calls the function immediately.
	  * @param f A function to call when this flag is set (will be called 0 or 1 times only)
	  * @tparam U Arbitrary function result type
	  */
	def onceSet[U](f: => U) = addListenerAndSimulateEvent(false) { e =>
		if (e.newValue) {
			f
			Detach
		}
		else
			Continue
	}
	/**
	  * Performs the specified function once this flag is reset / not set.
	  * If this flag is not set at this time, calls the function immediately.
	  * @param f A function to call once this flag is not set (will be called 0 or 1 times only)
	  * @tparam U Arbitrary function result type
	  */
	def onceNotSet[U](f: => U) = addListenerAndSimulateEvent(true) { e =>
		if (e.newValue)
			Continue
		else {
			f
			Detach
		}
	}
	/**
	  * Performs the specified function once this flag is set.
	  * If this flag is already set, will only call the specified function after this flag has been
	  * reset and then set again.
	  * If this is not a resettable flag and has been set, the specified function will never get called.
	  * @param f A function to call when this flag is set (will be called 0 or 1 times only)
	  * @tparam U Arbitrary function result type
	  */
	def whenNextSet[U](f: => U) = addListener { e =>
		if (e.newValue) {
			f
			Detach
		}
		else
			Continue
	}
	
	private object ReverseView extends Flag
	{
		// ATTRIBUTES   -----------------------
		
		private lazy val listenerCache = Cache { listener: ChangeListener[Boolean] =>
			ChangeListener[Boolean] { event => listener.onChangeEvent(event.map { !_ }) }
		}
		
		
		// COMPUTED ---------------------------
		
		private def target = Flag.this
		
		
		// IMPLEMENTED  -----------------------
		
		override implicit def listenerLogger: Logger = target.listenerLogger
		
		override def value: Boolean = !target.value
		override def destiny: Destiny = target.destiny
		
		override def readOnly: Changing[Boolean] = this
		
		override def hasListeners: Boolean = target.hasListeners
		override def numberOfListeners: Int = target.numberOfListeners
		
		override def unary_! = target
		
		override def toString = s"$target.reversed"
		
		override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[Boolean]]): Unit =
			target.addListenerOfPriority(priority) { listenerCache(lazyListener.value) }
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit =
			target.addChangingStoppedListener(listener)
		
		override def removeListener(changeListener: Any): Unit = changeListener match {
			case listener: ChangeListener[Boolean] => listenerCache.cached(listener).foreach(target.removeListener)
			case _ => ()
		}
		
		override def viewLocked[B](operation: Boolean => B): B = Flag.this.viewLocked { v => operation(!v) }
		override def lockWhile[B](operation: => B): B = Flag.this.lockWhile(operation)
	}
}
