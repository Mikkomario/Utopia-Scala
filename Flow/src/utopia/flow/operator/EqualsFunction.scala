package utopia.flow.operator

import utopia.flow.operator.EqualsFunction.NotEqualsWrapper

import scala.language.implicitConversions

object EqualsFunction
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * The default equals function (i.e. ==), which works for any item
	  */
	val default: EqualsFunction[Any] = apply { _ == _ }
	
	/**
	  * A case-insensitive string equality function
	  */
	lazy val stringCaseInsensitive = apply[String] { _.equalsIgnoreCase(_) }
	/**
	  * A double equality function that rounds to 7th decimal place
	  */
	lazy val approxDouble = apply[Double] { (a, b) =>  a == b || (a - b).abs < 0.0000001 }
	
	
	// OTHER    ---------------------------
	
	/**
	  * Wraps a function into an EqualsFunction
	  * @param f A function to wrap
	  * @tparam A Type of compared item
	  * @return Function wrapper
	  */
	implicit def apply[A](f: (A, A) => Boolean): EqualsFunction[A] = new EqualsFunctionWrapper[A](f)
	
	/**
	  * Creates a new equals function that maps values before comparing them
	  * @param f A mapping function that yields a comparable value
	  * @param e Equals function used for comparing mapping results
	  * @tparam A Type of original item
	  * @tparam B Type of map result
	  * @return An equals function that compares items of type A by mapping them to type B
	  */
	def by[A, B](f: A => B)(implicit e: EqualsFunction[B]) = apply[A] { (a, b) => e(f(a), f(b)) }
	
	
	// NESTED   ---------------------------
	
	private class EqualsFunctionWrapper[-A](f: (A, A) => Boolean) extends EqualsFunction[A]
	{
		override def apply(a: A, b: A) = f(a, b)
	}
	
	private class NotEqualsWrapper[-A](equals: EqualsFunction[A]) extends EqualsFunction[A]
	{
		override def apply(a: A, b: A) = !equals(a, b)
	}
}

/**
  * Common trait for equality (or approximate equality) tests
  * @author Mikko Hilpinen
  * @since 1.8.2022, v1.16
  */
trait EqualsFunction[-A]
{
	// ABSTRACT -------------------------
	
	/**
	  * Tests item equality
	  * @param a First item
	  * @param b Second item
	  * @return Whether the two items may be considered equal
	  */
	def apply(a: A, b: A): Boolean
	
	
	// COMPUTED    ----------------------
	
	/**
	  * @return An inverted (i.e. "not equals") -version of this function
	  */
	def unary_! = inverted
	/**
	  * @return An inverted (i.e. "not equals") -version of this function
	  */
	def inverted: EqualsFunction[A] = new NotEqualsWrapper[A](this)
	
	
	// OTHER    -------------------------
	
	/**
	  * Tests whether the two items are NOT equal
	  * @param a First item
	  * @param b Second item
	  * @return Whether the two items are not equal
	  */
	def not(a: A, b: A) = !apply(a, b)
	
	/**
	  * @param other Another equals function
	  * @tparam B Type of compared types in that function
	  * @return A combination of these functions where both must return true
	  */
	def &&[B <: A](other: EqualsFunction[B]) =
		EqualsFunction[B] { (a, b) => apply(a, b) && other(a, b) }
	/**
	  * @param other Another equals function
	  * @tparam B Type of compared types in that function
	  * @return A combination of these functions where only one must return true
	  */
	def ||[B <: A](other: EqualsFunction[B]) =
		EqualsFunction[B] { (a, b) => apply(a, b) || other(a, b) }
}
