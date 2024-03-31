package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.mouse.MouseOverEvent.{MouseOverEventFilter, MouseOverFilteringFactory}
import utopia.genesis.handling.template.Handleable
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.template.vector.DoubleVector

import scala.annotation.unused
import scala.language.implicitConversions

object MouseOverListener
{
	// ATTRIBUTES   --------------------
	
	/**
	  * A listener factory that doesn't apply any listening condition or event filter
	  */
	val unconditional = MouseOverListenerFactory()
	
	
	// IMPLICIT ------------------------
	
	implicit def objectToFactory(@unused o: MouseOverListener.type): MouseOverListenerFactory = unconditional
	
	
	// NESTED   ------------------------
	
	case class MouseOverListenerFactory(condition: FlagLike = AlwaysTrue, filter: MouseOverEventFilter = AcceptAll)
		extends ListenerFactory[MouseOverEvent, MouseOverListenerFactory]
			with MouseOverFilteringFactory[MouseOverListenerFactory]
	{
		// IMPLEMENTED  ---------------------
		
		override def usingFilter(filter: Filter[MouseOverEvent]): MouseOverListenerFactory = copy(filter = filter)
		override def usingCondition(condition: FlagLike): MouseOverListenerFactory = copy(condition = condition)
		
		override protected def withFilter(filter: Filter[MouseOverEvent]): MouseOverListenerFactory = filtering(filter)
		
		
		// OTHER    ------------------------
		
		/**
		  * @param contains A function for testing whether a point is over the targeted area
		  * @param f A function called on mouse over events,
		  *          but only if the listening condition and event filter of this factory allow it
		  * @return A new mouse over listener
		  */
		def apply(contains: DoubleVector => Boolean)(f: MouseOverEvent => ConsumeChoice): MouseOverListener =
			new _MouseOverListener(condition, filter, contains, f)
	}
	
	private class _MouseOverListener(override val handleCondition: FlagLike,
	                                 override val mouseOverEventFilter: MouseOverEventFilter,
	                                 containment: DoubleVector => Boolean, f: MouseOverEvent => ConsumeChoice)
		extends MouseOverListener
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
trait MouseOverListener extends Handleable with Area2D
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
