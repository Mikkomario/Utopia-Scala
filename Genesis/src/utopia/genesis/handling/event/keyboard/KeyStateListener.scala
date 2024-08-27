package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.SysErrLogger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.keyboard.KeyStateEvent.{KeyStateEventFilter, KeyStateFilteringFactory}
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused
import scala.language.implicitConversions

object KeyStateListener
{
    // ATTRIBUTES   ------------------
    
    /**
      * A factory used for constructing unconditional key-state-event listeners
      */
    val unconditional = KeyStateListenerFactory()
    
    
    // IMPLICIT ----------------------
    
    implicit def objectToFactory(@unused o: KeyStateListener.type): KeyStateListenerFactory = unconditional
    
    /**
      * @param f A function that accepts a key-state event
      * @return A key-state listener that wraps the specified function
      */
    implicit def apply(f: KeyStateEvent => Unit): KeyStateListener = unconditional(f)
    
    
    // NESTED   -----------------------------
    
    case class KeyStateListenerFactory(condition: Flag = AlwaysTrue,
                                       filter: KeyStateEventFilter = AcceptAll)
        extends ListenerFactory[KeyStateEvent, KeyStateListenerFactory]
            with KeyStateFilteringFactory[KeyStateListenerFactory]
    {
        // IMPLEMENTED  ---------------------
        
        override def usingFilter(filter: Filter[KeyStateEvent]): KeyStateListenerFactory = copy(filter = filter)
        override def usingCondition(condition: Flag): KeyStateListenerFactory = copy(condition = condition)
	    
	    override protected def withFilter(filter: Filter[KeyStateEvent]): KeyStateListenerFactory = filtering(filter)
	    
	    
	    // OTHER    -------------------------
        
        /**
          * @param f A function to call when a key state event occurs
          * @tparam U Arbitrary function result type
          * @return A listener that wraps the specified function and uses this factory's conditions & filters
          */
        def apply[U](f: KeyStateEvent => U): KeyStateListener = new _KeyStateListener[U](condition, filter, f)
        
        /**
          * Creates a new listener that receives one event, after which it stops receiving events
          * @param f A function called for the first accepted event
          * @tparam U Arbitrary function result type
          * @return A listener that receives one event only
          */
        def once[U](f: KeyStateEvent => U): KeyStateListener = {
            val completionFlag = SettableFlag()(SysErrLogger)
            conditional(!completionFlag) { event =>
                f(event)
                completionFlag.set()
            }
        }
        
        /**
          * @param f A function that accepts the KeyStateEventFilter object and yields a suitable filter to add
          *          to this factory
          * @return A copy of this factory that applies the resulting filter.
          *         If there were already filters applied, combines these with logical and.
          */
        def buildFilter(f: KeyStateEventFilter.type => KeyStateEventFilter) =
            withFilter(f(KeyStateEventFilter))
    }
    
    private class _KeyStateListener[U](override val handleCondition: Flag,
                                       override val keyStateEventFilter: KeyStateEventFilter,
                                       f: KeyStateEvent => U)
        extends KeyStateListener
    {
        override def onKeyState(event: KeyStateEvent): Unit = f(event)
    }
}

/**
  * Common trait for listeners that are interested in receiving events concerning keyboard state changes
  * @author Mikko Hilpinen
  * @since 22.2.2017
  */
trait KeyStateListener extends Handleable
{
    /**
      * This method will be called when the keyboard state changes, provided this items handling state allows it.
      * @param event The key-state event that just occurred
      */
    def onKeyState(event: KeyStateEvent): Unit
    /**
      * This listener will only be called for events accepted by this filter.
      */
    def keyStateEventFilter: KeyStateEventFilter
}