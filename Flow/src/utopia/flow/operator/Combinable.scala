package utopia.flow.operator

object Combinable
{
	implicit class Subtractable[+R, A <: Reversible[A]](val c: Combinable[R, A]) extends AnyVal
	{
		/**
		  * @param other Another item
		  * @return A subtraction between these two items
		  */
		def -(other: A): R = c + (-other)
	}
	
	implicit class Averaging[+R <: LinearScalable[R], -A](val c: Combinable[R, A]) extends AnyVal
	{
		/**
		  * @param other Another item
		  * @return The average between these two items
		  */
		def average(other: A) = (c + other) / 2.0
	}
}

/**
  * A common trait for items which can be combined together
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait Combinable[+Repr, -Addition] extends Any
{
	/**
	  * @param other An item to add to this one
	  * @return A combination of these two items
	  */
	def +(other: Addition): Repr
}
