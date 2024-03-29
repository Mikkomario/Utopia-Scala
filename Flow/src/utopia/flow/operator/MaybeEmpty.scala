package utopia.flow.operator

import scala.language.implicitConversions
import scala.language.reflectiveCalls

object MaybeEmpty
{
	// TYPES    -----------------------------
	
	/**
	  * A structural type common for items which have an .isEmpty -property
	  */
	type HasIsEmpty = { def isEmpty: Boolean }
	
	
	// IMPLICIT ----------------------------
	
	// Collections always have the capacity to be empty
	implicit def collectionMayBeEmpty[C <: Iterable[_]](coll: C): MaybeEmpty[C] =
		new MayBeEmptyCollectionWrapper[C](coll)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param item An item which may be empty
	  * @tparam A Type of that item
	  * @return That item wrapped as a MaybeEmpty
	  */
	def apply[A <: HasIsEmpty](item: A): MaybeEmpty[A] = new MaybeEmptyWrapper[A](item)(item.isEmpty)
	/**
	  * @param string A string
	  * @return A "MaybeEmpty" based on that string
	  */
	def apply(string: String): MaybeEmpty[String] = new MaybeEmptyWrapper[String](string)(string.isEmpty)
	
	
	// NESTED   ----------------------------
	
	class MaybeEmptyWrapper[+A](wrapped: A)(testIsEmpty: => Boolean) extends MaybeEmpty[A] {
		override def self = wrapped
		override def isEmpty = testIsEmpty
	}
	
	private class MayBeEmptyCollectionWrapper[+C <: Iterable[_]](wrapped: C) extends MaybeEmpty[C]
	{
		override def self: C = wrapped
		override def isEmpty: Boolean = wrapped.isEmpty
	}
}

/**
  * Common trait for items which have an empty and a non-empty state
  * @author Mikko Hilpinen
  * @since 30.11.2022, v2.0
  */
trait MaybeEmpty[+Repr] extends Any
{
	// ABSTRACT --------------------------------
	
	/**
	  * @return This instance
	  */
	def self: Repr
	
	/**
	  * @return Whether this item is empty
	  */
	def isEmpty: Boolean
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return True if this item is not empty
	  */
	def nonEmpty = !isEmpty
	
	/**
	  * @return Some(this) if not empty. None if empty.
	  */
	def notEmpty = if (isEmpty) None else Some(self)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param default An item to return in case this one is empty (call-by-name)
	  * @tparam B Type of the default result
	  * @return This if not empty, otherwise the default
	  */
	def nonEmptyOrElse[B >: Repr](default: => B) = if (isEmpty) default else self
	
	/**
	  * @param f A mapping function to apply for non-empty items
	  * @tparam B Type of mapping result
	  * @return A mapped copy of this item, if this item was not empty.
	  *         Otherwise returns this item.
	  */
	def mapIfNotEmpty[B >: Repr](f: Repr => B) = if (isEmpty) self else f(self)
}
