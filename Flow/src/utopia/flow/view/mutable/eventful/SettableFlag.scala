package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.Destiny.{MaySeal, Sealed}
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.operator.enumeration.End
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.mutable.Settable
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper, Flag}

import scala.concurrent.Future
import scala.util.Try

object SettableFlag
{
	// COMPUTED   ----------------------
	
	/**
	  * @return A flag that has been already set. Will not be modified and will not generate any change events.
	  */
	def alreadySet: SettableFlag = AlreadySetFlag
	
	
	// OTHER    ------------------------
	
	/**
	  * @return A new flag
	  */
	def apply()(implicit log: Logger): SettableFlag = new _SettableFlag()
	/**
	  * @param initialState Initial state to assign to this flag (i.e. whether already set)
	  * @param log Implicit logging implementation
	  * @return A new flag with the specified state
	  */
	def apply(initialState: Boolean)(implicit log: Logger): SettableFlag = if (initialState) AlreadySetFlag else apply()
	
	/**
	  * Wraps a flag view, adding a mutable interface to it
	  * @param view A view that provides the state of this flag
	  * @param set A function for setting this flag.
	  *            Returns whether the state was altered.
	  * @return A new flag that wraps the specified view and uses the specified function to alter state.
	  */
	def wrap(view: Changing[Boolean])(set: => Boolean): SettableFlag = new IndirectSettableFlag(view, () => set)
	
	
	// NESTED   ------------------------
	
	private object AlreadySetFlag extends SettableFlag
	{
		override implicit def listenerLogger: Logger = SysErrLogger
		
		override def view: Flag = this
		
		override def value: Boolean = true
		override def destiny: Destiny = Sealed
		
		override def hasListeners: Boolean = false
		override def numberOfListeners: Int = 0
		
		override def removeListener(changeListener: Any): Unit = ()
		
		override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[Boolean]]): Unit = ()
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
		
		override def set(): Boolean = false
	}
	
	/**
	  * An item that may be set (from false to true) exactly once.
	  * Generates change events when set.
	  * @author Mikko Hilpinen
	  * @since 18.9.2022, v1.17
	  */
	private class _SettableFlag(implicit log: Logger) extends AbstractMayStopChanging[Boolean] with SettableFlag
	{
		// ATTRIBUTES   ---------------------
		
		private var _value = false
		
		override lazy val view = new FlagView(this)
		override lazy val future = super.future
		
		
		// IMPLEMENTED  ---------------------
		
		override def value = _value
		override def destiny: Destiny = if (isSet) Sealed else MaySeal
		
		// Can't be set twice, so asking for nextFuture after set is futile
		override def nextFuture = if (isSet) Future.never else future
		
		override def set() = {
			if (isNotSet) {
				_value = true
				fireEvent(ChangeEvent(false, true)).foreach { effect => Try { effect() }.log }
				// Forgets all the listeners at this point, because no more change events will be fired
				declareChangingStopped()
				true
			}
			else
				false
		}
	}
	
	private class IndirectSettableFlag(override protected val wrapped: Changing[Boolean], indirectSet: () => Boolean)
		extends SettableFlag with ChangingWrapper[Boolean]
	{
		override lazy val view: Flag = wrapped match {
			case f: Flag => f
			case o => o
		}
		
		override implicit def listenerLogger: Logger = wrapped.listenerLogger
		override def readOnly = view
		
		override def set(): Boolean = indirectSet()
	}
}

/**
  * Common trait for eventful boolean containers that may be set (to contain true)
  * @author Mikko Hilpinen
  * @since ???, < v2.5
  */
trait SettableFlag extends Flag with Settable
{
	// ABSTRACT -------------------
	
	/**
	  * @return An immutable view into this flag
	  */
	def view: Flag
	
	
	// IMPLEMENTED  --------------
	
	override def readOnly: Changing[Boolean] = view
}
