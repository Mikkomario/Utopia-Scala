package utopia.flow.operator.filter

import utopia.flow.util.NotEmpty

import scala.language.implicitConversions

object Filter
{
	// IMPLICIT ----------------------
	
	implicit def apply[A](f: A => Boolean): Filter[A] = new FilterFunction[A](f)
	
	
	// NESTED   ---------------------
	
	private class FilterFunction[-A](f: A => Boolean) extends Filter[A]
	{
		override def apply(item: A): Boolean = f(item)
	}
}

/**
 * Function that accepts or rejects an input value
 * @author Mikko Hilpinen
 * @since Inception 16.10.2016 (Added to flow at 30.1.2024, v2.4)
 */
trait Filter[-A]
{
	// ABSTRACT	------------------
	
	/**
	 * Applies 'this' filter over the 'item'
	 * @param item the event that is being evaluated
	 * @return does the filter accept / include 'element'
	 */
	def apply(item: A): Boolean
	
	
	// OPERATORS	---------------
	
	/**
	  * @return A negation of this filter
	  */
	def unary_! : Filter[A] = NotFilter(this)
	
	/**
	  * @param other Another filter
	  * @tparam B Filtered item type
	  * @return A filter that accepts an item if any of these does
	  */
	def ||[B <: A](other: Filter[B]): Filter[B] = or(other)
	/**
	  * @param other Another filter
	  * @tparam B Filtered item type
	  * @return A filter that accepts an item if both of these do
	  */
	def &&[B <: A](other: Filter[B]): Filter[B] = and(other)
	
	
	// OTHER	-------------------
	
	/**
	  * @param other Another filter
	  * @tparam B Filtered item type
	  * @return A filter that accepts an item if any of these does
	  */
	def or[B <: A](other: Filter[B]): Filter[B] = OrFilter(this, other)
	/**
	  * @param other Another filter
	  * @param more Even more filters
	  * @tparam B Filtered type
	  * @return A new filter that accepts an item if any of these filters does
	  */
	def or[B <: A](other: Filter[B], more: Filter[B]*): Filter[B] = or(other +: more)
	/**
	  * @param filters Other filters that may be fulfilled
	  * @tparam B Filtered type
	  * @return A new filter that accepts an item if any of these filters does
	  */
	def or[B <: A](filters: IterableOnce[Filter[B]]): Filter[B] = NotEmpty(Seq.from(filters)) match {
		case Some(filters) => OrFilter(this +: filters)
		case None => this
	}
	/**
	  * @param other Another filter
	  * @tparam B Filtered item type
	  * @return A filter that accepts an item if both of these do
	  */
	def and[B <: A](other: Filter[B]): Filter[B] = AndFilter(this, other)
	/**
	  * @param other Another filter
	  * @param more Even more filters
	  * @tparam B Filtered type
	  * @return A new filter that accepts an item only if all of these filters do
	  */
	def and[B <: A](other: Filter[B], more: Filter[B]*): Filter[B] = and(other +: more)
	/**
	  * @param filters additional filters that must also much
	  * @tparam B Filtered type
	  * @return A new filter that accepts an item only if all of these filters do
	  */
	def and[B <: A](filters: IterableOnce[Filter[B]]): Filter[B] = NotEmpty(Seq.from(filters)) match {
		case Some(filters) => AndFilter(this +: filters)
		case None => this
	}
}