package utopia.flow.operator

import scala.language.reflectiveCalls

object MaybeEmpty
{
	// TYPES    -----------------------------
	
	/**
	  * A structural type common for items which have an .isEmpty -property
	  */
	type HasIsEmpty = { def isEmpty: Boolean }
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param item An item which may be empty
	  * @tparam A Type of that item
	  * @return That item wrapped as a MaybeEmpty
	  */
	def apply[A <: HasIsEmpty](item: A): MaybeEmpty[A] = new MaybeEmptyWrapper[A](item)
	
	
	// NESTED   ----------------------------
	
	class MaybeEmptyWrapper[+A <: HasIsEmpty](wrapped: A) extends MaybeEmpty[A]
	{
		override def repr = wrapped
		override def isEmpty = wrapped.isEmpty
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
	def repr: Repr
	
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
	def notEmpty = if (isEmpty) None else Some(repr)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param default An item to return in case this one is empty (call-by-name)
	  * @tparam B Type of the default result
	  * @return This if not empty, otherwise the default
	  */
	def nonEmptyOrElse[B >: Repr](default: => B) = if (isEmpty) default else repr
	
	/**
	  * @param f A mapping function to apply for non-empty items
	  * @tparam B Type of mapping result
	  * @return A mapped copy of this item, if this item was not empty.
	  *         Otherwise returns this item.
	  */
	def mapIfNotEmpty[B >: Repr](f: Repr => B) = if (isEmpty) repr else f(repr)
}
