package utopia.genesis.shape.shape2D

/**
* Classes extending this trait can be treated as continuous 2-dimensional areas
* @author Mikko Hilpinen
* @since 21.11.2018
**/
trait Area2D
{
    /**
     * Whether this area contains the specified 2D point
     */
	def contains(point: Point): Boolean
}