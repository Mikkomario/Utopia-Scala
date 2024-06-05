package utopia.genesis.handling.template

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.view.mutable.Pointer
import utopia.genesis.handling.template.Handlers.AnyHandler

object MutableHandlers extends FromCollectionFactory[AnyHandler, MutableHandlers]
{
	// IMPLEMENTED  -------------------------
	
	override def from(items: IterableOnce[AnyHandler]): MutableHandlers = apply(items)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param pointer A mutable pointer to the managed handlers
	  * @return An instance that utilizes / wraps the specified pointer
	  */
	def apply(pointer: Pointer[Seq[AnyHandler]] = Pointer(Empty)): MutableHandlers =
		new _MutableHandlers(pointer)
	/**
	  * @param handlers Set of handlers to assign initially
	  * @return An instance that contains the specified handlers, initially
	  */
	def apply(handlers: IterableOnce[AnyHandler]): MutableHandlers = apply(Pointer(Seq.from(handlers)))
	
	
	// NESTED   -----------------------------
	
	private class _MutableHandlers(override val handlersPointer: Pointer[Seq[AnyHandler]]) extends MutableHandlers
}

/**
  * Common trait for mutable sets of handlers.
  * Allows the addition and removal of specific handlers.
  * @author Mikko Hilpinen
  * @since 25/02/2024, v4.0
  */
trait MutableHandlers extends Handlers
{
	// ABSTRACT --------------------------
	
	/**
	  * @return A mutable pointer to the managed handlers
	  */
	def handlersPointer: Pointer[Seq[AnyHandler]]
	
	
	// IMPLEMENTED  ----------------------
	
	override def handlers: Seq[AnyHandler] = handlersPointer.value
	def handlers_=(newHandlers: Seq[AnyHandler]) = handlersPointer.value = newHandlers
	
	
	// OTHER    --------------------------
	
	/**
	  * Adds a new handler to this collection
	  * @param handler Handler to add
	  */
	def addHandler(handler: AnyHandler) = handlersPointer.update { _ :+ handler }
	/**
	  * Adds 0-n new handlers to this collection
	  * @param handlers Handlers to add
	  */
	def addHandlers(handlers: IterableOnce[AnyHandler]) = handlersPointer.update { _ ++ handlers }
	def addHandlers(handler1: AnyHandler, handler2: AnyHandler, more: AnyHandler*): Unit =
		addHandlers(Pair(handler1, handler2) ++ more)
	
	/**
	  * Removes an handler from this collection
	  * @param handler Handler to remove
	  */
	def removeHandler(handler: AnyHandler) = handlersPointer.update { _.filterNot { _ == handler } }
	/**
	  * Removes 0-n handlers from this collection
	  * @param handlers Handlers to remove
	  */
	def removeHandlers(handlers: Set[AnyHandler]) = handlersPointer.update { _.filterNot(handlers.contains) }
	def removeHandlers(h1: AnyHandler, h2: AnyHandler, more: AnyHandler*): Unit = removeHandlers(Set(h1, h2) ++ more)
}
