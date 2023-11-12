package utopia.paradigm.angular

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{Sign, Signed}

/**
  * Common trait for factory objects that are used for constructing directed rotation instances
  * @author Mikko Hilpinen
  * @since 11.11.2023, v1.5
  * @tparam D Type of directions used by this factory
  * @tparam R Type of rotations constructed
  */
trait BidirectionalRotationFactory[D <: Signed[D], +R] extends DirectionalRotationFactory[D, R] with RotationFactory[R]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @param sign A sign
	  * @return A direction matching that sign
	  */
	protected def directionForSign(sign: Sign): D
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Factory that creates rotations towards the positive direction
	  */
	def positive = apply(directionForSign(Positive))
	/**
	  * @return Factory that creates rotations towards the negative direction
	  */
	def negative = apply(directionForSign(Negative))
	
	
	// IMPLEMENTED  ----------------------
	
	override def radians(rads: Double): R = apply(Rotation.radians(rads))
	
	
	// OTHER    --------------------------
	
	/**
	  * @param amount Rotation amount to apply
	  * @return Specified rotation amount applied towards the positive direction
	  */
	def apply(amount: Rotation): R = apply(amount, directionForSign(Positive))
	/**
	  * @param sign Targeted sign
	  * @return A factory used for constructing rotations towards the direction that matches the specified sign
	  */
	def apply(sign: Sign): DirectedRotationFactory = apply(directionForSign(sign))
}

