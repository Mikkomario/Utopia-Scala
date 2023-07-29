package utopia.paradigm.shape.template

import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}

/**
  * These items can be projected over vectors
  * @author Mikko Hilpinen
  * @since Genesis 13.9.2019, v2.1+
  * @tparam P Type of vector projection result
  */
trait VectorProjectable[+P]
{
	// ABSTRACT	--------------------
	
	/**
	  * Projects this instance over specified vector
	  * @param vector Target vector
	  * @return This instance's projection over target vector
	  */
	def projectedOver[V <: DoubleVectorLike[V]](vector: V): P
	
	
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
	def projectedOver(axis: Axis): P = projectedOver(axis.unit)
}
