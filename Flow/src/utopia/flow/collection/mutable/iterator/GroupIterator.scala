package utopia.flow.collection.mutable.iterator

import scala.collection.immutable.VectorBuilder

object GroupIterator
{
	/**
	 * Creates a new grouping iterator
	 * @param source Source iterator
	 * @param group A grouping function
	 * @tparam A Type of source items
	 * @tparam G Type of group identifiers
	 * @return A new iterator
	 */
	def apply[A, G](source: Iterator[A])(group: A => G) = new GroupIterator[A, G](source)(group)
}

/**
 * An iterator that allows grouped item handling. Groups consecutive items based on a grouping function and
 * returns them as a group.
 * @author Mikko Hilpinen
 * @since 28.3.2021, v1.9
 */
class GroupIterator[A, G](source: Iterator[A])(group: A => G) extends Iterator[(G, Vector[A])]
{
	// ATTRIBUTES   ----------------------------
	
	// Initially empty, after first next() contains the head of the group
	private var polled: Option[(A, G)] = None
	
	
	// IMPLEMENTED  ----------------------------
	
	override def hasNext = source.hasNext
	
	override def next() =
	{
		// Starts the group by picking the first item
		val (firstItem, currentGroup) = polled.getOrElse { nextItem() }
		
		val groupBuilder = new VectorBuilder[A]()
		groupBuilder += firstItem
		
		// Continues to poll the items until a different group is found
		var last = _nextOption()
		while (last.exists { _._2 == currentGroup })
		{
			groupBuilder += last.get._1
			last = _nextOption()
		}
		
		// Remembers the pre-polled result for the next next() call
		polled = last
		
		// Returns the group and the collected items
		currentGroup -> groupBuilder.result()
	}
	
	
	// OTHER    ------------------------------
	
	private def _nextOption(): Option[(A, G)] = if (source.hasNext) Some(nextItem()) else None
	
	private def nextItem() =
	{
		val item = source.next()
		item -> group(item)
	}
}
