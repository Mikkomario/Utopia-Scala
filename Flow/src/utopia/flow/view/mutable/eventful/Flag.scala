package utopia.flow.view.mutable.eventful

import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper, FlagLike}

import scala.concurrent.Future

object Flag
{
	// OTHER    ------------------------
	
	/**
	  * @return A new flag
	  */
	def apply(): Flag = new _Flag()
	
	/**
	  * Wraps a flag view, adding a mutable interface to it
	  * @param view A view that provides the state of this flag
	  * @param set A function for setting this flag.
	  *            Returns whether the state was altered.
	  * @return A new flag that wraps the specified view and uses the specified function to alter state.
	  */
	def wrap(view: Changing[Boolean])(set: => Boolean): Flag = new IndirectFlag(view, () => set)
	
	
	// NESTED   ------------------------
	
	/**
	  * An item that may be set (from false to true) exactly once.
	  * Generates change events when set.
	  * @author Mikko Hilpinen
	  * @since 18.9.2022, v1.17
	  */
	private class _Flag() extends AbstractMayStopChanging[Boolean] with Flag
	{
		// ATTRIBUTES   ---------------------
		
		private var _value = false
		
		override lazy val view = new FlagView(this)
		override lazy val future = super.future
		
		//noinspection PostfixUnaryOperation
		override lazy val unary_! = super.unary_!
		
		
		// IMPLEMENTED  ---------------------
		
		override def value = _value
		override def destiny: Destiny = if (isSet) Sealed else MaySeal
		
		// Can't be set twice, so asking for nextFuture after set is futile
		override def nextFuture = if (isSet) Future.never else future
		
		override def set() = {
			if (isNotSet) {
				_value = true
				fireEvent(ChangeEvent(false, true)).foreach { _() }
				// Forgets all the listeners at this point, because no more change events will be fired
				declareChangingStopped()
				true
			}
			else
				false
		}
	}
	
	private class IndirectFlag(override protected val wrapped: Changing[Boolean], indirectSet: () => Boolean)
		extends Flag with ChangingWrapper[Boolean]
	{
		override lazy val view: FlagLike = wrapped match {
			case f: FlagLike => f
			case o => o
		}
		
		override def readOnly = view
		
		override def set(): Boolean = indirectSet()
	}
}

/**
  * A common trait for flags.
  * I.e. for boolean containers that may be set from false to true.
  */
trait Flag extends FlagLike
{
	// ABSTRACT -------------------
	
	/**
	  * @return An immutable view into this flag
	  */
	def view: FlagLike
	
	/**
	  * Sets this flag (from false to true), unless set already
	  * @return Whether the state of this flag was altered, i.e. whether this flag was not set previously.
	  */
	def set(): Boolean
	
	
	// IMPLEMENTED  --------------
	
	override def readOnly: Changing[Boolean] = view
}
