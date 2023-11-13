package utopia.flow.operator.combine

import utopia.flow.operator.Reversible

object Combinable
{
	/**
	  * A type where instances of that type can be combined together
	  */
	type SelfCombinable[Repr] = Combinable[Repr, Repr]
	
	implicit class Averaging[-A, +R <: LinearScalable[R]](val c: Combinable[A, R]) extends AnyVal
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
trait Combinable[-Addition, +Repr] extends Any with Subtractable[Reversible[Addition], Repr]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param other An item to add to this one
	  * @return A combination of these two items
	  */
	def +(other: Addition): Repr
	
	
	// IMPLEMENTED    -----------------
	
	/**
	  * @param other An item to subtract from this one
	  * @return A subtracted copy of this item
	  */
	override def -(other: Reversible[Addition]) = this + (-other)
}
