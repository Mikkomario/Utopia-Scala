package utopia.paradigm.shape.shape2d.insets

import utopia.paradigm.enumeration.Direction2D

object Sides
{
	// COMPUTED --------------------------
	
	/**
	  * @tparam A Type of the sides, when specified
	  * @return A factory for constructing optional sides
	  */
	def optional[A] = new SpecificSidesFactory[Option[A]](None)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param zero Zero length value
	  * @tparam A Type of side lengths
	  * @return A factory for constructing new sets of sides
	  */
	def apply[A](zero: A) = new SpecificSidesFactory[A](zero)
	
	
	// NESTED   --------------------------
	
	class SpecificSidesFactory[A](zeroLength: A) extends SidesFactory[A, Sides[A]]
	{
		// IMPLEMENTED  ------------------
		
		override def withSides(sides: Map[Direction2D, A]): Sides[A] = _Sides(sides, zeroLength)
		
		
		// OTHER    ----------------------
		
		def apply(sides: Map[Direction2D, A]) = withSides(sides)
	}
	
	private case class _Sides[A](sides: Map[Direction2D, A], zeroLength: A) extends Sides[A]
}

/**
  * A basic version of the SidesLike trait
  * @author Mikko Hilpinen
  * @since 16/01/2024, v1.5
  */
trait Sides[A] extends SidesLike[A, Sides[A]]
{
	// IMPLEMENTED  ----------------------
	
	override def self: Sides[A] = this
	
	override protected def withSides(sides: Map[Direction2D, A]): Sides[A] = Sides(zeroLength)(sides)
}
