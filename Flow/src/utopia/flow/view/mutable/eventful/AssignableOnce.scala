package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.End
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.MaybeAssignable
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper, MayStopChanging}

import scala.concurrent.Future
import scala.util.Try

object AssignableOnce
{
	// OTHER    ----------------------------
	
	/**
	  * @tparam A Type of set item
	  * @return A new pointer that may only be set once
	  */
	def apply[A]()(implicit log: Logger): AssignableOnce[A] = new _AssignableOnce[A]()
	/**
	  * @param initialValue Predefined value, which may be empty.
	  *                     Note: If defined, this pointer can't be modified further.
	  * @param log Implicit logging implementation used during change event -handling
	  * @tparam A Type of the held value, once defined
	  * @return A new pointer that may only be set once
	  */
	def apply[A](initialValue: Option[A])(implicit log: Logger): AssignableOnce[A] = initialValue match {
		case Some(v) => set(v)
		case None => apply()
	}
	
	/**
	  * @param value A preset value
	  * @tparam A Type of the specified value
	  * @return A pointer that has been set and can't be modified anymore
	  */
	def set[A](value: A): AssignableOnce[A] = new SetOnce[A](value)
	
	
	// NESTED   ------------------------------
	
	private class _AssignableOnce[A](implicit log: Logger)
		extends AbstractMayStopChanging[Option[A]] with AssignableOnce[A]
	{
		// ATTRIBUTES   -------------------------
		
		private var _value: Option[A] = None
		
		override lazy val readOnly: Changing[Option[A]] = if (_value.isDefined) this else ChangingWrapper(this)
		
		override lazy val future = findMapFuture(Identity)
		
		
		// IMPLEMENTED  -------------------------
		
		override def value = _value
		
		override protected def _set(value: A): Unit = {
			_value = Some(value)
			fireEventIfNecessary(None, _value).foreach { effect => Try { effect() }.log }
		}
	}
	
	/**
	  * A fixed (i.e. already set) variant of the SettableOnce trait
	  * @param v A predefined value
	  * @tparam A Type of held value
	  */
	private class SetOnce[A](v: A) extends AssignableOnce[A]
	{
		// ATTRIBUTES   ------------------------
		
		override implicit val listenerLogger: Logger = SysErrLogger
		override val hasListeners: Boolean = false
		override val numberOfListeners: Int = 0
		
		override lazy val value: Option[A] = Some(v)
		override val isSet = true
		override val destiny = Sealed
		override lazy val future: Future[A] = Future.successful(v)
		
		
		// IMPLEMENTED  ------------------------
		
		override def get = v
		
		override def readOnly: Changing[Option[A]] = this
		
		override protected def _set(value: A): Unit = ()
		override protected def declareChangingStopped(): Unit = ()
		
		override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[Option[A]]]): Unit = ()
		override def removeListener(changeListener: Any): Unit = ()
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
		
		override def onceSet[U](f: A => U): Unit = f(v)
	}
}

/**
  * A container that works like a Promise, in the sense that it can only be set once.
  * Generates ChangeEvents when set.
  * @author Mikko Hilpinen
  * @since 16.11.2022, v2.0
  */
trait AssignableOnce[A] extends MayStopChanging[Option[A]] with EventfulPointer[Option[A]] with MaybeAssignable[A]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Future that resolves once this pointer is set / completed
	  */
	def future: Future[A]
	
	/**
	  * Changes the value of this pointer and generates change events.
	  * This function is only called once, while [[value]] remains None.
	  * @param value New value to assign to this pointer
	  */
	protected def _set(value: A): Unit
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Whether this pointer has been set and is now immutable
	  */
	def isCompleted = isSet
	/**
	  * @return Whether the value of this pointer has yet to be defined
	  */
	def isEmpty = !isCompleted
	
	/**
	  * @return The value set to this pointer
	  * @throws IllegalStateException If this pointer hasn't been set
	  */
	@throws[IllegalStateException]("No value has been set yet")
	def get = value.getOrElse { throw new IllegalStateException("Called get before the value was set") }
	
	
	// IMPLEMENTED  -------------------------
	
	override def isSet: Boolean = value.isDefined
	override def destiny: Destiny = if (isSet) Sealed else MaySeal
	
	@throws[IllegalStateException]("If this pointer has already been set")
	override def value_=(newValue: Option[A]) = newValue match {
		case Some(newValue) => set(newValue)
		case None =>
			if (isSet)
				throw new IllegalStateException("SettableOnce cannot be reset")
	}
	
	/**
	  * Specifies the value in this pointer
	  * @param value Value for this pointer to hold
	  * @throws IllegalStateException If this pointer has already been set
	  */
	@throws[IllegalStateException]("If this pointer has already been set")
	override def set(value: A) = {
		if (this.value.exists { _ != value })
			throw new IllegalStateException("SettableOnce.value may only be defined once")
		else {
			_set(value)
			declareChangingStopped()
		}
	}
	/**
	  * Specifies the value in this pointer, unless specified already
	  * @param value Value for this pointer to hold (call-by-name, only called if this pointer is empty)
	  * @return Whether this pointer was set.
	  *         False if this pointer had already been set.
	  */
	override def trySet(value: => A) = {
		if (isCompleted)
			false
		else {
			_set(value)
			declareChangingStopped()
			true
		}
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Calls the specified function once this pointer has been set.
	  * If already set, calls the function immediately.
	  * @param f A function to call once this pointer has been set.
	  * @tparam U Arbitrary function result type.
	  */
	def onceSet[U](f: A => U) = addListenerAndSimulateEvent(None) { e =>
		e.newValue match {
			// Case: Set event => Calls the function and ends listening
			case Some(value) =>
				f(value)
				Detach
				
			// Case: Reset event (not applicable)
			case None => Continue
		}
	}
}
