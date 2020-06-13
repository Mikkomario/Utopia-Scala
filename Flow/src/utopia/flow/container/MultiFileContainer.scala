package utopia.flow.container

import java.nio.file.Path

import utopia.flow.datastructure.immutable.Value
import utopia.flow.parse.JsonParser
import utopia.flow.generic.ValueConversions._

import scala.util.Try

/**
  * This type of container tracks multiple items, and stores them in a local file
  * @author Mikko Hilpinen
  * @since 13.6.2020, v1.8
  * @param fileLocation Location in the file system where this container's back up file should be located
  * @param jsonParser A parser used for handling json reading (implicit)
  * @tparam A Type of individual items stored in this container
  */
abstract class MultiFileContainer[A](fileLocation: Path)(implicit jsonParser: JsonParser)
	extends FileContainer[Vector[A]](fileLocation)
{
	// ABSTRACT --------------------------------
	
	/**
	  * @param item An item to save
	  * @return A value representation of the item
	  */
	protected def itemToValue(item: A): Value
	
	/**
	  * @param value A read value
	  * @return An item parsed from the value
	  */
	protected def itemFromValue(value: Value): Try[A]
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def toValue(item: Vector[A]) = item.map(itemToValue)
	
	override protected def fromValue(value: Value) = value.vector match
	{
		case Some(values) =>
			// Ignores parsing failures
			values.flatMap { itemFromValue(_).toOption }
		case None => empty
	}
	
	override protected def empty = Vector()
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param newItem Adds a new item to this container
	  */
	def add(newItem: A) = _current.update { _ :+ newItem }
	
	/**
	  * Removes an item from this container
	  * @param item An item to remove
	  */
	def remove(item: Any) = filterNot { _ == item }
	
	/**
	  * Filters the contents of this container
	  * @param f A filtering function
	  */
	def filter(f: A => Boolean) = _current.update { _.filter(f) }
	
	/**
	  * Filters the contents of this container
	  * @param f A filtering function that determines the items that are removed
	  */
	def filterNot(f: A => Boolean) = _current.update { _.filterNot(f) }
	
	/**
	  * Removes all content from this container
	  * @return Content in this container before removal
	  */
	def popAll() = getAndSet(empty)
}
