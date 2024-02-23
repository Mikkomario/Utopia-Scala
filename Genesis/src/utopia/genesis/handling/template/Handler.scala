package utopia.genesis.handling.template

import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.view.template.eventful.Changing

import scala.collection.mutable

/**
  * Handlers are container which perform some kind of operation on the items connected to them.
  * Typically handlers are used for distributing events.
  * Handlers are mutable in nature.
  * @author Mikko Hilpinen
  * @since 30/01/2024, v4.0
  */
trait Handler[A <: Handleable] extends mutable.Growable[A]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Current list of the items handled.
	  *         This list will be automatically updated, so that it will only contain items that are currently active
	  *         (whose handleCondition allows it).
	  *
	  *         The sub-classes should only add or remove items from this collection via this interface
	  *         (i.e. using +=, -= etc. functions)
	  */
	protected def items: Iterable[A]
	
	/**
	  * Adds an item to this handler.
	  * The implementation of this function assumes that the item's handling condition has been accounted for already.
	  * @param item An item to add to this handler
	  */
	protected def addOneUnconditionally(item: A): Unit
	/**
	  * Conditionally removes items from this handler
	  * @param f A function that yields true for the items to remove from this handler
	  */
	def removeWhere(f: A => Boolean): Unit
	
	/**
	  * Tests whether the specified item is of the handleable type (or convertible to one)
	  * @param item An item that may or may not be processed by this handler
	  * @return Some if the item could be converted to the accepted type. None otherwise.
	  */
	protected def asHandleable(item: Handleable): Option[A]
	
	
	// IMPLEMENTED  --------------------
	
	override def addOne(elem: A) = {
		_handleWhile(elem, elem.handleCondition)
		this
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * @param item An item
	  * @return Whether this handler contains the specified item
	  */
	def contains(item: A) = items.exists { _ == item }
	
	/**
	  * Removes an item from this handler
	  * @param item The item to remove
	  */
	def -=(item: Handleable): Unit = removeWhere { _ == item }
	/**
	  * Removes a number of items from this handler
	  * @param items Items to remove
	  */
	def --=(items: IterableOnce[Handleable]) = {
		val _items = Set.from(items)
		if (_items.nonEmpty)
			removeWhere(_items.contains)
	}
	/**
	  * Conditionally attaches an item to this handler
	  * @param item An item to attach to this handler + condition that is required for keeping that item attached
	  */
	def +=(item: (A, Changing[Boolean])): Unit = handleWhile(item._1, item._2)
	/**
	  * Conditionally attaches an item to this handler
	  * @param item An item to attach to this handler
	  * @param condition Condition that is required for keeping that item attached
	  */
	def handleWhile(item: A, condition: Changing[Boolean]) = _handleWhile(item, item.handleCondition && condition)
	
	/**
	  * Adds the specified item to this handler, provided this handler supports / accepts that item
	  * @param item An item that may be handled by this handler
	  * @return Whether that item was attached to this handler
	  */
	def ?+=(item: Handleable): Boolean = asHandleable(item) match {
		case Some(item) =>
			addOne(item)
			true
		case None => false
	}
	
	private def _handleWhile(item: A, condition: Changing[Boolean]) = {
		// Keeps the item attached while the condition holds
		// If the item is removed via some other manner, however, won't reattach it even if the condition becomes true
		condition.addListenerAndSimulateEvent(false) { e =>
			// Case: Condition triggered => Attaches the item to this handler
			if (e.newValue) {
				// Case: Already contained the specified item (manually added) => Stops following the condition
				if (contains(item))
					Detach
				// Case: Default => Adds the item
				else {
					addOneUnconditionally(item)
					Continue
				}
			}
			// Case: Condition released => Removes the item from this handler
			else if (contains(item)) {
				this -= item
				Continue
			}
			// Case: Condition released after the item was removed via some other manner =>
			// Stops listening to the condition
			else
				Detach
		}
	}
}
