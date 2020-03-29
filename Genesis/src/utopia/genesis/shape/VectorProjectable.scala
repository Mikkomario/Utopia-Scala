package utopia.genesis.shape

import utopia.genesis.shape.Axis._

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
	def projectedOver(vector: Vector3D): Projection
	
	
	// COMPUTED	-------------------
	
	/**
	  * @return The x-wise projection of this item (only has x-component)
	  */
	def xProjection = projectedOver(X)
	/**
	  * @return The y-wise projection of this item (only has y-component)
	  */
	def yProjection = projectedOver(Y)
	/**
	  * @return The z-wise projection of this item (only has z-component)
	  */
	def zProjection = projectedOver(Z)
	
	
	// OTHER	-------------------
	
	/**
	  * @param axis Target axis
	  * @return A projection of this item over specified axis
	  */
	def projectedOver(axis: Axis): Projection = projectedOver(axis.toUnitVector)
}
