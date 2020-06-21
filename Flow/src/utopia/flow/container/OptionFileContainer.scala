package utopia.flow.container

import java.nio.file.Path

import utopia.flow.datastructure.immutable.Value
import utopia.flow.parse.JsonParser

/**
  * A common parent class for containers which store 0 to 1 items
  * @author Mikko Hilpinen
  * @since 21.6.2020, v1.8
  * @param fileLocation Path to where the data is stored
  * @param jsonParser A parser that will be used for parsing read json data
  */
abstract class OptionFileContainer[A](fileLocation: Path)(implicit jsonParser: JsonParser) extends FileContainer[Option[A]](fileLocation)
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @param item An item to save
	  * @return A value representation of the item
	  */
	protected def itemToValue(item: A): Value
	
	
	// IMPLEMENTED	----------------------------
	
	override protected def toValue(item: Option[A]) = item.map(itemToValue).getOrElse(Value.empty)
	
	override protected def empty = None
}
