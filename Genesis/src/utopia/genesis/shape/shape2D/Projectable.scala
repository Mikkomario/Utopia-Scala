package utopia.genesis.shape.shape2D

import utopia.genesis.util.Extensions._
import utopia.genesis.shape.Axis
import utopia.genesis.shape.Vector3D
import utopia.genesis.shape.shape2D.Projectable.PointOrdering

object Projectable
{
    private object PointOrdering extends Ordering[Point]
    {
        override def compare(v1: Point, v2: Point) =
        {
            if (v1.x < v2.x) { -1 }
            else if (v1.x > v2.x) { 1 }
            else
            {
                if (v1.y < v2.y) { -1 }
                else if (v1.y > v2.y) { 1 }
                else 0
            }
        }
    }
}

/**
 * This trait is extended by shapes that can be projected over a specified axis
 * @author Mikko Hilpinen
 * @since 18.7.2017
 */
trait Projectable
{
    // ABSTRACT METHODS    --------------------
    
    /**
     * Projects this shape, creating a line parallel to the provided axis
     */
    def projectedOver(axis: Vector3D): Line
    
    
    // COMPUTED -------------------------------
    
    /**
      * @return An ordering for points, when it comes to projections
      */
    implicit def pointOrdering: Ordering[Point] = PointOrdering
    
    
    // OTHER METHODS    -----------------------
    
    /**
      * Projects this shape, creating a line parallel to the provided axis
      */
    def projectedOver(axis: Axis): Line = projectedOver(axis.toUnitVector)
    
    /**
    * Calculates if / how much the projections of the two shapes overlap on the specified axis
    * @param other the other projectable shape
    * @param axis the axis along which the overlap is checked
    * @return the mtv for the specified axis, if there is overlap
    */
    def projectionOverlapWith(other: Projectable, axis: Vector3D) = 
    {
        val projection = orderedProjectionOver(axis)
        val otherProjection = other.orderedProjectionOver(axis)
        
        if (comparePoints(projection.end, otherProjection.start) <= 0 || 
                comparePoints(projection.start, otherProjection.end) >= 0)
            None
        else 
        {
            val forwardsMtv = (otherProjection.end - projection.start).toVector
            val backwardsMtv = (otherProjection.start - projection.end).toVector
            
            if (forwardsMtv.length < backwardsMtv.length) Some(forwardsMtv) else Some(backwardsMtv)
        }
    }
    
    /**
     * Calculates the minimum translation vector that would get the two projectable shapes out of 
     * a collision situation
     * @param other the other projectable instance
     * @param axes the axes along which the collision is checked
     * @return The minimum translation vector that gets the two shapes out of a collision situation 
     * or none if there is no collision
     */
    def collisionMtvWith(other: Projectable, axes: Iterable[Vector3D]) = 
    {
        // If there is collision, there must be overlap on each axis
        val mtvs = axes.mapOrFail { projectionOverlapWith(other, _) }
        
        if (mtvs.exists { _.nonEmpty })
        {
            // Finds the smallest possible translation vector
            Some(mtvs.get.minBy { _.length })
        }
        else
            None
    }
    
    /**
      * Checks whether a point lies in this object's projection on a certain axis
      * @param point A point
      * @param axis axis where the overlap is checked
      * @return Whether the projected point is contained within this object's projection when considering only the
      *         specified axis
      */
    def containsProjection(point: Point, axis: Vector3D) =
    {
        val pointProjection = point.toVector.projectedOver(axis).toPoint
        val myProjection = projectedOver(axis)
        
        // Checks whether point lies on the projection. Points at the edge do count
        comparePoints(myProjection.start, pointProjection) <= 0 && comparePoints(myProjection.end, pointProjection) >= 0
    }
    
    private def orderedProjectionOver(axis: Vector3D) = 
    {
        val projection = projectedOver(axis)
        if (comparePoints(projection.start, projection.end) <= 0) projection else projection.reverse
    }
    
    protected def comparePoints(v1: Point, v2: Point) =
    {
        if (v1.x < v2.x) { -1 }
        else if (v1.x > v2.x) { 1 }
        else
        {
            if (v1.y < v2.y) { -1 }
            else if (v1.y > v2.y) { 1 }
            else 0
        }
    }
}