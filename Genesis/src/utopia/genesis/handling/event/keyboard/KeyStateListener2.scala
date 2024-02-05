package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.Flag
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.event.KeyLocation
import KeyEvent.KeyFilteringFactory
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.keyboard.KeyStateListener2.KeyStateEventFilter
import utopia.genesis.handling.template.Handleable2

import scala.annotation.unused
import scala.language.implicitConversions

object KeyStateListener2
{
    // TYPES    -------------------------
    
    /**
      * Type for filters applied to key state -events
      */
    type KeyStateEventFilter = Filter[KeyStateEvent2]
    
    
    // ATTRIBUTES   ------------------
    
    /**
      * A factory used for constructing unconditional key-state-event listeners
      */
    val unconditional = KeyStateListenerFactory()
    
    
    // COMPUTED ----------------------
    
    /**
      * @return Access point for key-state-event -related filters
      */
    def filter = KeyStateEventFilter
    
    
    // IMPLICIT ----------------------
    
    implicit def objectToFactory(@unused o: KeyStateListener2.type): KeyStateListenerFactory = unconditional
    
    /**
      * @param f A function that accepts a key-state event
      * @return A key-state listener that wraps the specified function
      */
    implicit def apply(f: KeyStateEvent2 => Unit): KeyStateListener2 = unconditional(f)
    
    
    // NESTED   -----------------------------
    
    /**
      * Common trait for factory-like classes that support key-state-event -based filtering
      * @tparam A Type of generated items
      */
    trait KeyStateFilteringFactory[+A] extends KeyFilteringFactory[KeyStateEvent2, A]
    {
        // COMPUTED   ---------------------
        
        /**
          * An item that only accepts key-pressed events
          */
        def pressed = withFilter { _.pressed }
        /**
          * An item that only accepts key-released events
          */
        def released = withFilter { _.released }
        
        
        // OTHER    -----------------------
        
        /**
          * @param location Targeted location
          * @return Filter that only accepts key events at that location
          */
        def location(location: KeyLocation) = withFilter { _.location == location }
        /**
          * @param key Targeted key
          * @param location Targeted specific key location
          * @return Filter that only accepts key events of that key at that specific location
          */
        def specificKey(key: Key, location: KeyLocation) = withFilter { _.concernsKey(key, location) }
    }
    
    object KeyStateEventFilter extends KeyStateFilteringFactory[KeyStateEventFilter]
    {
        // ATTRIBUTES   ---------------------
        
        /**
          * A filter that only accepts key-pressed events
          */
        override lazy val pressed = super.pressed
        /**
          * A filter that only accepts key-released events
          */
        override lazy val released = super.released
        
        
        // IMPLEMENTED  ---------------------
        
        override protected def withFilter(filter: Filter[KeyStateEvent2]): KeyStateEventFilter = filter
        
        
        // OTHER    -------------------------
        
        /**
          * @param f A filter function
          * @return A key-state event-filter that applies the specified function
          */
        def apply(f: KeyStateEvent2 => Boolean): KeyStateEventFilter = Filter[KeyStateEvent2](f)
        
        /**
          * @param char A character (key)
          * @return A filter that only accepts events of that key's pressed-events,
          *         and only while a control key is being held down
          */
        def controlChar(char: Char) = whileControlDown && pressed && this.char(char)
    }
    
    case class KeyStateListenerFactory(condition: FlagLike = AlwaysTrue,
                                       filter: KeyStateEventFilter = AcceptAll)
        extends ListenerFactory[KeyStateEvent2, KeyStateListener2, KeyStateListenerFactory]
            with KeyStateFilteringFactory[KeyStateListenerFactory]
    {
        // IMPLEMENTED  ---------------------
        
        override def usingFilter(filter: Filter[KeyStateEvent2]): KeyStateListenerFactory = copy(filter = filter)
        override def usingCondition(condition: Changing[Boolean]): KeyStateListenerFactory = copy(condition = condition)
	    
	    override def apply[U](f: KeyStateEvent2 => U): KeyStateListener2 = new _KeyStateListener[U](condition, filter, f)
	    
	    override protected def withFilter(filter: Filter[KeyStateEvent2]): KeyStateListenerFactory = filtering(filter)
	    
	    
	    // OTHER    -------------------------
        
        /**
          * Creates a new listener that receives one event, after which it stops receiving events
          * @param f A function called for the first accepted event
          * @tparam U Arbitrary function result type
          * @return A listener that receives one event only
          */
        def once[U](f: KeyStateEvent2 => U): KeyStateListener2 = {
            val completionFlag = Flag()
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
    
    private class _KeyStateListener[U](override val handleCondition: FlagLike,
                                       override val keyStateEventFilter: KeyStateEventFilter,
                                       f: KeyStateEvent2 => U)
        extends KeyStateListener2
    {
        override def onKeyState(event: KeyStateEvent2): Unit = f(event)
    }
}

/**
  * Common trait for listeners that are interested in receiving events concerning keyboard state changes
  * @author Mikko Hilpinen
  * @since 22.2.2017
  */
trait KeyStateListener2 extends Handleable2
{
    /**
      * This method will be called when the keyboard state changes, provided this items handling state allows it.
      * @param event The key-state event that just occurred
      */
    def onKeyState(event: KeyStateEvent2): Unit
    /**
      * This listener will only be called for events accepted by this filter.
      */
    def keyStateEventFilter: KeyStateEventFilter
}