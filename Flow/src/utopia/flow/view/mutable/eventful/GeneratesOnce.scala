package utopia.flow.view.mutable.eventful

import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed, OptimizedMirror}
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper, Flag}

object GeneratesOnce
{
	// OTHER    --------------------------------
	
	/**
	 * Creates a new pointer that initializes when its value is called.
	 *
	 * The pointer will fire a change event when it initializes.
	 * Note, however, that the change event will contain the same value as both the old and the new value.
	 *
	 * @param value A function that will generate the value, once it is needed
	 * @param log Implicit logging implementation used in change-event handling
	 * @tparam A Type of the generated value
	 * @return A new pointer that will initialize once its value is called
	 */
	def apply[A](value: => A)(implicit log: Logger): GeneratesOnce[A] = new _GeneratesOnce[A](None, value)
	/**
	 * Creates a new pointer that initializes when its value is called.
	 * The pointer will fire a change event when it initializes.
	 *
	 * @param oldValue A function that will generate the old value, used when generating change events.
	 *                 May be called 0-n times.
	 * @param value A function that will generate the value, once it is needed
	 * @param log Implicit logging implementation used in change-event handling
	 * @tparam A Type of the generated value
	 * @return A new pointer that will initialize once its value is called
	 */
	def simulatingOldValue[A](oldValue: => A)(value: => A)(implicit log: Logger): GeneratesOnce[A] =
		new _GeneratesOnce[A](Some({ () => oldValue }), value)
	
	/**
	 * Creates a new pre-initialized pointer
	 * @param value Value to wrap
	 * @param log Implicit logging implementation.
	 *            Used for change-event -handling within mapped versions of this pointer.
	 * @tparam A Type of the wrapped value
	 * @return A new pre-initialized pointer that will no longer change
	 */
	def initialized[A](value: A)(implicit log: Logger): GeneratesOnce[A] = new AlreadyGenerated[A](value)
	
	
	// NESTED   --------------------------------
	
	private class _GeneratesOnce[A](simulateOldValue: Option[() => A], generate: => A)(implicit log: Logger)
		extends AbstractMayStopChanging[A] with GeneratesOnce[A]
	{
		// ATTRIBUTES   -------------------------
		
		private var _value: Option[A] = None
		
		private val lazyCurrentPointer = Lazy { AssignableOnce(_value) }
		override lazy val initializedFlag: Flag = lazyCurrentPointer.value.nonEmptyFlag
		override lazy val emptyFlag: Flag = !initializedFlag
		
		
		// IMPLEMENTED  -------------------------
		
		override def current: Option[A] = _value
		override def value: A = _value.getOrElse {
			// Generates and remembers the new value
			val value = generate
			_value = Some(value)
			
			// Updates the current value -view, if applicable
			lazyCurrentPointer.current.foreach { _.set(value) }
			
			// Fires the change event (if necessary), as well as the changing stopped -event
			if (hasListeners) {
				val oldValue = simulateOldValue match {
					case Some(getOldValue) => getOldValue()
					case None => value
				}
				fireEvent(ChangeEvent(oldValue, value))
			}
			declareChangingStopped()
			
			value
		}
		
		override def readOnly: Changing[A] = this
		
		/**
		 * @return A pointer that contains None until this pointer is initialized.
		 *         Once this pointer is initialized, contains the same value.
		 */
		override def currentPointer = lazyCurrentPointer.value.readOnly
		
		override protected def _map[B](f: A => B): GeneratesOnce[B] = current match {
			case Some(value) => new AlreadyGenerated[B](f(value))
			case None => new MappedGeneratesOnce[A, B](this, f)
		}
	}
	
	private class AlreadyGenerated[A](override val value: A)(implicit log: Logger)
		extends Fixed[A] with GeneratesOnce[A]
	{
		// ATTRIBUTES   -----------------------
		
		override val current: Option[A] = Some(value)
		override val initializedFlag: Flag = AlwaysTrue
		override val emptyFlag: Flag = AlwaysFalse
		override val destiny: Destiny = Sealed
		
		override lazy val currentPointer: Changing[Option[A]] = Fixed(Some(value))
		
		
		// IMPLEMENTED  -----------------------
		
		override protected def _map[B](f: A => B): GeneratesOnce[B] = new AlreadyGenerated[B](f(value))
	}
	
	private class MappedGeneratesOnce[A, B](source: GeneratesOnce[A], f: A => B)
		extends GeneratesOnce[B] with ChangingWrapper[B]
	{
		// ATTRIBUTES   ---------------------
		
		protected override val wrapped = OptimizedMirror(source)(f)
		override lazy val currentPointer: Changing[Option[B]] = source.currentPointer.map { _.map(f) }
		
		
		// IMPLEMENTED  ---------------------
		
		override implicit def listenerLogger: Logger = source.listenerLogger
		
		override def value: B = {
			source.value // Makes sure the source is up-to-date
			super.value
		}
		override def destiny: Destiny = source.destiny
		
		override def current: Option[B] = if (source.isInitialized) Some(wrapped.value) else None
		
		override def isInitialized: Boolean = source.isInitialized
		override def initializedFlag: Flag = source.initializedFlag
		override def emptyFlag: Flag = source.emptyFlag
		
		override def map[C](f: B => C) = _map(f)
		override protected def _map[C](f: B => C): GeneratesOnce[C] = current match {
			case Some(value) => new AlreadyGenerated[C](f(value))
			case None => new MappedGeneratesOnce[B, C](this, f)
		}
	}
}

/**
 * A combination of [[Lazy]] and [[Changing]], which initializes its value once, firing a change event in the process.
 * @author Mikko Hilpinen
 * @since 06.10.2025, v2.7
 */
trait GeneratesOnce[+A] extends GeneratesLike[A, GeneratesOnce]
{
	// ABSTRACT -----------------------------
	
	/**
	 * @return A flag that contains true after this pointer has been initialized
	 */
	def initializedFlag: Flag
	
	
	// IMPLEMENTED  -------------------------
	
	override def nonEmptyFlag: Flag = initializedFlag
	
	override def destiny: Destiny = if (isInitialized) Sealed else MaySeal
}
