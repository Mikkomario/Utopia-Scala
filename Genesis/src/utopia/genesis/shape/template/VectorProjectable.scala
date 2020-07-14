package utopia.genesis.shape.template

import utopia.genesis.shape.Axis
import utopia.genesis.shape.Axis.{X, Y, Z}

/**
  * These items can be projected over vectors
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
trait VectorProjectable[+Projection]
{
	// ABSTRACT	--------------------
	
	/**
	  * Projects this instance over specified vector
	  * @param vector Target vector
	  * @return This instance's projection over target vector
	  */
	def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]): Projection
	
	
	// COMPUTED	-------------------
	
	/**
	  * @return The x-wise projection of this item (only has x-component)
	  */
	def xProjection = projectedOver(X.toUnitVector)
	/**
	  * @return The y-wise projection of this item (only has y-component)
	  */
	def yProjection = projectedOver(Y.toUnitVector)
	/**
	  * @return The z-wise projection of this item (only has z-component)
	  */
	def zProjection = projectedOver(Z.toUnitVector)
	
	
	// OTHER	-------------------
	
	/**
	  * @param axis Target axis
	  * @return A projection of this item over specified axis
	  */
	def projectedOver(axis: Axis): Projection = axis match
	{
		case X => xProjection
		case Y => yProjection
		case Z => zProjection
	}
}
