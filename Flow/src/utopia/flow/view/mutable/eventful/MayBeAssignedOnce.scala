package utopia.flow.view.mutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.event.model.{AfterEffect, ChangeEvent, Destiny}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper, Flag}

import scala.concurrent.{Future, Promise}

object MayBeAssignedOnce
{
	// COMPUTED ----------------------------
	
	/**
	 * @tparam A Type of the theoretically assignable values
	 * @return A pointer that has already been locked and may never be set
	 */
	def locked[A]: MayBeAssignedOnce[A] = new AlreadyLocked
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param log Implicit logging implementation used for handling errors during change-event processing
	 * @tparam A Type of the assignable value
	 * @return A pointer that may be either assigned a value (once),
	 *         or locked without ever receiving a value.
	 */
	def apply[A]()(implicit log: Logger): MayBeAssignedOnce[A] = new _MayBeAssignedOnce[A]
	/**
	 * @param initialValue A pre-assigned value, if applicable
	 * @param log Implicit logging implementation used for handling errors during change-event processing
	 * @tparam A Type of the assignable value
	 * @return A pointer that may be either assigned a value (once),
	 *         or locked without ever receiving a value.
	 *
	 *         If 'initialValue' was specified, this pointer is already set and won't change anymore.
	 */
	def apply[A](initialValue: Option[A])(implicit log: Logger): MayBeAssignedOnce[A] = initialValue match {
		case Some(value) => set(value)
		case None => apply[A]()
	}
	
	/**
	 * @param value A pre-assigned value
	 * @tparam A Type of the specified value
	 * @return A pointer that has already been given the specified value, and won't change anymore
	 */
	def set[A](value: A): MayBeAssignedOnce[A] = new AlreadyAssigned[A](value)
	
	
	// NESTED   ----------------------------
	
	private class _MayBeAssignedOnce[A](implicit log: Logger)
		extends AbstractMayStopChanging[Option[A]] with MayBeAssignedOnce[A]
	{
		// ATTRIBUTES   --------------------
		
		private var _value: Option[A] = None
		private var _locked = false
		
		override lazy val readOnly: Changing[Option[A]] = ChangingWrapper(this)
		
		override lazy val setFlag: Flag = {
			if (_value.isDefined)
				AlwaysTrue
			else if (_locked)
				AlwaysFalse
			else {
				val flag = LockableFlag()
				addListenerAndSimulateEvent(None) { e =>
					if (e.newValue.isDefined) {
						flag.set()
						Detach
					}
					else
						Continue
				}
				addChangingStoppedListener { flag.lock() }
				
				flag
			}
		}
		
		override lazy val finalStateFuture: Future[Option[A]] = {
			if (mayChange) {
				val promise = Promise[Option[A]]()
				forFinalValue(promise.trySuccess)
				promise.future
			}
			else
				Future.successful(value)
		}
		override lazy val future: Future[A] = {
			if (mayChange) {
				val promise = Promise[A]()
				forFinalValue {
					case Some(value) => promise.trySuccess(value)
					case None =>
						promise.tryFailure(new IllegalStateException(
							"This pointer was locked without ever receiving a value"))
				}
				promise.future
			}
			else
				value match {
					case Some(value) => Future.successful(value)
					case None =>
						Future.failed(new IllegalStateException(
							"This pointer was locked without ever receiving a value"))
				}
		}
		
		
		// IMPLEMENTED  --------------------
		
		override def value: Option[A] = _value
		override def locked: Boolean = _locked
		
		override protected def _set(value: A): Unit = {
			_value = Some(value)
			fireEvent(ChangeEvent(None, Some(value)))
		}
		override protected def _setAndQueueEvent(value: A): IterableOnce[AfterEffect] = {
			_value = Some(value)
			fireEventEffects(ChangeEvent(None, Some(value)))
		}
		
		override def lock(): Unit = {
			if (!_locked) {
				_locked = true
				// Only fires a changing stopped -event if this pointer had not yet been set,
				// because if already set, a changing stopped -event has already been fired.
				if (isNotSet)
					declareChangingStopped()
			}
		}
	}
	
	private class AlreadyAssigned[A](_value: A) extends Fixed[Option[A]] with MayBeAssignedOnce[A]
	{
		// ATTRIBUTES   ----------------------
		
		override val value: Option[A] = Some(_value)
		override val destiny: Destiny = Sealed
		
		private var _locked = false
		
		override lazy val readOnly: Changing[Option[A]] = if (_locked) this else ChangingWrapper(this)
		
		
		// IMPLEMENTED  ----------------------
		
		override def locked: Boolean = _locked
		
		override def setFlag: Flag = AlwaysTrue
		
		override def finalStateFuture: Future[Option[A]] = Future.successful(value)
		override def future: Future[A] = Future.successful(_value)
		
		override protected def _set(value: A): Unit = ()
		override protected def _setAndQueueEvent(value: A): IterableOnce[AfterEffect] = Empty
		
		override def lock(): Unit = _locked = true
		
		override protected def declareChangingStopped(): Unit = ()
		
		override def onceSet[U](f: A => U): Unit = f(_value)
	}
	
	private class AlreadyLocked[A] extends Fixed[Option[A]] with MayBeAssignedOnce[A]
	{
		// ATTRIBUTES   --------------------
		
		override val locked: Boolean = true
		override val destiny: Destiny = Sealed
		
		
		// IMPLEMENTED  --------------------
		
		override def value: Option[A] = None
		
		override def setFlag: Flag = AlwaysFalse
		
		override def finalStateFuture: Future[Option[A]] = Future.successful(None)
		override def future: Future[A] =
			Future.failed(new UnsupportedOperationException("This pointer may never be set"))
		
		override protected def _set(value: A): Unit = ()
		override protected def _setAndQueueEvent(value: A): IterableOnce[AfterEffect] = Empty
		
		override def lock(): Unit = ()
		
		override protected def declareChangingStopped(): Unit = ()
	}
}

/**
 * A lockable implementation of [[AssignableOnce]].
 * This pointer may receive a new value once, or it may be locked and never receive a value.
 * @author Mikko Hilpinen
 * @since 06.10.2025, v2.7
 */
trait MayBeAssignedOnce[A] extends AssignableOnce[A] with Lockable[Option[A]]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return A future that resolves once this pointer receives a value, or once this pointer gets locked.
	 *         Will contain the assigned value, if one was acquired.
	 */
	def finalStateFuture: Future[Option[A]]
	
	
	// IMPLEMENTED  ----------------------
	
	override def destiny: Destiny = if (locked || isSet) Sealed else MaySeal
	
	override def value_=(newValue: Option[A]): Unit = failIfLocked { super.value_=(newValue) }
	override def set(value: A): Unit = failIfLocked { super.set(value) }
	override def setAndQueueEvent(newValue: Option[A]): IterableOnce[AfterEffect] =
		failIfLocked { super.setAndQueueEvent(newValue) }
	override def trySet(value: => A): Boolean = if (locked) false else super.trySet(value)
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param f A function to call once/if this pointer is locked before a value is assigned.
	 *          If a value is assigned instead, this function will never be called.
	 * @tparam U An arbitrary function result type.
	 */
	def onceLockedWithoutValue[U](f: => U) = forFinalValue { v => if (v.isEmpty) f }
}
