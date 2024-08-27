package utopia.flow.view.mutable.eventful

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.{Pointer, PointerFactory}
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

import scala.annotation.unused
import scala.language.implicitConversions
import scala.util.Try

object EventfulPointer
{
	// COMPUTED -----------------------
	
	/**
	  * @param log Implicit logging implementation for handling failures thrown by event listeners
	  * @return Factory for constructing eventful pointers
	  */
	def factory(implicit log: Logger): PointerFactory[EventfulPointer] = new EventfulPointerFactory()
	
	
	// IMPLICIT -----------------------
	
	implicit def objectToFactory(@unused o: EventfulPointer.type)(implicit log: Logger): PointerFactory[EventfulPointer] =
		factory
	
	
	// OTHER    -----------------------
	
	/**
	  * Creates a new mutable pointer
	  * @param initialValue Initial value to assign to this pointer
	  * @param log Implicit logging implementation for handling failures thrown by event listeners
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	def apply[A](initialValue: A)(implicit log: Logger): EventfulPointer[A] = new _EventfulPointer[A](initialValue)
	
	
	// NESTED   -----------------------
	
	private class EventfulPointerFactory(implicit log: Logger) extends PointerFactory[EventfulPointer]
	{
		override def apply[A](initialValue: A): EventfulPointer[A] = EventfulPointer(initialValue)
	}
	
	private class _EventfulPointer[A](initialValue: A)(implicit log: Logger)
		extends AbstractChanging[A] with EventfulPointer[A]
	{
		// ATTRIBUTES	----------------
		
		private var _value = initialValue
		
		// Caches the read-only view
		override lazy val readOnly = ChangingWrapper(this)
		
		
		// COMPUTED --------------------
		
		/**
		  * A read-only view into this pointer
		  */
		@deprecated("Please switch to using .readOnly instead", "v2.3")
		def view: Changing[A] = readOnly
		
		
		// IMPLEMENTED	----------------
		
		override def destiny: Destiny = ForeverFlux
		
		/**
		  * @return The current value in this mutable
		  */
		override def value = _value
		/**
		  * @param newValue The new value in this mutable
		  */
		def value_=(newValue: A) = {
			val oldValue = _value
			_value = newValue
			fireEventIfNecessary(oldValue, newValue).foreach { effect => Try { effect() }.logFailure }
		}
		
		override def toString = s"EventfulPointer(${_value})"
		
		// Can never stop changing, so listener assignment is not needed either
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	}
}

/**
  * Common trait for changing items (i.e. items that fire change events) that provide a mutable interface
  * @tparam A Type of the values in this pointer
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1
  */
trait EventfulPointer[A] extends Pointer[A] with Changing[A]