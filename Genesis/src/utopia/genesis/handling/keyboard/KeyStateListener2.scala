package utopia.genesis.handling.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.Flag
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.event.{KeyLocation, KeyStateEvent}
import utopia.genesis.handling.template.Handleable2
import utopia.paradigm.enumeration.Direction2D

import java.awt.event.KeyEvent
import scala.annotation.unused
import scala.language.implicitConversions

object KeyStateListener2
{
    // TYPES    -------------------------
    
    /**
      * Type for filters applied to key state -events
      */
    type KeyStateEventFilter = Filter[KeyStateEvent]
    
    
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
    implicit def apply(f: KeyStateEvent => Unit): KeyStateListener2 = unconditional(f)
    
    
    // NESTED   -----------------------------
    
    /**
      * Common trait for factory-like classes that support key-state-event -based filtering
      * @tparam A Type of generated items
      */
    trait KeyStateFilteringFactory[+A]
    {
        // ABSTRACT -------------------------
        
        /**
          * @param filter A filter to apply
          * @return An item with that filter applied
          */
        protected def withFilter(filter: Filter[KeyStateEvent]): A
        
        
        // COMPUTED   ---------------------
        
        /**
          * An item that only accepts key-pressed events
          */
        def pressed = withFilter { _.isDown }
        /**
          * An item that only accepts key-released events
          */
        def released = withFilter { _.isReleased }
        
        /**
          * An item that only accepts arrow key -related events
          */
        def anyArrow = keys(Direction2D.values.map(KeyStateEvent.arrowKeyIndex).toSet)
        
        /**
          * An item that only applies while the control key (at any location) is down
          */
        def whileControlDown = whileKeyDown(KeyEvent.VK_CONTROL)
        /**
          * An item that only applies while the shift key (at any location) is down
          */
        def whileShiftDown = whileKeyDown(KeyEvent.VK_SHIFT)
        
        
        // OTHER    -------------------------
        
        /**
          * @param index Index of the targeted key (see [[java.awt.event.KeyEvent]])
          * @return An item that only accepts events concerning the specified key
          */
        def key(index: Int) = withFilter { _.index == index }
        /**
          * @param index Index of the targeted key (see [[java.awt.event.KeyEvent]])
          * @param location Location where the key must be pressed
          * @return An item that only accepts events concerning that specific key at that specific location
          */
        def key(index: Int, location: KeyLocation) =
            withFilter { e => e.index == index && e.location == location }
        /**
          * @param keyIndices Indices of the targeted keys (see [[java.awt.event.KeyEvent]])
          * @return An item that only accepts events that concern one of the specified keys
          */
        def keys(keyIndices: Set[Int]) = withFilter { e => keyIndices.contains(e.index) }
        /**
          * @return An item that only accepts events that concern one of the specified keys
          */
        def keys(keyIndex1: Int, keyIndex2: Int, moreIndices: Int*): A =
            keys(Set(keyIndex1, keyIndex2) ++ moreIndices)
        
        /**
          * @param character A character (key)
          * @return An item that only accepts events concerning that character-key
          */
        def char(character: Char) = withFilter { _.isCharacter(character) }
        /**
          * @param characters Character keys
          * @return An item that only accepts events concerning those character-keys
          */
        def chars(characters: Iterable[Char]) = withFilter { e => characters.exists(e.isCharacter) }
        /**
          * @return An item that only accepts events concerning the specified character-keys
          */
        def chars(char1: Char, char2: Char, moreChars: Char*): A = chars(Set(char1, char2) ++ moreChars)
        
        /**
          * @param direction Arrow key direction
          * @return An item that only accepts events concerning the targeted arrow key
          */
        def arrow(direction: Direction2D) = key(KeyStateEvent.arrowKeyIndex(direction))
        
