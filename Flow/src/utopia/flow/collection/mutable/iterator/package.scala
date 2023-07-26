package utopia.flow.collection.mutable

/**
  * @author Mikko Hilpinen
  * @since 26.7.2023, v2.2
  */
package object iterator
{
	@deprecated("Please convert to using EventfulIterator instead", "v2.2")
	type IteratorWithEvents[A] = EventfulIterator[A]
}
