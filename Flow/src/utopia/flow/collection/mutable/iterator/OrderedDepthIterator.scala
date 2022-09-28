package utopia.flow.collection.mutable.iterator

import scala.collection.immutable.VectorBuilder

object OrderedDepthIterator
{
	/**
	  * Creates a new ordered-depth-iterator
	  * @param firstLayer The first layer of items that will be accessed.
	  *                   Should be lazily initialized (like an iterator), if possible.
	  * @param goDeeper A function that opens the next layer behind a specific item.
	  *                 Called right before those items would be accessed.
	  *                 Still, it is preferred if this function opens the items in a lazy manner,
	  *                 as that will reduce the number of cached / buffered items.
	  * @tparam A Type of items that form each layer
	  * @return An iterator that fully traverses each layer of depth before moving to the next layer
	  */
	def apply[A](firstLayer: IterableOnce[A])(goDeeper: A => IterableOnce[A]) =
		new OrderedDepthIterator[A](firstLayer.iterator)(goDeeper)
}

/**
  * An iterator used for "flushing" through a recursive collection in an ordered manner.
  * This iterator fully iterates over a single "layer" of items before moving to the next "layer".
  * Suitable for recursive data structures such as trees and graphs,
  * in situations where one first wants to iterate over the closest nodes and only then the further nodes.
  * In such cases where ordering is not so important, this iterator is not as efficient,
  * as it initializes and buffers additional iterators during a layer's iteration.
  * @author Mikko Hilpinen
  * @since 28.9.2022, v2.0
  */
class OrderedDepthIterator[A](firstLayerSource: Iterator[A])(goDeeper: A => IterableOnce[A]) extends Iterator[A]
{
	// ATTRIBUTES   ----------------------
	
	private var currentLayerSource = firstLayerSource
	// Stores the current layer's iteration in memory in order to open the next level when it is reached
	private val currentLayerBuffer = new VectorBuilder[A]()
	
	
	// IMPLEMENTED  ---------------------
	
	override def hasNext = {
		// Case: Current layer has more items remaining => has at least one more item
		if (currentLayerSource.hasNext)
			true
		// Case: Current layer is finished => Starts the next layer and looks again
		else {
			startNextLayer()
			currentLayerSource.hasNext
		}
	}
	
	override def next() = {
		// Takes the next item. Moves between layers if necessary
		// (should not really happen here in normal use, as this may throw)
		val result = currentLayerSource.nextOption().getOrElse {
			startNextLayer()
			currentLayerSource.next()
		}
		// Remembers each iterated item in order to open the next layer later
		currentLayerBuffer += result
		result
	}
	
	
	// OTHER    -----------------------
	
	private def startNextLayer() = {
		currentLayerSource = currentLayerBuffer.result().iterator.flatMap(goDeeper)
		//noinspection ScalaUnusedExpression
		currentLayerBuffer.clear()
	}
}
