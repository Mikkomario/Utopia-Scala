package utopia.genesis.handling.template

import utopia.flow.collection.immutable.Empty
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile

/**
  * An abstract implementation of the Handler trait
  * @author Mikko Hilpinen
  * @since 30/01/2024, v4.0
  * @param initialItems Initially assigned items
  * @param log Logging implementation used in pointer-management
  */
abstract class AbstractHandler[A <: Handleable](initialItems: IterableOnce[A] = Empty)(implicit log: Logger)
	extends Handler[A]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A pointer that contains the currently attached items in this handler
	  */
	protected val itemsPointer = Volatile.eventful.seq[A]()
	
	
	// INITIAL CODE ------------------------
	
	// Attaches the initial items
	this ++= initialItems
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def items = itemsPointer.value
	
	override protected def addOneUnconditionally(item: A): Unit = itemsPointer.update { _ :+ item }
	override def removeWhere(f: A => Boolean): Unit = itemsPointer.update { _.filterNot(f) }
	
	override def clear() = itemsPointer.clear()
}
