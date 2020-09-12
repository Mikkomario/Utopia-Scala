package utopia.inception.handling.mutable

import utopia.inception.handling.HandlerType
import utopia.inception.handling.mutable.HandlerRelay.AnyHandler

object HandlerRelay
{
    // TYPES    -------------------
    
    type Handleable = utopia.inception.handling.Handleable
    type AnyHandler = Handler[_ <: Handleable]
    
    
    // OPERATORS    ---------------
    
    /**
      * @return A new empty relay
      */
    def apply() = new HandlerRelay(Vector())
    
    /**
      * @param handlers Handlers
      * @return A relay with specified handlers
      */
    def apply(handlers: IterableOnce[AnyHandler]) = new HandlerRelay(handlers)
    
    /**
      * @param handler A handler
      * @return A relay with the provided handler
      */
    def apply(handler: AnyHandler) = new HandlerRelay(Vector(handler))
    
    /**
      * @param first A handler
      * @param second Another handler
      * @param more More handlers
      * @return A relay with all the provided handlers
      */
    def apply(first: AnyHandler, second: AnyHandler, more: AnyHandler*) = new HandlerRelay(Vector(first, second) ++ more)
}

/**
 * HandlerRelays are as interfaces for handler groupings. Elements can be added to multiple handlers
 * at once through a relay.
 * @author Mikko Hilpinen
 * @since 22.10.2016
 */
class HandlerRelay(initialHandlers: IterableOnce[AnyHandler])
{
    // TYPES    -------------------
    
    type Handleable = HandlerRelay.Handleable
    
    
    // ATTRIBUTES    --------------
    
    private var _handlers: Map[HandlerType, AnyHandler] = initialHandlers.iterator.map { h => h.handlerType -> h }.toMap
    
    /**
      * @return A mapping of handlers, each tied to their handler type
      */
    def handlerMap = _handlers
    
    /**
     * The handlers registered into this relay.
     * @see register(Handler)
     */
    def handlers = _handlers.values
    
    
    // COMPUTED ------------------
    
    /**
      * @return A string representation of the contents of this relay
      */
    def debugString = s"Handler Relay with ${_handlers.size} handlers: [${
        _handlers.map { case (_, h) => s"\n\t- ${h.debugString}" }.mkString("")}\n]"
    
    
    // OPERATORS    --------------
    
    /**
     * Adds a single element to the suitable handlers in this relay
     * @param element the element that is added
     */
    def +=(element: Handleable) = handlers.foreach { _ ?+= element }
    
    /**
     * Adds a number of elements to suitable handlers in this relay
     * @param elements The elements to be added
     */
    def ++=(elements: IterableOnce[Handleable]) = elements.iterator.foreach(+=)
    
    /**
     * Adds two or more elements to suitable handlers in this relay
     */
    def ++=(first: Handleable, second: Handleable, more: Handleable*): Unit = this ++= (Vector(first, second) ++ more)
    
    /**
     * Removes an element from each handler in this relay
     */
    def -=(element: Handleable) = handlers.foreach { _ -= element }
    
    /**
     * Removes multiple elements from each handler in this relay
     */
    def --=(elements: Iterable[Handleable]) = handlers.foreach { _ --= elements }
    
    /**
     * Removes two or more elements from the handlers in this relay
     */
    def --=(first: Handleable, second: Handleable, more: Handleable*): Unit = this --= (Vector(first, second) ++ more)
    
    
    // OTHER    -------------------
    
    /**
      * @return A copy of this relay with shared handlers
      */
    def copy() = new HandlerRelay(handlers)
    
    /**
      * Registers a new handler to this relay. If there already exists another handler with the same handler type,
      * that will be replaced with this new one
      * @param handler A new handler
      */
    def register(handler: AnyHandler) = _handlers += handler.handlerType -> handler
    
    def register(handlers: IterableOnce[AnyHandler]) = _handlers ++= handlers.iterator.map { h => h.handlerType -> h }
    
    def register(first: AnyHandler, second: AnyHandler, more: AnyHandler*): Unit = register(Vector(first, second) ++ more)
    
    def remove(handler: AnyHandler) = _handlers = _handlers.filterNot { case (_, existing) => existing == handler }
    
    def remove(handlers: Iterable[AnyHandler]) = _handlers = _handlers.filterNot {
            case (_, existing) => handlers.exists { _ == existing } }
}