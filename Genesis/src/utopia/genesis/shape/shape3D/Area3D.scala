package utopia.genesis.shape.shape3D

import utopia.genesis.shape.Vector3D

/**
* Classes conforming to this trait are 3-dimensional continuous areas
* @author Mikko Hilpinen
* @since 21.11.2018
**/
trait Area3D
{
    /**
     * Whether this area contains the specified 3D point
     */
	def contains(point: Vector3D): Boolean
}