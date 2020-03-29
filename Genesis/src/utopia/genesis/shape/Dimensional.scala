package utopia.genesis.shape

import utopia.genesis.shape.Axis._

/**
  * Trait for items which can be split along different dimensions (which are represented by axes)
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
trait Dimensional[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param axis Target axis
	  * @return This instance's component along specified axis
	  */
	def along(axis: Axis): A
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return This instance's x-component
	  */
	def x = along(X)
	/**
	  * @return This instance's y-component
	  */
	def y = along(Y)
	/**
	  * @return This instance's z-component
	  */
	def z = along(Z)
	
	
	// OTHER	--------------------
	
	/**
	  * @param axis Target axis
	  * @return This instance's component perpendicular to targeted axis
	  */
	def perpendicularTo(axis: Axis2D) = along(axis.perpendicular)
}
