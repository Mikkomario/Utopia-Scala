package utopia.paradigm.shape.shape3d

/**
* Classes conforming to this trait are 3-dimensional continuous areas
* @author Mikko Hilpinen
* @since Genesis 21.11.2018
**/
trait Area3D
{
    /**
     * Whether this area contains the specified 3D point
     */
	def contains(point: Vector3D): Boolean
}