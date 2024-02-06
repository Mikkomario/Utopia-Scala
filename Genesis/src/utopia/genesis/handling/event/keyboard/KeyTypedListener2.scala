package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.template.Handleable2

import scala.annotation.unused
import scala.language.implicitConversions

object KeyTypedListener2
{
    // TYPES    --------------------------
    
    /**
      * Filter class for key typed -events
      */
    type KeyTypedEventFilter = Filter[KeyTypedEvent2]
    
    
    // ATTRIBUTES   ----------------------
    
    /**
      * A key typed listener factory that doesn't apply any listening conditions or event filters
      */
    val unconditional = KeyTypedListenerFactory()
    
    
    // IMPLICIT --------------------------
    
    implicit def objectToFactory(@unused o: KeyTypedListener2.type): KeyTypedListenerFactory = unconditional
    
    
    // NESTED   --------------------------
    
    case class KeyTypedListenerFactory(condition: FlagLike = AlwaysTrue, filter: KeyTypedEventFilter = AcceptAll)
        extends ListenerFactory[KeyTypedEvent2, KeyTypedListenerFactory]
    {
        // IMPLEMENTED  ------------------
        
        override def usingFilter(filter: Filter[KeyTypedEvent2]): KeyTypedListenerFactory = copy(filter = filter)
        override def usingCondition(condition: Changing[Boolean]): KeyTypedListenerFactory = copy(condition = condition)
        
        
        // OTHER    ----------------------
        
        /**
          * @param f A function to call upon key typed -events
          * @tparam U Arbitrary function result type
          * @return A listener that calls the specified function, after applying this factory's conditions & filters
          */
        def apply[U](f: KeyTypedEvent2 => U): KeyTypedListener2 = new _KeyTypedListener[U](condition, filter, f)
    }
    
    private class _KeyTypedListener[U](override val handleCondition: FlagLike,
                                       override val keyTypedEventFilter: KeyTypedEventFilter, f: KeyTypedEvent2 => U)
        extends KeyTypedListener2
    {
        override def onKeyTyped(event: KeyTypedEvent2): Unit = f(event)
    }
}

/**
 * These listeners are interested in receiving key typed -events
 * @author Mikko Hilpinen
 * @since 23.2.2017
 */
trait KeyTypedListener2 extends Handleable2
{
    /**
      * @return A filter used for filtering incoming key typed -events
      */
    def keyTypedEventFilter: Filter[KeyTypedEvent2]
    
    /**
     * This method will be called in order to inform the instance of new key typed -events
      * @param event the newly occurred key typed -event
     */
    def onKeyTyped(event: KeyTypedEvent2): Unit
}