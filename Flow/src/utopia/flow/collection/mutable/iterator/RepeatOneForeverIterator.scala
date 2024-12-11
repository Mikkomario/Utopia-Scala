package utopia.flow.collection.mutable.iterator

object RepeatOneForeverIterator
{
	/**
	  * @param elem The element to repeat (called lazily)
	  * @tparam A Type of the returned element
	  * @return A new iterator that infinitely yields the specified element (caching it at the first call to next())
	  */
	def apply[A](elem: => A) = new RepeatOneForeverIterator[A](elem)
}

/**
  * An iterator that lazily acquires the read value and then repeats it infinitely
  * @author Mikko Hilpinen
  * @since 26.11.2024, v2.5.1
  */
class RepeatOneForeverIterator[+A](getElem: => A) extends Iterator[A]
{
	// ATTRIBUTES   ------------------------
	
	private lazy val elem = getElem
	
	
	// IMPLEMENTED  -----------------------
	
	override def hasNext: Boolean = true
	override def next(): A = elem
}
