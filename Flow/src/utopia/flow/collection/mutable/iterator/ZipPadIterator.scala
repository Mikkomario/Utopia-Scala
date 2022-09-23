package utopia.flow.collection.mutable.iterator

object ZipPadIterator
{
	/**
	  * Creates a new iterator that zips all contents of the two specified iterators,
	  * padding one of them where necessary.
	  * The source iterators shouldn't be used by other instances afterwards.
	  * @param first The first source iterator
	  * @param second The second source iterator
	  * @param firstPadding The padding instance to use if items from the first source iterator end (call-by-name)
	  * @param secondPadding The padding instance to use if items from the second source iterator end (call-by-name)
	  * @tparam A Type of items in the first source iterator
	  * @tparam B Type of items in the second source iterator
	  * @return A new iterator that zips and possibly pads the contents of the source iterators
	  */
	def apply[A, B](first: Iterator[A], second: Iterator[B], firstPadding: => A, secondPadding: => B) =
		new ZipPadIterator[A, B](first, second, firstPadding, secondPadding)
	
	/**
	  * Creates a new iterator that merges all contents of the two specified iterators,
	  * padding one of them where necessary.
	  * The source iterators shouldn't be used by other instances afterwards.
	  * @param first The first source iterator
	  * @param second The second source iterator
	  * @param firstPadding The padding instance to use if items from the first source iterator end (call-by-name)
	  * @param secondPadding The padding instance to use if items from the second source iterator end (call-by-name)
	  * @param merge A function that accepts an item from both of the source iterators and merges them
	  * @tparam A Type of items in the first source iterator
	  * @tparam B Type of items in the second source iterator
	  * @tparam R Type of merge result
	  * @return A new iterator that yields the merge results until both of the source iterators are depleted
	  */
	def merge[A, B, R](first: Iterator[A], second: Iterator[B], firstPadding: => A, secondPadding: => B)
	                  (merge: (A, B) => R) =
		apply(first, second, firstPadding, secondPadding).map { case (a, b) => merge(a, b) }
	
	/**
	  * Creates a new iterator that zips all contents of the two specified iterators,
	  * padding one of them where necessary.
	  * The source iterators shouldn't be used by other instances afterwards.
	  * @param first The first source iterator
	  * @param second The second source iterator
	  * @param padding The padding instance to use if items from one first source iterator end before the other (call-by-name)
	  * @tparam A Type of items in the source iterators
	  * @return A new iterator that zips and possibly pads the contents of the source iterators
	  */
	def apply[A](first: Iterator[A], second: Iterator[A], padding: => A): ZipPadIterator[A, A] =
		apply(first, second, padding, padding)
}

/**
  * An iterator that 'zips' all items from the two source iterators, padding one of them if necessary.
  * @author Mikko Hilpinen
  * @since 16.9.2022, v1.17
  */
class ZipPadIterator[+A, +B](first: Iterator[A], second: Iterator[B], firstPadding: => A, secondPadding: => B)
	extends Iterator[(A, B)]
{
	override def hasNext = first.hasNext || second.hasNext
	
	override def next() =
		first.nextOption().getOrElse(firstPadding) -> second.nextOption().getOrElse(secondPadding)
}
