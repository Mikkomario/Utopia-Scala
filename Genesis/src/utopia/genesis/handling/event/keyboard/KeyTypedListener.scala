package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.ListenerFactory
import utopia.genesis.handling.event.keyboard.KeyTypedEvent.{KeyTypedEventFilter, KeyTypedFilteringFactory}
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused
import scala.language.implicitConversions

object KeyTypedListener
{
    // ATTRIBUTES   ----------------------
    
    /**
      * A key typed listener factory that doesn't apply any listening conditions or event filters
      */
    val unconditional = KeyTypedListenerFactory()
    
    
    // IMPLICIT --------------------------
    
    implicit def objectToFactory(@unused o: KeyTypedListener.type): KeyTypedListenerFactory = unconditional
    
    
    // NESTED   --------------------------
    
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