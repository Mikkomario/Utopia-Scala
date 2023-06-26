package utopia.flow.collection.mutable.iterator

object BottomToTopIterator
{
	/**
	  * Creates a new iterator that returns leaves before the rest of the branches
	  * @param top The topmost element in the tree-like structure
	  * @param goDeeper A function that returns the lower level elements from a tree node.
	  *
	  *                 Must return zero elements for the leaf nodes.
	  *                 If this function never returns 0 elements, this iterator will go into an infinite loop,
	  *                 trying to find the leaves.
	  *
	  * @tparam A Type of elements within the tree-like structure
	  * @return A new iterator
	  */
	def apply[A](top: A)(goDeeper: A => IterableOnce[A]) = new BottomToTopIterator[A](top)(goDeeper)
}

/**
  * An iterator intended for tree-like structures, which returns the leaves before the rest of the branches,
  * iterating a tree from the top to bottom. Doesn't necessarily start with the furthermost leaf but with the
  * first one available. Iterates one branch at a time, switching branches only at intersections.
  * @author Mikko Hilpinen
  * @since 26.6.2023, v2.2
  */
class BottomToTopIterator[A](top: A)(goDeeper: A => IterableOnce[A]) extends Iterator[A]
{
	// ATTRIBUTES   -----------------------------
	
	private var queuedSources = Vector(top -> goDeeper(top).iterator)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def hasNext: Boolean = queuedSources.nonEmpty
	
	override def next(): A = {
		val initialBottom = queuedSources.last
		// Case: There are more branches available => Goes to the bottom of the first available branch
		if (initialBottom._2.hasNext) {
			var nextNode = {
				val n = initialBottom._2.next()
				n -> goDeeper(n).iterator
			}
			// Moves forward as long as the branch continues
			while (nextNode._2.hasNext) {
				// Remembers the non-leaf nodes for future iterations
				queuedSources :+= nextNode
				val n = nextNode._2.next()
				val nextIter = goDeeper(n).iterator
				nextNode = n -> nextIter
			}
			// Returns the leaf
			nextNode._1
		}
		// Case: The current branch is fully traversed => Returns it
		else {
			// Moves to the next higher level for the next iteration
			queuedSources = queuedSources.dropRight(1)
			initialBottom._1
		}
	}
}
