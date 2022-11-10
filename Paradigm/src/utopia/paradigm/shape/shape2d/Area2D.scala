package utopia.paradigm.shape.shape2d

import utopia.paradigm.shape.template.DoubleVectorLike

/**
* Classes extending this trait can be treated as continuous 2-dimensional areas
* @author Mikko Hilpinen
* @since Genesis 21.11.2018
**/
trait Area2D
{
    /**
     * Whether this area contains the specified 2D point
     */
	def contains[V <: DoubleVectorLike[V]](point: V): Boolean
}