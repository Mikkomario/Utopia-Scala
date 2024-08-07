package utopia.genesis.handling.template

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.mutable.VolatileList

/**
  * An abstract implementation of the Handler trait
  * @author Mikko Hilpinen
  * @since 30/01/2024, v4.0
  */
abstract class AbstractHandler[A <: Handleable](initialItems: IterableOnce[A] = Empty) extends Handler[A]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A pointer that contains the currently attached items in this handler
	  */
	protected val itemsPointer = VolatileList[A]()
	
	
	// INITIAL CODE ------------------------
	
	// Attaches the initial items
	this ++= initialItems
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def items = itemsPointer.value
	
	override protected def addOneUnconditionally(item: A): Unit = itemsPointer.update { _ :+ item }
	override def removeWhere(f: A => Boolean): Unit = itemsPointer.update { _.filterNot(f) }
	
	override def clear() = itemsPointer.clear()
}
