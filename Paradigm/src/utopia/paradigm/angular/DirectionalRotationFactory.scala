package utopia.paradigm.angular

import utopia.flow.operator.Signed

/**
  * Common trait for factory objects that are used for constructing directed rotation instances
  * @author Mikko Hilpinen
  * @since 11.11.2023, v1.5
  * @tparam R Type of rotations constructed
  */
trait DirectionalRotationFactory[D <: Signed[D], +R]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @param absolute The absolute rotation amount (>= 0)
	  * @param direction Direction of rotation
	  * @return A new directed rotation instance
	  */
	protected def _apply(absolute: Rotation, direction: D): R
	
	
	// OTHER    --------------------------
	
	/**
	  * @param amount    Amount of rotation to apply towards the specified direction
	  * @param direction Targeted direction
	  * @return A new directional rotation instance
	  */
	def apply(amount: Rotation, direction: D) = {
		// Case: Amount would be negative => Converts it into a positive value (and reverses direction)
		if (amount.isNegative)
			_apply(-amount, -direction)
		else
			_apply(amount, direction)
	}
	
	/**
	  * @param direction Targeted rotation direction
	  * @return A factory used for constructing rotations towards that direction
	  */
	def apply(direction: D) = new DirectedRotationFactory(direction)
	
	
	// NESTED   ---------------------
	
	class DirectedRotationFactory(direction: D) extends RotationFactory[R]
	{
		// ATTRIBUTES   -------------
		
		override lazy val zero = super.zero
		
		
		// IMPLEMENTED  -------------
		
		override def radians(rads: Double): R = apply(Rotation(rads))
		
		
		// OTHER    -----------------
		
		/**
		  * @param amount The amount of rotation to apply
		  * @return A rotation with the specified amount to the targeted direction
		  */
		def apply(amount: Rotation) = DirectionalRotationFactory.this.apply(amount, direction)
		
		/**
		  * Calculates the rotation between two angles
		  * @param start The start angle
		  * @param end   The end angle
		  * @return A rotation from start to end with specified direction
		  */
		def between(start: Angle, end: Angle) = {
			if (start == end)
				circle
			else {
				val rotationAmount = {
					if (direction.isPositive) {
						if (end > start) end.radians - start.radians else end.radians + math.Pi * 2 - start.radians
					}
					else if (start > end) start.radians - end.radians else start.radians + math.Pi * 2 - end.radians
				}
				radians(rotationAmount)
			}
		}
	}
}

