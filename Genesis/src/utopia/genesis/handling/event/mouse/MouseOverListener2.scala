package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter, RejectAll}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.mouse.MouseEvent2.MouseFilteringFactory
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.template.Handleable2
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.template.vector.DoubleVector

import scala.annotation.unused
import scala.concurrent.duration.Duration
import scala.language.implicitConversions

object MouseOverListener2
{
	// TYPES    ------------------------
	
	/**
	  * Type of filters applied to mouse over events
	  */
	type MouseOverEventFilter = Filter[MouseOverEvent]
	
	
	// ATTRIBUTES   --------------------
	
	/**
	  * A listener factory that doesn't apply any listening condition or event filter
	  */
	val unconditional = MouseOverListenerFactory()
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return An access point to mouse over event filters
	  */
	def filter = MouseOverEventFilter
	
	
	// IMPLICIT ------------------------
	
	implicit def objectToFactory(@unused o: MouseOverListener2.type): MouseOverListenerFactory = unconditional
	
	
	// NESTED   ------------------------
	
	trait MouseOverFilteringFactory[+A] extends MouseFilteringFactory[MouseOverEvent, A]
	{
		/**
		  * @return An item that only accepts unconsumed events
		  */
		def unconsumed = withFilter { _.unconsumed }
		
		/**
		  * @param minimumDuration Minimum hover duration
		  * @return An item that only accepts events once the hover extends over the specified duration
		  */
		def longerThan(minimumDuration: Duration) = {
			if (minimumDuration <= Duration.Zero)
				withFilter(AcceptAll)
			else
				minimumDuration.finite match {
					case Some(duration) => withFilter { _.totalDuration >= duration }
					case None => withFilter(RejectAll)
				}
		}
	}
	
	object MouseOverEventFilter extends MouseOverFilteringFactory[MouseOverEventFilter]
	{
		// IMPLEMENTED  -----------------------
		
		override protected def withFilter(filter: Filter[MouseOverEvent]): MouseOverEventFilter = filter
		
		
		// OTHER    ---------------------------
		
		/**
		  * @param f A filtering function
		  * @return A filter that uses the specified function
		  */
		def apply(f: MouseOverEvent => Boolean) = Filter(f)
	}
	
	case class MouseOverListenerFactory(condition: FlagLike = AlwaysTrue, filter: MouseOverEventFilter = AcceptAll)
		extends ListenerFactory[MouseOverEvent, MouseOverListenerFactory]
			with MouseOverFilteringFactory[MouseOverListenerFactory]
	{
		// IMPLEMENTED  ---------------------
		
		override def usingFilter(filter: Filter[MouseOverEvent]): MouseOverListenerFactory = copy(filter = filter)
		override def usingCondition(condition: Changing[Boolean]): MouseOverListenerFactory = copy(condition = condition)
		
		override protected def withFilter(filter: Filter[MouseOverEvent]): MouseOverListenerFactory = filtering(filter)
		
		
		// OTHER    ------------------------
		
		/**
		  * @param contains A function for testing whether a point is over the targeted area
		  * @param f A function called on mouse over events,
		  *          but only if the listening condition and event filter of this factory allow it
		  * @return A new mouse over listener
		  */
		def apply(contains: DoubleVector => Boolean)(f: MouseOverEvent => ConsumeChoice): MouseOverListener2 =
			new _MouseOverListener(condition, filter, contains, f)
	}
	
	private class _MouseOverListener(override val handleCondition: FlagLike,
	                                 override val mouseOverEventFilter: MouseOverEventFilter,
	                                 containment: DoubleVector => Boolean, f: MouseOverEvent => ConsumeChoice)
		extends MouseOverListener2
	{
		override def contains(point: DoubleVector): Boolean = containment(point)
		
		override def onMouseOver(event: MouseOverEvent): ConsumeChoice = f(event)
	}
}

/**
  * Common trait for classes which are interested in receiving events while the mouse cursor hovers over them
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
trait MouseOverListener2 extends Handleable2 with Area2D
{
	/**
	  * @return A filter applied to incoming mouse over events.
	  *         Only events that are accepted by this filter may be delivered to this listener.
	  */
	def mouseOverEventFilter: Filter[MouseOverEvent]
	
	/**
	  * Called while the mouse hovers over this object.
	  * Only events accepted by this item's filter should trigger this function.
	  * @param event A mouse hover event
	  */
	def onMouseOver(event: MouseOverEvent): ConsumeChoice
}
