package utopia.flow.util

import utopia.flow.collection.immutable.range.NumericSpan

import scala.collection.Factory

/**
  * Adds new functions related to [[Range]]s and Spans
  * @author Mikko Hilpinen
  * @since 22.09.2024, v2.5
  */
object RangeExtensions
{
	implicit class RichRange(val range: Range) extends AnyVal
	{
		/**
		  * @return The first index that is outside of this range
		  */
		def exclusiveEnd = range match {
			case r: Range.Exclusive => r.end
			case r: Range.Inclusive => if (r.step > 0) r.end + 1 else r.end - 1
		}
		
		/**
		  * This function works like foldLeft, except that it stores each step (including the start) into a vector
		  * @param start   The starting step
		  * @param map     A function for calculating the next step, takes the previous result + the next item in this range
		  * @param factory A factory for final collection (implicit)
		  * @tparam B The type of steps
		  * @return All of the steps mapped into a collection
		  */
		def foldMapToVector[B](start: B)(map: (B, Int) => B)(implicit factory: Factory[B, Vector[B]]): Vector[B] = {
			val builder = factory.newBuilder
			var last = start
			builder += last
			
			range.foreach { item =>
				last = map(last, item)
				builder += last
			}
			
			builder.result()
		}
	}
	
	implicit class RichInclusiveRange(val range: Range.Inclusive) extends AnyVal
	{
		/**
		  * @param stepSize How much this range is advanced on each step (sign doesn't matter)
		  * @return An iterator that contains all smaller ranges within this range. The length of these ranges is
		  *         determined by the 'step' of this range, although the last returned range may be shorter.
		  */
		def subRangeIterator(stepSize: Int): Iterator[Range.Inclusive] =
		{
			val step = if (range.start < range.end) stepSize.abs else -stepSize.abs
			new RangeIterator(range.start, range.end, step)
		}
	}
	
	implicit class NumToSpan[N](val number: N)(implicit n: Numeric[N])
	{
		/**
		  * @param end Span end (inclusive)
		  * @return A span from this number to 'end'
		  */
		def spanTo(end: N) = NumericSpan(number, end)
		/**
		  * @param start Span start
		  * @return A span from 'start' to this number (inclusive)
		  */
		def spanFrom(start: N) = NumericSpan(start, number)
	}
	
	private class RangeIterator(start: Int, end: Int, by: Int) extends Iterator[Range.Inclusive]
	{
		// ATTRIBUTES   ----------------------
		
		private val minStep = if (by < 0) -1 else if (by > 0) 1 else 0
		
		private var lastEnd = start - minStep
		
		
		// IMPLEMENTED  ----------------------
		
		override def hasNext = lastEnd != end
		
		override def next() =
		{
			val start = lastEnd + minStep
			val defaultEnd = start + by
			val actualEnd = {
				if ((by < 0 && defaultEnd < end) || (by > 0 && defaultEnd > end))
					end
				else
					defaultEnd
			}
			lastEnd = actualEnd
			start to actualEnd
		}
	}
}
