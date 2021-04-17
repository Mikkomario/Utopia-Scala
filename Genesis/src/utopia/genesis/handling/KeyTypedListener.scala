package utopia.genesis.handling

import scala.language.implicitConversions

import utopia.genesis.event.KeyTypedEvent
import utopia.inception.handling.Handleable

object KeyTypedListener
{
    // IMPLICIT -----------------------------
    
    // Automatically converts a function to a listener
    implicit def functionToListener(function: KeyTypedEvent => Unit): KeyTypedListener = apply(function)
    
    
    // OTHER    -----------------------------
    
    /**
      * Creates a simple key typed listener that calls specified function each time an event is received
      * @param f a function that will be called
      * @return A new key typed listener
      */
    def apply(f: KeyTypedEvent => Unit): KeyTypedListener = new FunctionalKeyTypedListener(f)
    
    
    // NESTED   -----------------------------
    
    private class FunctionalKeyTypedListener(f: KeyTypedEvent => Unit) extends KeyTypedListener
        with utopia.inception.handling.immutable.Handleable
    {
        override def onKeyTyped(event: KeyTypedEvent) = f(event)
    }
}

/**
 * These listeners are interested in receiving key typed events
 * @author Mikko Hilpinen
 * @since 23.2.2017
 */
trait KeyTypedListener extends Handleable
{
    /**
     * This method will be called in order to inform the instance of new key typed events
      * @param event the newly occurred key typed event
     */
    def onKeyTyped(event: KeyTypedEvent): Unit
    
    /**
      * @return Whether this instance is receiving key typed events
      */
    def isReceivingKeyTypedEvents = allowsHandlingFrom(KeyTypedHandlerType)
}