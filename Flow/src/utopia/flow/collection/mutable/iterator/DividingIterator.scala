package utopia.flow.collection.mutable.iterator

import utopia.flow.view.template.HasTwoSides

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable

object DividingIterator
{
	// OTHER    ----------------------------
	
	/**
	 * @param source Source collection to iterate
	 * @param f A mapping function applied to each element from 'source'.
	 *          Yields an item either to the right or to the left.
	 * @tparam I Type of items in the source collection
	 * @tparam L Type of left map results
	 * @tparam R Type of right map results
	 * @tparam C Lowest common type between L and R
	 * @return A new divided iterator for both the left and the right results
	 */
	def apply[I, L <: C, R <: C, C](source: IterableOnce[I])(f: I => Either[L, R]): DividingIterator[I, L, R, C] =
		new _DividingIterator[I, L, R, C](source.iterator)(f)
	/**
	 * @param source Source collection to iterate
	 * @param f A mapping function applied to each element from 'source'.
	 *          Yields n items that each fall either to the right or to the left.
	 * @tparam I Type of items in the source collection
	 * @tparam L Type of left map results
	 * @tparam R Type of right map results
	 * @tparam C Lowest common type between L and R
	 * @return A new divided iterator for both the left and the right results
	 */
	def flat[I, L <: C, R <: C, C](source: IterableOnce[I])
	                              (f: I => IterableOnce[Either[L, R]]): DividingIterator[I, L, R, C] =
		new FlatDividingIterator[I, L, R, C](source.iterator)(f)
	/**
	 * @param source Source collection to iterate
	 * @param f A function that receives a source collection item and yields either true or false.
	 * @tparam A Type of items in the source collection
	 * @return A new divided iterator that contains two sides:
	 *              1. Left: An iterator that yields items for which 'f' yielded false
	 *              1. Right: An iterator that yields items for which 'f' yielded true
	 */
	def by[A](source: IterableOnce[A])(f: A => Boolean): DividingIterator[A, A, A, A] =
		new DividingByIterator[A](source.iterator)(f)
		
	
	// NESTED   -----------------------------
	
	private class DividingByIterator[A](source: Iterator[A])(f: A => Boolean) extends DividingIterator[A, A, A, A]
	{
		// ATTRIBUTES   --------------------
		
		private val buffers = Map(false -> mutable.Queue.empty[A], true -> mutable.Queue.empty[A])
		
		override val left: Iterator[A] = new Side(right = false)
		override val right: Iterator[A] = new Side(right = true)
		
		
		// OTHER    -------------------------
		
		private def pollNext(right: Boolean, buffer: Boolean) = {
			var found: Option[A] = None
			while (found.isEmpty && source.hasNext) {
				val next = source.next()
				val side = f(next)
				
				if (buffer || side != right)
					buffers(side).enqueue(next)
				if (side == right)
					found = Some(next)
			}
			found
		}
		
		
		// NESTED   -------------------------
		
		private class Side(right: Boolean) extends Iterator[A]
		{
			// ATTRIBUTES   -----------------
			
			private val buffer = buffers(right)
			
			
			// IMPLEMENTED ------------------
			
			override def hasNext: Boolean = buffer.nonEmpty || pollNext(right, buffer = true).isDefined
			override def knownSize = {
				val kn = buffer.knownSize
				if (kn < 0 || source.hasNext)
					-1
				else
					kn
			}
			
			override def next(): A = {
				if (buffer.isEmpty)
					pollNext(right, buffer = false).get
				else
					buffer.dequeue()
			}
		}
	}
	
	private abstract class AbstractDividingIterator[-I, +L <: C, +R <: C, +C](source: Iterator[I])
		extends DividingIterator[I, L, R, C]
	{
		// ATTRIBUTES   --------------------
		
		/**
		 * Buffer used for storing unqueued left-side items
		 */
		protected val leftBuffer: mutable.Queue[L @uncheckedVariance] = mutable.Queue.empty[L]
		/**
		 * Buffer used for storing unqueued right-side items
		 */
		protected val rightBuffer: mutable.Queue[R @uncheckedVariance] = mutable.Queue.empty[R]
		
		override val left: Iterator[L] = new Side(leftBuffer, right = false)
		override val right: Iterator[R] = new Side(rightBuffer, right = true)
		
		
		// ABSTRACT ------------------------
		
		/**
		 * Processes the next item from the source collection, buffering the results.
		 * @param source The next source item
		 * @param lookingForRight Whether looking for entries for the right buffer.
		 *                        False if looking for entries for the left buffer.
		 * @return True if the targeted buffer is now non-empty.
		 */
		protected def bufferNext(source: I, lookingForRight: Boolean): Boolean
		
		
		// OTHER    ------------------------
		
		private def pollNext(right: Boolean) = {
			var found = false
			while (!found && source.hasNext) {
				found = bufferNext(source.next(), right)
			}
			found
		}
		
		
		// NESTED   ------------------------
		
		private class Side[A](buffer: mutable.Queue[A], right: Boolean) extends Iterator[A]
		{
			override def hasNext: Boolean = buffer.nonEmpty || pollNext(right)
			override def knownSize = {
				val kn = buffer.knownSize
				if (kn < 0 || source.hasNext)
					-1
				else
					kn
			}
			
			override def next(): A = {
				if (buffer.isEmpty)
					pollNext(right)
				buffer.dequeue()
			}
		}
	}
	
	private class FlatDividingIterator[-I, +L <: C, +R <: C, +C](source: Iterator[I])
	                                                            (f: I => IterableOnce[Either[L, R]])
		extends AbstractDividingIterator[I, L, R, C](source)
	{
		// IMPLEMENTED  --------------------
		
		override protected def bufferNext(source: I, lookingForRight: Boolean): Boolean = {
			var found = false
			f(source).iterator.foreach {
				case Left(l) =>
					leftBuffer.enqueue(l)
					if (!lookingForRight)
						found = true
				
				case Right(r) =>
					rightBuffer.enqueue(r)
					if (lookingForRight)
						found = true
			}
			found
		}
	}
	
	private class _DividingIterator[-I, +L <: C, +R <: C, +C](source: Iterator[I])(f: I => Either[L, R])
		extends AbstractDividingIterator[I, L, R, C](source)
	{
		override protected def bufferNext(source: I, lookingForRight: Boolean): Boolean = {
			f(source) match {
				case Left(l) =>
					leftBuffer.enqueue(l)
					!lookingForRight
			
				case Right(r) =>
					rightBuffer.enqueue(r)
					lookingForRight
			}
		}
	}
}

/**
 * An iterator that divides a source iterator into two: Left (1) and right (2).
 * @tparam I Type of items in the source collection
 * @tparam L Type of left map results
 * @tparam R Type of right map results
 * @author Mikko Hilpinen
 * @since 29.09.2026, v2.9
 */
trait DividingIterator[-I, +L <: C, +R <: C, +C] extends HasTwoSides[Iterator[C]]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return An iterator that yields the left side items
	 */
	def left: Iterator[L]
	/**
	 * @return An iterator that yields the right side items
	 */
	def right: Iterator[R]
	
	
	// IMPLEMENTED  ---------------------
	
	override def first: Iterator[C] = left
	override def second: Iterator[C] = right
}
