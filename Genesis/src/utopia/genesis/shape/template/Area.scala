package utopia.genesis.shape.template

import utopia.genesis.shape.Vector3D

/**
 * Areas are able to specify whether they contain a specific coordinate point
 * @author Mikko Hilpinen
 * @since 19.2.2017
 */
@deprecated("(Will be) replaced with separate 2D and 3D implementations", "v1.1.2")
trait Area
{
    /**
     * Whether a set of 3D coordinates lies within this specific area
     */
    def contains(point: Vector3D): Boolean
    
    /**
     * Whether a point lies within this area when both are projected to 2D (x-y) plane
     */
    def contains2D(point: Vector3D): Boolean
}