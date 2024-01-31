package utopia.inception.util

import scala.language.implicitConversions

object Filter
{
	// IMPLICIT ----------------------
	
	implicit def apply[A](f: A => Boolean): Filter[A] = (item: A) => f(item)
	
	def functionAsFilter[T](f: T => Boolean): Filter[T] = (item: T) => f(item)
}

/**
 * Filters are used for filtering elements that are of importance to the user. Functions
 * usually work better in simple use cases but Filter instances may be used in more dynamic
 * solutions.
 * @author Mikko Hilpinen
 * @since 16.10.2016 (Rewritten: 5.4.2019, v2+)
 */
trait Filter[-T]
{
	// ABSTRACT	------------------
	
	/**
	 * Applies 'this' filter over the 'item'
	 * @param item the event that is being evaluated
	 * @return does the filter accept / include 'element'
	 */
	def apply(item: T): Boolean
	
	
	// OPERATORS	---------------
	
	/**
	  * @return A negation of this filter
	  */
	def unary_! : Filter[T] = NotFilter(this)
	
	/**
	  * @param other Another filter
	  * @tparam B Filtered item type
	  * @return A filter that accepts an item if any of these does
	  */
	def ||[B <: T](other: Filter[B]): Filter[B] = OrFilter(this, other)
	
	/**
	  * @param other Another filter
	  * @tparam B Filtered item type
	  * @return A filter that accepts an item if both of these do
	  */
	def &&[B <: T](other: Filter[B]): Filter[B] = AndFilter(this, other)
	
	
	// OTHER	-------------------
	
	/**
	  * @param other Another filter
	  * @param more Even more filters
	  * @tparam B Filtered type
	  * @return A new filter that accepts an item if any of these filters does
	  */
	def or[B <: T](other: Filter[B], more: Filter[B]*) = new OrFilter(Vector(this, other) ++ more)
	
	/**
	  * @param other Another filter
	  * @param more Even more filters
	  * @tparam B Filtered type
	  * @return A new filter that accepts an item only if all of these filters do
	  */
	def and[B <: T](other: Filter[B], more: Filter[B]*) = new AndFilter(Vector(this, other) ++ more)
}