package utopia.genesis.shape.template

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.Axis.{X, Y, Z}
import utopia.genesis.shape.{Axis, Axis2D}

/**
  * Trait for items which can be split along different dimensions (which are represented by axes)
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
trait Dimensional[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @return The X, Y, Z ... dimensions of this vectorlike instance. No specific length required, however.
	  */
	def dimensions: Vector[A]
	
	/**
	  * @return A value with length of zero
	  */
	protected def zeroDimension: A
	
	
	// OTHER	--------------------
	
	/**
	  * @param axis Target axis
	  * @return This instance's component along specified axis
	  */
	def along(axis: Axis) = dimensions.getOrElse(indexForAxis(axis), zeroDimension)
	
	/**
	  * @param axis Target axis
	  * @return This instance's component perpendicular to targeted axis
	  */
	def perpendicularTo(axis: Axis2D) = along(axis.perpendicular)
	
	/**
	  * @param axis Target axis
	  * @return Index in the dimensions array for the specified axis
	  */
	def indexForAxis(axis: Axis) = axis match
	{
		case X => 0
		case Y => 1
		case Z => 2
	}
}
