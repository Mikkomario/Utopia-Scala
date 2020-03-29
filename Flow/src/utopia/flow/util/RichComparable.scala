package utopia.flow.util

import scala.annotation.unchecked.uncheckedVariance

object RichComparable
{
    // Extension for existing comparables
    implicit class ExtendedComparable[T](val c: Comparable[T]) extends RichComparable[T]
    {
        def compareTo(other: T) = c.compareTo(other)
    }
    
    /**
     * Finds the smaller of the two provided values
     */
    def min[T <: Comparable[T]](a: T, b: T) = if (a > b) b else a
    
    /**
     * Finds the larger of the two values
     */
    def max[T <: Comparable[T]](a: T, b: T) = if (a < b) b else a
}

/**
* This is an extension trait for the comparable interface in Java
* @author Mikko Hilpinen
* @since 17.11.2018
**/
//noinspection ScalaUnnecessaryParentheses
trait RichComparable[-T] extends Comparable[T @uncheckedVariance]
{
    /**
     * Checks whether this item is larger than the specified item
     */
	def >(other: T) = compareTo(other) > 0
	
	/**
	 * Checks whether this item is smaller than the specified item
	 */
	def <(other: T) = compareTo(other) < 0
	
	/**
	 * Checks whether this item is equal or larger than the specified item
	 */
	def >=(other: T) = !(<(other))
	
	/**
	 * Checks whether this item is equal or smaller than the specified item
	 */
	def <=(other: T) = !(>(other))
	
	/**
	 * Checks whether this item may be considered equal with the specified item
	 */
	def isEqualTo(other: T) = compareTo(other) == 0
	
	/**
	 * Compares this item with another, but if they are equal, provides a backup result
	 * @param other another instance
	 * @param backUp a function for providing a backup result in case the first one doesn't
	 * separate the values
	 */
	def compareOr(other: T)(backUp: => Int) =
	{
	    val primary = compareTo(other)
	    if (primary == 0)
	        backUp
	    else
	        primary
	}
}