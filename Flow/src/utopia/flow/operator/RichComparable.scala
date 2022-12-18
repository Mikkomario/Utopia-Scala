package utopia.flow.operator

import scala.annotation.unchecked.uncheckedVariance

object RichComparable
{
	// IMPLICIT	--------------------------
	
    // Extension for existing comparables
    implicit class ExtendedComparable[T](val c: Comparable[T]) extends AnyVal with RichComparable[T]
    {
        def compareTo(other: T) = c.compareTo(other)
    }
	
	/*
	implicit class IndirectComparable[A, B](val c: A)(implicit f: A => Comparable[B]) extends RichComparable[B]
	{
		override def compareTo(o: B) = f(c).compareTo(o)
	}*/
	
	implicit class ComparableInt(val i: Int) extends AnyVal with RichComparable[Int]
	{
		override def compareTo(o: Int) = i - o
	}
	
	implicit class ComparableDouble(val d: Double) extends AnyVal with RichComparable[Double]
	{
		override def compareTo(o: Double) = if (d < o) -1 else if (d > o) 1 else 0
	}
	
	implicit class ExtendedRichComparable[A, C <: RichComparable[A] with A](val c: C) extends AnyVal
	{
		/**
		  * @param other Another item
		  * @return The smaller between these two items
		  */
		def min(other: A): A = if (c > other) other else c
		
		/**
		  * @param other Another item
		  * @return The larger between these to items
		  */
		def max(other: A): A = if (c < other) other else c
	}
	
	
	// OTHER	------------------------
    
    /**
     * Finds the smaller of the two provided values
     */
    def min[A <: Comparable[A]](a: A, b: A) = if (a > b) b else a
    
    /**
     * Finds the larger of the two values
     */
    def max[A <: Comparable[A]](a: A, b: A) = if (a < b) b else a
}

/**
* This is an extension trait for the comparable interface in Java
* @author Mikko Hilpinen
* @since 17.11.2018
**/
//noinspection ScalaUnnecessaryParentheses
trait RichComparable[-A] extends Any with Comparable[A @uncheckedVariance]
{
	// OTHER	-----------------------
	
    /**
     * Checks whether this item is larger than the specified item
     */
	def >(other: A) = compareTo(other) > 0
	
	/**
	 * Checks whether this item is smaller than the specified item
	 */
	def <(other: A) = compareTo(other) < 0
	
	/**
	 * Checks whether this item is equal or larger than the specified item
	 */
	def >=(other: A) = !(<(other))
	
	/**
	 * Checks whether this item is equal or smaller than the specified item
	 */
	def <=(other: A) = !(>(other))
	
	/**
	 * Checks whether this item may be considered equal with the specified item
	 */
	def isEqualTo(other: A) = compareTo(other) == 0
	
	/**
	 * Compares this item with another, but if they are equal, provides a backup result
	 * @param other another instance
	 * @param backUp a function for providing a backup result in case the first one doesn't
	 * separate the values
	 */
	def compareOr(other: A)(backUp: => Int) =
	{
	    val primary = compareTo(other)
	    if (primary == 0)
	        backUp
	    else
	        primary
	}
}

trait SelfComparable[Repr] extends Any with RichComparable[Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return This instance
	  */
	def self: Repr
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param other Another instance
	  * @return Smaller of these two instances
	  */
	def min(other: Repr): Repr = if (this > other) other else self
	/**
	  * @param other Another instance
	  * @return Larger of these two instances
	  */
	def max(other: Repr): Repr = if (this < other) other else self
}