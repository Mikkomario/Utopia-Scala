package utopia.flow.view.template

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.{End, Extreme}
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.operator.sign.Sign

/**
  * Common trait for views and collections (such as Pairs) that contain (exactly) two elements.
  * @author Mikko Hilpinen
  * @since 18.12.2023, v2.3
  * @tparam A Type of the items contained or viewed
  */
trait HasTwoSides[+A]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The first item in this pair. Associated with side First.
	  */
	def first: A
	/**
	  * @return The second item in this pair. Associated with side Last.
	  */
	def second: A
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return A tuple based on this pair
	  */
	def toTuple = first -> second
	
	/**
	  * @return An iterator that returns values in this pair, along with the sides on which those values appear.
	  *         Negative represents the left / first side, Positive represents the right / second side.
	  */
	def iteratorWithSides = End.values.iterator.map { side => apply(side) -> side }
	/**
	  * @return Copy of this pair zipped with the sides on which the items appear
	  */
	def zipWithSide = zip(End.values)
	
	/**
	  * @return Whether the two values in this pair are equal
	  */
	def isSymmetric(implicit eq: EqualsFunction[A] = EqualsFunction.default) = eq(first, second)
	/**
	  * @return Whether the two values in this pair are not equal
	  */
	def isAsymmetric(implicit eq: EqualsFunction[A] = EqualsFunction.default) = !isSymmetric
	
	def max[B >: A](implicit ord: Ordering[B]) = ord.max(first, second)
	def min[B >: A](implicit ord: Ordering[B]) = ord.min(first, second)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param side The targeted side
	  * @return The item of this pair from that side
	  */
	def apply(side: End) = side match {
		case First => first
		case Last => second
	}
	/**
	  * @param extreme The targeted extreme
	  * @param ord Implicit ordering applied
	  * @tparam B Type of the ordering used
	  * @return The more extreme of the two available items
	  */
	def apply[B >: A](extreme: Extreme)(implicit ord: Ordering[B]): A = extreme match {
		case Max => max[B]
		case Min => min[B]
	}
	/**
	  * @param sign The targeted sign, where Negative targets the smaller item and Positive targets the greater
	  * @param ord     Implicit ordering applied
	  * @tparam B Type of the ordering used
	  * @return The smaller or larger of the two available items
	  */
	def apply[B >: A](sign: Sign)(implicit ord: Ordering[B]): A = apply[B](sign.extreme)
	
	/**
	  * Calls the specified function for each value of this pair. Includes the side where that item appears.
	  * @param f A function that processes the items. Accepts the item and the side on which it appears.
	  * @tparam U Arbitrary function result type
	  */
	def foreachSide[U](f: (A, End) => U) = End.values.foreach { side => f(apply(side), side) }
	
	/**
	  * @param item An item
	  * @return The side (Negative for left / first, Positive for right / second) on which that item resides
	  *         in this pair. None if that item is not in this pair.
	  */
	def sideOf[B >: A](item: B): Option[End] =
		if (item == first) Some(First) else if (item == second) Some(Last) else None
	/**
	  * @param item An item
	  * @return The item opposite to the specified item.
	  *         None if the specified item didn't appear in this pair.
	  */
	def oppositeOf[B >: A](item: B) =
		if (item == first) Some(second) else if (item == second) Some(first) else None
	/**
	  * Finds the item opposite to one matching a condition.
	  * Works like find, except that this function returns the opposite item.
	  *
	  * I.e. if 'f' returns true for the first item, returns the second item;
	  * If 'f' returns false for the first item and true for the second item, returns the first item;
	  * If 'f' returns false for both items, returns None.
	  *
	  * @param f A find function for the targeted (not returned) item
	  * @return The item opposite to the item for which 'f' returned true.
	  *         None if 'f' returned false for both items.
	  */
	def oppositeToWhere(f: A => Boolean) =
		if (f(first)) Some(second) else if (f(second)) Some(first) else None
	
	
	/**
	  * @param other Another pair
	  * @param f A predicate that compares the values of these pairs
	  * @tparam B Type of values in the other pair
	  * @return Whether the specified predicate returns true for either side
	  */
	def existsWith[B](other: HasTwoSides[B])(f: (A, B) => Boolean) =
		f(first, other.first) || f(second, other.second)
	/**
	  * @param other Another pair
	  * @param f     A predicate that compares the values of these pairs
	  * @tparam B Type of values in the other pair
	  * @return Whether the specified predicate returns true for both sides
	  */
	def forallWith[B](other: HasTwoSides[B])(f: (A, B) => Boolean) =
		f(first, other.first) && f(second, other.second)
	/**
	  * Merges this pair with another pair, resulting in a pair containing the entries from both
	  * @param other Another pair
	  * @tparam B Type of items in the other pair
	  * @return A pair that combines the values of both of these pairs in tuples
	  */
	def zip[B](other: HasTwoSides[B]) = Pair((first, other.first), (second, other.second))
	
	/**
	  * Merges together the two values in this pair using a function.
	  * Works exactly like reduce.
	  * @param f A function that accepts the two values in this pair and yields a merge value
	  * @tparam B Function result type
	  * @return Function result
	  */
	def merge[B](f: (A, A) => B) = f(first, second)
	/**
	  * Maps the values in this pair and merges them together using another function.
	  * This yields the same result as calling .map(...) followed by .merge(...), but
	  * is more optimized, as no additional collection is constructed in between.
	  * @param map Value mapping function to apply
	  * @param merge A merge function to apply to mapped values
	  * @tparam B Type of mapping results
	  * @tparam R Type of the merge result
	  * @return Merge result based on the mapped values of this pair
	  */
	def mapAndMerge[B, R](map: A => B)(merge: (B, B) => R) = merge(map(first), map(second))
	
	/**
	  * @param e An equals function
	  * @return Whether the two values of this pair are equal when applying the specified function
	  */
	def isSymmetricWith(e: EqualsFunction[A]) = e(first, second)
	/**
	  * @param e An equals function
	  * @return Whether the two values of this pair are unequal when applying the specified function
	  */
	def isAsymmetricWith(e: EqualsFunction[A]) = e.not(first, second)
	/**
	  * @param f A mapping function
	  * @tparam B Type of mapping results
	  * @return Whether the values in this pair are symmetric (i.e. equal) after applying the specified mapping function
	  */
	def isSymmetricBy[B](f: A => B) = merge { f(_) == f(_) }
	/**
	  * @param f A mapping function
	  * @tparam B Type of mapping result
	  * @return Whether the values in this pair are asymmetric (i.e. not equal)
	  *         after applying the specified mapping function
	  */
	def isAsymmetricBy[B](f: A => B) = !isSymmetricBy(f)
	
	/**
	  * @param other Another pair
	  * @param eq Implicit equals function to use
	  * @tparam B Type of values in the other pair
	  * @return Whether these pairs are equal when using the specified function
	  */
	def ~==[B >: A](other: HasTwoSides[B])(implicit eq: EqualsFunction[B]) =
		eq(first, other.first) && eq(second, other.second)
	/**
	  * @param other Another pair
	  * @param eq    Implicit equals function to use
	  * @tparam B Type of values in the other pair
	  * @return Whether these pairs are not equal when using the specified function
	  */
	def !~==[B >: A](other: HasTwoSides[B])(implicit eq: EqualsFunction[B]) = !(this ~== other)
}
