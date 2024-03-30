package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter, RejectAll}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.keyboard.KeyEvent.KeyFilteringFactory
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused
import scala.language.implicitConversions

object KeyTypedListener
{
    // TYPES    --------------------------
    
    /**
      * Filter class for key typed -events
      */
    type KeyTypedEventFilter = Filter[KeyTypedEvent]
    
    
    // ATTRIBUTES   ----------------------
    
    /**
      * A key typed listener factory that doesn't apply any listening conditions or event filters
      */
    val unconditional = KeyTypedListenerFactory()
    
    
    // COMPUTED --------------------------
    
    /**
     * @return A factory for constructing key typed event filters
     */
    def filter = KeyTypedEventFilter
    
    
    // IMPLICIT --------------------------
    
    implicit def objectToFactory(@unused o: KeyTypedListener.type): KeyTypedListenerFactory = unconditional
    
    
    // NESTED   --------------------------
    
    trait KeyTypedFilteringFactory[+A] extends KeyFilteringFactory[KeyTypedEvent, A]
    {
        /**
         * @param char Targeted character
         * @return An item that only accepts events concerning that typed character
         */
        def apply(char: Char): A = withFilter { _.typedChar == char }
        /**
         * @param chars Targeted characters
         * @return An item that only accepts events concerning those typed characters
         */
        def chars(chars: Set[Char]): A = {
            if (chars.isEmpty)
                withFilter(RejectAll)
            else
                withFilter { e => chars.contains(e.typedChar) }
        }
    }
    
    object KeyTypedEventFilter extends KeyTypedFilteringFactory[KeyTypedEventFilter]
    {
        // IMPLEMENTED  -------------------
        
        override protected def withFilter(filter: Filter[KeyTypedEvent]): KeyTypedEventFilter = filter
        
        
        // OTHER    ----------------------
        
        /**
         * @param f A filtering-function for key typed -events
         * @return A filter that uses the specified function
         */
        def apply(f: KeyTypedEvent => Boolean) = Filter(f)
    }
    
    case class KeyTypedListenerFactory(condition: FlagLike = AlwaysTrue, filter: KeyTypedEventFilter = AcceptAll)
        extends ListenerFactory[KeyTypedEvent, KeyTypedListenerFactory]
            with KeyTypedFilteringFactory[KeyTypedListenerFactory]
    {
        // IMPLEMENTED  ------------------
        
        override def usingFilter(filter: Filter[KeyTypedEvent]): KeyTypedListenerFactory = copy(filter = filter)
        override def usingCondition(condition: FlagLike): KeyTypedListenerFactory = copy(condition = condition)
        
        override protected def withFilter(filter: Filter[KeyTypedEvent]): KeyTypedListenerFactory =
            copy(filter = this.filter && filter)
        
        
        // OTHER    ----------------------
        
        /**
          * @param f A function to call upon key typed -events
          * @tparam U Arbitrary function result type
          * @return A listener that calls the specified function, after applying this factory's conditions & filters
          */
        def apply[U](f: KeyTypedEvent => U): KeyTypedListener = new _KeyTypedListener[U](condition, filter, f)
    }
    
    private class _KeyTypedListener[U](override val handleCondition: FlagLike,
                                       override val keyTypedEventFilter: KeyTypedEventFilter, f: KeyTypedEvent => U)
        extends KeyTypedListener
    {
        override def onKeyTyped(event: KeyTypedEvent): Unit = f(event)
    }
}

/**
 * These listeners are interested in receiving key typed -events
 * @author Mikko Hilpinen
 * @since 23.2.2017
 */
trait KeyTypedListener extends Handleable
{
    /**
      * @return A filter used for filtering incoming key typed -events
      */
    def keyTypedEventFilter: Filter[KeyTypedEvent]
    
    /**
     * This method will be called in order to inform the instance of new key typed -events
      * @param event the newly occurred key typed -event
     */
    def onKeyTyped(event: KeyTypedEvent): Unit
}