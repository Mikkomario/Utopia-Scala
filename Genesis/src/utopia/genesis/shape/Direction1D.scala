package utopia.genesis.shape

import utopia.genesis.shape.shape2D.Direction2D
import utopia.genesis.util.{Scalable, Signed}

/**
  * A common trait for one dimensional directions (positive and negative)
  * @author Mikko Hilpinen
  * @since 17.4.2020, v2.3
  */
sealed trait Direction1D
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @return Whether this direction is considered positive
	  */
	def isPositive: Boolean
	
	/**
	  * @return A modifier applied to double numbers that have this direction
	  */
	def signModifier: Short
	
	
	// OTHER	----------------------------
	
	/**
	  * @param i An integer
	  * @return 'i' length to this direction
	  */
	def *(i: Int) = if (isPositive) i else -i
	
	/**
	  * @param d A double
	  * @return 'd' length to this direction
	  */
	def *(d: Double) = if (isPositive) d else -d
	
	/**
	  * @param s A scalable instance
	  * @tparam S2 Repr
	  * @tparam S Original
	  * @return 's' length to this direction
	  */
	def *[S2, S <: Scalable[S2]](s: S) = if (isPositive) s.repr else s * -1.0
	
	/**
	  * @param i A length
	  * @return 'i' length (absolute) to this direction
	  */
	def apply(i: Int) = this * math.abs(i)
	
	/**
	  * @param d A length
	  * @return 'd' length (absolute) to this direction
	  */
	def apply(d: Double) = this * math.abs(d)
	
	/**
	  * @param s A scalable length
	  * @tparam S2 Repr
	  * @tparam S Original
	  * @return 's' length (absolute) to this direction
	  */
	def apply[S2, S <: Scalable[S2] with Signed[S2]](s: S) = if (s.isPositive == isPositive) s.repr else s * -1.0
	
	/**
	  * @param axis Targeted axis
	  * @return A 2D version of this direction
	  */
	def along(axis: Axis2D) = Direction2D(axis, this)
}

object Direction1D
{
	/**
	  * Positive / forward / more direction
	  */
	case object Positive extends Direction1D
	{
		override val isPositive = true
		
		override val signModifier = 1
	}
	
	/**
	  * Negative / backward / less direction
	  */
	case object Negative extends Direction1D
	{
		override val isPositive = false
		
		override val signModifier = -1
	}
	
	/**
	  * All directions [Positive, Negative]
	  */
	val values = Vector(Positive, Negative)
	
	/**
	  * @return Positive direction
	  */
	def forward = Positive
	
	/**
	  * @return Negative direction
	  */
	def backward = Negative
	
	/**
	  * @return Positive direction
	  */
	def increase = Positive
	
	/**
	  * @return Negative direction
	  */
	def decrease = Negative
	
	/**
	  * Converts a boolean value to a direction
	  * @param isPositive Boolean value that represents direction sign (true = positive)
	  * @return A direction matching the specified boolean
	  */
	def matching(isPositive: Boolean) = if (isPositive) Positive else Negative
}