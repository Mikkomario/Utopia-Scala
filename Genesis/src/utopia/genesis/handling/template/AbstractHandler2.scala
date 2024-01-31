package utopia.genesis.handling.template

import utopia.flow.collection.mutable.VolatileList

/**
  * An abstract implementation of the Handler trait
  * @author Mikko Hilpinen
  * @since 30/01/2024, v3.6
  */
abstract class AbstractHandler2[A <: Handleable2](initialItems: IterableOnce[A] = Vector.empty) extends Handler2[A]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A pointer that contains the currently attached items in this handler
	  */
	protected val itemsPointer = VolatileList(initialItems)
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def items = itemsPointer.value
	
	override protected def addOneUnconditionally(item: A): Unit = itemsPointer.update { _ :+ item }
	override def removeWhere(f: A => Boolean): Unit = itemsPointer.update { _.filterNot(f) }
	
	override def clear() = itemsPointer.clear()
}
