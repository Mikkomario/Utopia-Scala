package utopia.flow.view.mutable.async

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.PointerFactory
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

import scala.annotation.unused
import scala.language.implicitConversions
import scala.util.Try

object EventfulVolatile
{
	// COMPUTED ----------------------
	
	/**
	  * @param log Implicit logging implementation for handling errors thrown by assigned listeners
	  * @return A factory for constructing Volatile pointers that fire change events
	  */
	def factory(implicit log: Logger): PointerFactory[EventfulVolatile] = new EventfulVolatileFactory()
	
	
	// IMPLICIT ----------------------
	
	// Implicitly converts this object into a factory when logging is available
	implicit def objectToFactory(@unused o: EventfulVolatile.type)
	                            (implicit log: Logger): PointerFactory[EventfulVolatile] = factory
	
	
	// OTHER    ----------------------
	
	/**
	  * @param initialValue Initial value to assign to this pointer
	  * @param log Implicit logging implementation for handling errors thrown by assigned listeners
	  * @tparam A Type of values held within this container
	  * @return A new pointer
	  */
	def apply[A](initialValue: A)(implicit log: Logger): EventfulVolatile[A] = new _EventfulVolatile[A](initialValue)
	
	
	// NESTED   ----------------------
	
	private class EventfulVolatileFactory(implicit log: Logger) extends PointerFactory[EventfulVolatile]
	{
		override def apply[A](initialValue: A): EventfulVolatile[A] = EventfulVolatile(initialValue)
	}
	
	private class _EventfulVolatile[A](initialValue: A)(implicit log: Logger) extends EventfulVolatile[A]
	{
		// ATTRIBUTES   -----------
		
		@volatile private var _value: A = initialValue
		
		override lazy val readOnly: Changing[A] = ChangingWrapper(this)
		
		
		// IMPLEMENTED  -----------
		
		override def value: A = _value
		override def destiny: Destiny = ForeverFlux
		
		override protected def assignWithoutEvents(newValue: A): Unit = _value = newValue
		
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	}
}

/**
  * Common trait for mutable containers that are safe to use in a multithreaded environment
  * and which generate change events.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
abstract class EventfulVolatile[A](implicit listenerLogger: Logger)
	extends AbstractChanging[A] with Volatile[A] with EventfulPointer[A]
{
	// ABSTRACT -----------------------
	
	/**
	  * Assigns a new value to the handled volatile pointer or variable.
	  * Change event -generation is handled outside of this method call.
	  *
	  * This method is always called in a synchronized block.
	  *
	  * @param newValue New value. Will never match the current value.
	  */
	protected def assignWithoutEvents(newValue: A): Unit
	
	
	// IMPLEMENTED  -------------------
	
	override protected def assign(newValue: A): Seq[() => Unit] = {
		val oldValue = value
		// Case: Value doesn't change => No-op
		if (newValue == oldValue)
			Empty
		// Case: Value changes => Updates the value and prepares to fire a change event
		else {
			assignWithoutEvents(newValue)
			Single(() => fireEventIfNecessary(oldValue, newValue).foreach { effect => Try { effect() }.logFailure })
		}
	}
}
