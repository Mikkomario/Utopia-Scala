package utopia.genesis.handling.template

import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.view.immutable.View
import utopia.genesis.handling.template.Handlers.AnyHandler

import scala.collection.mutable

object Handlers extends FromCollectionFactory[Handler2[_ <: Handleable2], Handlers]
{
	// TYPES    -----------------------
	
	/**
	  * A handler type without specification on what type of content it manages
	  */
	type AnyHandler = Handler2[_ <: Handleable2]
	
	
	// IMPLEMENTED  -------------------
	
	override def from(items: IterableOnce[AnyHandler]): Handlers = apply(Iterable.from(items))
	
	
	// OTHER    -----------------------
	
	/**
	  * Wraps a set of handlers
	  * @param handlers Handlers to wrap
	  * @return A new Handlers instance that wraps the specified handlers
	  */
	def apply(handlers: Iterable[AnyHandler]): Handlers = new _Handlers(handlers)
	/**
	  * Wraps a possibly mutating set of handlers
	  * @param handlerView A view into the handlers to wrap
	  * @return A Handlers instance that wraps the handlers accessible through the specified View at any time
	  */
	def apply(handlerView: View[Iterable[AnyHandler]]): Handlers = new ViewHandlers(handlerView)
	
	
	// NESTED   -----------------------
	
	private class _Handlers(override val handlers: Iterable[AnyHandler]) extends Handlers
	
	private class ViewHandlers(handlersView: View[Iterable[AnyHandler]]) extends Handlers
	{
		override protected def handlers: Iterable[AnyHandler] = handlersView.value
	}
}

/**
  * Common trait for a collection of handlers
  * @author Mikko Hilpinen
  * @since 30/01/2024, v4.0
  */
trait Handlers extends mutable.Growable[Handleable2]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The wrapped handlers
	  */
	protected def handlers: Iterable[AnyHandler]
	
	
	// IMPLEMENTED  ------------------
	
	override def addOne(elem: Handleable2) = {
		handlers.foreach { _ ?+= elem }
		this
	}
	
	override def clear() = handlers.foreach { _.clear() }
	
	
	// OTHER    ---------------------
	
	/**
	  * Removes an item from all associated handlers
	  * @param item An item to remove
	  */
	def -=(item: Handleable2): Unit = handlers.foreach { _ -= item }
	/**
	  * Removes the specified items from all associated handlers
	  * @param items Items to remove
	  */
	def --=(items: IterableOnce[Handleable2]) = {
		val _items = Set.from(items)
		if (_items.nonEmpty)
			handlers.foreach { _.removeWhere(_items.contains) }
	}
}