        /**
          * @param keyIndex Index of the targeted key (see [[java.awt.event.KeyEvent]])
          * @return An item that only accepts events while the specified key is held down
          *         (i.e. accepts key-combos involving that key)
          */
        def whileKeyDown(keyIndex: Int) = withFilter { _.keyStatus(keyIndex) }
        /**
          * @param keyIndex Index of the targeted key (see [[java.awt.event.KeyEvent]])
          * @param location Location where the key must be pressed
          * @return An item that only accepts events while the specified key is held down
          *         (i.e. accepts key-combos involving that key)
          */
        def whileKeyDown(keyIndex: Int, location: KeyLocation) = withFilter { _.keyStatus(keyIndex, location) }
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
        
        override protected def withFilter(filter: Filter[KeyStateEvent]): KeyStateEventFilter = filter
        
        
        // OTHER    -------------------------
        
        /**
          * @param f A filter function
          * @return A key-state event-filter that applies the specified function
          */
        def apply(f: KeyStateEvent => Boolean): KeyStateEventFilter = Filter[KeyStateEvent](f)
        
        /**
          * @param char A character (key)
          * @return A filter that only accepts events of that key's pressed-events,
          *         and only while a control key is being held down
          */
        def controlChar(char: Char) = whileControlDown && pressed && this.char(char)
    }
    
    case class KeyStateListenerFactory(condition: FlagLike = AlwaysTrue,
                                       filter: Filter[KeyStateEvent] = AcceptAll)
        extends KeyStateFilteringFactory[KeyStateListenerFactory]
    {
        // IMPLEMENTED  ---------------------
        
        /**
          * @param filter A filter applied to incoming events.
          *               Only events accepted by this filter will trigger this listener / wrapped function.
          * @return Copy of this factory that applies the specified filter to the created listeners.
          *         If there were already other filters specified, combines these filters with logical and.
          */
        override def withFilter(filter: Filter[KeyStateEvent]) =
            copy(filter = this.filter && filter)
            
        
        // OTHER    -------------------------
        
        /**
          * @param f Function called when a key event is received
          * @tparam U Arbitrary function result type
          * @return A new listener that calls the specified function
          *         when the conditions listed in this factory are met.
          */
        def apply[U](f: KeyStateEvent => U): KeyStateListener2 = new _KeyStateListener[U](condition, filter, f)
        
        /**
          * Creates a new listener that receives one event, after which it stops receiving events
          * @param f A function called for the first accepted event
          * @tparam U Arbitrary function result type
          * @return A listener that receives one event only
          */
        def once[U](f: KeyStateEvent => U): KeyStateListener2 = {
            val completionFlag = Flag()
            listeningWhile(!completionFlag) { event =>
                f(event)
                completionFlag.set()
            }
        }
        
        /**
          * @param condition A condition that must be met for this listener to receive event data
          * @return Copy of this factory that applies this filter to created listeners.
          *         If there was a condition already in place, both of these conditions will be applied
          *         (using logical and).
          */
        def listeningWhile(condition: Changing[Boolean]) =
            copy(condition = this.condition && condition)
        
        /**
          * @param f A mapping function to apply to the current event-filter used by this factory
          * @return Copy of this factory with the mapped filter applied
          */
        def mapFilter(f: Mutate[Filter[KeyStateEvent]]) = copy(filter = f(filter))
        /**
          * @param f A function that accepts the KeyStateEventFilter object and yields a suitable filter to add
          *          to this factory
          * @return A copy of this factory that applies the resulting filter.
          *         If there were already filters applied, combines these with logical and.
          */
        def buildFilter(f: KeyStateEventFilter.type => KeyStateEventFilter) = withFilter(f(KeyStateEventFilter))
    }
    
    private class _KeyStateListener[U](override val handleCondition: FlagLike,
                                       override val keyStateEventFilter: Filter[KeyStateEvent],
                                       f: KeyStateEvent => U)
        extends KeyStateListener2
    {
        override def onKeyState(event: KeyStateEvent): Unit = f(event)
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
    def onKeyState(event: KeyStateEvent): Unit
    /**
     * This listener will only be called for events accepted by this filter.
     */
    def keyStateEventFilter: Filter[KeyStateEvent]
}