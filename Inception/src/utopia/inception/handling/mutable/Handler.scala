package utopia.inception.handling.mutable

import utopia.flow.collection.mutable.VolatileList
import utopia.inception.handling
import utopia.inception.handling.HandlerType

object Handler
{
    def apply[A <: handling.Handleable](hType: HandlerType, elements: IterableOnce[A] = Vector()) = new Handler(elements)
    {
        val handlerType = hType
    }
    
    def apply[A <: handling.Handleable](handlerType: HandlerType, element: A): Handler[A] = apply(handlerType, Vector(element))
    
    def apply[A <: handling.Handleable](handlerType: HandlerType, first: A, second: A, more: A*): Handler[A] = apply(handlerType, Vector(first, second) ++ more)
}

/**
  * This is a mutable implementation of the Handler trait. This handler is safe to use in a multithreaded environment
  * @author Mikko Hilpinen
 * @since 20.10.2016 (Rewritten: 5.4.2019, v2+)
  * @tparam A The type of object handled by this handler
 */
abstract class Handler[A <: handling.Handleable](initialElements: IterableOnce[A]) extends handling.Handler[A]
{
    // ATTRIBUTES   -------------------
    
    private val elements = VolatileList.from(initialElements)
    
    
    // IMPLEMENTED    -----------------
    
    override def aliveElements = elements.updateAndGet { _.filterNot(considersDead) }
    
    
    // OPERATORS    -----------------
    
    /**
     * Adds a new element to this handler
      * @param element The element to be added to this handler
     */
    def +=(element: A) = elements :+= element
    /**
     * Adds the contents of a collection to this handler
     * @param elements The elements to be added to this handler
     */
    def ++=(elements: IterableOnce[A]) = this.elements ++= elements
    /**
     * Adds two or more elements to this handler
     */
    def ++=(first: A, second: A, more: A*) = this.elements ++=(Vector(first, second) ++ more)
    /**
     * Removes an element from this handler
     * @param element The element to be removed from this handler
     */
    def -=(element: Any) = elements -= element
    /**
     * Removes the provided elements from this handler
     * @param elements The elements to be removed from this handler
     */
    def --=(elements: Iterable[Any]) = this.elements --= elements
    /**
     * Removes two or more elements from this handler
     */
    def --=(first: Any, second: Any, more: Any*): Unit = --=(Vector(first, second) ++ more)
    /**
     * Adds a new element to this handler, provided that it's of correct type. If the element is 
     * not of supported type, it won't be added
     * @param element The element that is being added to this handler
     * @return Was the element suitable to be used by this handler
     */
    def ?+=(element: handling.Handleable) =
    {
        if (handlerType.supportsInstance(element))
        {
            this += element.asInstanceOf[A]
            true
        }
        else
            false
    }
    
    
    // OTHER METHODS    ----------
    
    /**
     * Clears this handler of all elements
     */
    def clear() = elements.clear()
    
    /**
      * Sorts the contents of this handler with an implicit ordering
      * @param ordering The ordering used for sorting the elements
      * @tparam B The type of object accepted by the ordering
      */
    def sort[B >: A]()(implicit ordering: Ordering[B]) = elements.update { _.sorted(ordering) }
    
    /**
     * Sorts the contents of this handler using the specified sorting function
     * @param order The function that determines whether the first element comes before the second
     * element in the new ordering
     */
    def sortWith[U >: A](order: (U, U) => Boolean) = elements.update { _.sortWith(order) }
    
    /**
     * Absorbs the contents of another handler, removing the elements from that one and adding them 
     * to this one
     * @param other The handler that is emptied into this one
     */
    def absorb[U <: A](other: Handler[U]) = ++=(other.elements.getAndSet(Vector()))
}