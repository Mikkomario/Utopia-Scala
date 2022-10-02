package utopia.flow.collection.mutable.iterator

object FoldingIterator
{
	/**
	  * Creates a new folding iterator
	  * @param start The starting value (first next() result)
	  * @param source Source material iterator
	  * @param fold The folding function - Accepts last result and next source iterator value and produces the
	  *             output value
	  * @tparam A Type of input (source) items
	  * @tparam V Type of output (fold function) values
	  * @return A new folding iterator
	  */
	def apply[A, V](start: V, source: Iterator[A])(fold: (V, A) => V) =
		new FoldingIterator[A, V](start, source)(fold)
	
	/**
	  * Creates a "reducing" iterator, which combines returned values
	  * @param source The source iterator
	  * @param reduce A reduce function that accepts the last result and a value from the source iterator and returns
	  *               their combination.
	  * @tparam A Type of source items / output values
	  * @return A new reducing iterator
	  */
	def reduce[A](source: Iterator[A])(reduce: (A, A) => A) = {
		if (source.hasNext)
			apply(source.next(), source)(reduce)
		else
			Iterator.empty
	}
}

/**
  * An iterator where items are based on folded items from another iterator
  * @author Mikko Hilpinen
  * @since 31.7.2022, v1.16
  */
class FoldingIterator[-A, V](start: V, source: Iterator[A])(f: (V, A) => V) extends Iterator[V]
{
	// ATTRIBUTES   -----------------------------
	
	// False initially, true after first call to next()
	private var startConsumed = false
	// The first parameter of the next fold function call
	private var nextOrigin = start
	
	
	// IMPLEMENTED  ----------------------------
	
	override def hasNext = !startConsumed || source.hasNext
	
	override def next() = {
		// Case: Folding source iterator values
		if (startConsumed) {
			val result = f(nextOrigin, source.next())
			// Stores the fold result to use in the next calculation
			nextOrigin = result
			result
		}
		// Case: Yields the starting value
		else {
			startConsumed = true
			start
		}
	}
}
