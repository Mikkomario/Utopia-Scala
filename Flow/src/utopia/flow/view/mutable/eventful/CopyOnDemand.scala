package utopia.flow.view.mutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.{ForeverFlux, MaySeal, Sealed}
import utopia.flow.operator.enumeration.End
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.View
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

import scala.util.Try

object CopyOnDemand
{
	// OTHER    -------------------------------
	
	/**
	  * @param f A function for producing the viewed value
	  * @param log Implicit logging implementation for handling failures thrown by event listeners
	  * @tparam A Type of viewed values
	  * @return A pointer that updates its cached value on demand (i.e. whenever update() is called)
	  */
	def apply[A](f: => A)(implicit log: Logger): CopyOnDemand[A] = new ViewOnDemand[A](View(f))
	
	/**
	  * Creates a new pointer for viewing the specified source view
	  * @param source A value view
	  * @param log Implicit logging implementation for handling failures thrown by event listeners
	  * @tparam A Type of the viewed values
	  * @return A pointer that will mirror the specified view's value, whenever update() is called
	  */
	// Checks what kind of source is in question
	def apply[A](source: View[A])(implicit log: Logger): CopyOnDemand[A] = source match {
		// Case: Changing item
		case c: Changing[A] => apply(c)
		// Case: General view
		case v => new ViewOnDemand[A](v)
	}
	/**
	  * Creates a new pointer for viewing another changing item
	  * @param source An item whose value is copied upon calls to update()
	  * @tparam A Type of the viewed values
	  * @return A pointer that will mirror the specified pointer's value, whenever update() is called
	  */
	def apply[A](source: Changing[A]): CopyOnDemand[A] = source.fixedValue match {
		// Case: Fixed item
		case Some(fixedValue) => fixed(fixedValue)
		// Case: Changing item
		case None =>
			// Case: The source may stop changing at some point => Uses the more advanced implementation
			if (source.destiny.isPossibleToSeal)
				new _CopyOnDemand[A](source)
			// Case: The source is unlikely to ever stop changing => Uses the more simple, view-based approach
			else
				new ViewOnDemand[A](source)(source.listenerLogger)
	}
	
	/**
	  * @param value A fixed value
	  * @tparam A Type of the specified value
	  * @return A pointer that implements the CopyOnDemand interface,
	  *         but in actuality always displays the specified value
	  */
	def fixed[A](value: A): CopyOnDemand[A] = new FixedOnDemand[A](value)
	
	
	// NESTED   -------------------------------
	
	// Implementation for Changing items
	private class _CopyOnDemand[A](source: Changing[A])
		extends AbstractChanging[A]()(source.listenerLogger) with CopyOnDemand[A]
	{
		// ATTRIBUTES   -----------------------
		
		// Stores the last look-up value
		private var cachedValue = source.value
		private var changingStoppedListeners: Seq[ChangingStoppedListener] = Empty
		
		override lazy val readOnly: Changing[A] = ChangingWrapper(this)
		
		
		// INITIAL CODE -----------------------
		
		// Tracks possible changing stopped -events in order to relay them to this pointer's listeners
		if (source.destiny == MaySeal)
			source.onceChangingStops { changingStoppedListeners.foreach { _.onChangingStopped() } }
		
		
		// IMPLEMENTED  -----------------------
		
		override def value: A = cachedValue
		override def destiny: Destiny = source.destiny
		
		override def update() = {
			val oldValue = cachedValue
			cachedValue = source.value
			fireEventIfNecessary(oldValue).foreach { effect => Try { effect() }.log }
		}
		
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit =
			changingStoppedListeners :+= listener
	}
	
	// Implementation for general Views
	private class ViewOnDemand[A](source: View[A])(implicit log: Logger)
		extends AbstractChanging[A] with CopyOnDemand[A]
	{
		// ATTRIBUTES   --------------------
		
		private var cachedValue = source.value
		
		override lazy val readOnly: Changing[A] = ChangingWrapper(this)
		
		
		// IMPLEMENTED  --------------------
		
		override def value: A = cachedValue
		override def destiny: Destiny = ForeverFlux
		
		override def update(): Unit = {
			val oldValue = cachedValue
			cachedValue = source.value
			fireEventIfNecessary(oldValue).foreach { _() }
		}
		
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	}
	
	// Implementation for fixed values
	private class FixedOnDemand[+A](override val value: A) extends CopyOnDemand[A]
	{
		override implicit def listenerLogger: Logger = SysErrLogger
		
		override def destiny: Destiny = Sealed
		override def readOnly: Changing[A] = this
		
		override def hasListeners: Boolean = false
		override def numberOfListeners: Int = 0
		
		override def update(): Unit = ()
		
		override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit = ()
		override def removeListener(changeListener: Any): Unit = ()
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	}
}

/**
  * An eventful pointer class that copies the value of another view, but only when commanded to do so.
  * @author Mikko Hilpinen
  * @since 11/02/2024, v2.4
  */
trait CopyOnDemand[+A] extends Changing[A]
{
	// ABSTRACT    --------------------------
	
	/**
	  * Updates the value in this pointer to match that of the tracked pointer.
	  */
	def update(): Unit
}
