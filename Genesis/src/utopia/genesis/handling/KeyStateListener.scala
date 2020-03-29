package utopia.genesis.handling

import scala.language.implicitConversions
import java.awt.event.KeyEvent

import utopia.genesis.event.KeyStateEvent
import utopia.inception.handling.Handleable
import utopia.inception.util.{AnyFilter, Filter}

object KeyStateListener
{
    implicit def functionToListener(f: KeyStateEvent => Unit): KeyStateListener = apply()(f)
    
    /**
      * Creates a simple key state listener that calls specified function on key state events
      * @param filter A filter that determines when the function is called (default = no filtering)
      * @param f A function that will be called on key state events
      * @return A new key state listener
      */
    def apply(filter: Filter[KeyStateEvent] = AnyFilter)(f: KeyStateEvent => Unit): KeyStateListener =
        new FunctionalKeyStateListener(f, filter)
    
    /**
      * Creates a simple key state listener that calls specified function on certain key presses
      * @param f A function that will be called
      * @return A new key state listener
      */
    def onKeyPressed(targetKeyIndex: Int)(f: KeyStateEvent => Unit) = apply(KeyStateEvent.wasPressedFilter &&
        KeyStateEvent.keyFilter(targetKeyIndex))(f)
    
    /**
      * Creates a simple key state listener that calls specified function on key presses
      * @param f A function that will be called
      * @return A new key state listener
      */
    def onAnyKeyPressed(f: KeyStateEvent => Unit) = apply(KeyStateEvent.wasPressedFilter)(f)
    
    /**
      * Creates a simple key state listener that calls specified function on key releases
      * @param f A function that will be called
      * @return A new key state listener
      */
    def onKeyReleased(targetKeyIndex: Int)(f: KeyStateEvent => Unit) = apply(KeyStateEvent.wasReleasedFilter &&
        KeyStateEvent.keyFilter(targetKeyIndex))(f)
    
    /**
      * Creates a simple key state listener that calls specified function on key releases
      * @param f A function that will be called
      * @return A new key state listener
      */
    def onAnyKeyReleased(f: KeyStateEvent => Unit) = apply(KeyStateEvent.wasReleasedFilter)(f)
    
    /**
      * Creates a simple key state listener that calls specified function each time enter is pressed
      * @param f A function that will be called
      * @return A new key state listener
      */
    def onEnterPressed(f: KeyStateEvent => Unit) = onKeyPressed(KeyEvent.VK_ENTER)(f)
}

/**
 * These listeners are interested in receiving events whenever the keyboard state changes
 * @author Mikko Hilpinen
 * @since 22.2.2017
 */
trait KeyStateListener extends Handleable
{
    /**
     * This method will be called when the keyboard state changes
     */
    def onKeyState(event: KeyStateEvent)
    
    /**
     * This listener will only be called for events accepted by this filter. By default the filter
     * accepts all key state events.
     */
    def keyStateEventFilter: Filter[KeyStateEvent] = AnyFilter
    
    /**
      * @return Whether this instance is currently willing to receive key state events
      */
    def isReceivingKeyStateEvents = allowsHandlingFrom(KeyStateHandlerType)
}

private class FunctionalKeyStateListener(val f: KeyStateEvent => Unit, val filter: Filter[KeyStateEvent])
    extends KeyStateListener with utopia.inception.handling.immutable.Handleable
{
    override def onKeyState(event: KeyStateEvent) = f(event)
    
    override def keyStateEventFilter = filter
}