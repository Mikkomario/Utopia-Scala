package utopia.paradigm.shape.shape2d

import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.shape.shape2d.LineProjectable.PointOrdering
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.VectorProjectable

object LineProjectable
{
    private object PointOrdering extends Ordering[Point]
    {
        override def compare(v1: Point, v2: Point) = {
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
 * This trait is extended by shapes that can be projected over a specified axis, forming a double-precision line.
 * @author Mikko Hilpinen
 * @since Genesis 18.7.2017
 */
trait LineProjectable extends VectorProjectable[Line]
{
    // COMPUTED -------------------------------
    
    /**
      * @return An ordering for points, when it comes to projections
      */
    implicit def pointOrdering: Ordering[Point] = PointOrdering
    
    
    // OTHER METHODS    -----------------------
    
    /**
    * Calculates if / how much the projections of the two shapes overlap on the specified axis
    * @param other the other projectable shape
    * @param axis the axis along which the overlap is checked
    * @return the mtv for the specified axis, if there is overlap
    */
    def projectionOverlapWith(other: LineProjectable, axis: Vector2D) = {
        val projection = orderedProjectionOver(axis)
        val otherProjection = other.orderedProjectionOver(axis)
        
        if (comparePoints(projection.end, otherProjection.start) <= 0 ||
                comparePoints(projection.start, otherProjection.end) >= 0)
            None
        else {
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
    def collisionMtvWith(other: LineProjectable, axes: Iterable[Vector2D]) = {
        // If there is collision, there must be overlap on each axis
        val mtvs = axes.lazyMap { projectionOverlapWith(other, _) }
        if (mtvs.forall { _.isDefined })
            // Finds the smallest possible translation vector
            mtvs.iterator.flatten.minByOption { _.length }
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
    def containsProjection(point: VectorProjectable[HasDoubleDimensions], axis: Vector2D) = {
        val pointProjection = point.projectedOver(axis)
        val myProjection = projectedOver(axis)
        
        // Checks whether point lies on the projection. Points at the edge do count
        comparePoints(myProjection.start, pointProjection) <= 0 && comparePoints(myProjection.end, pointProjection) >= 0
    }
    
    private def orderedProjectionOver(axis: Vector2D) = {
        val projection = projectedOver(axis)
        if (comparePoints(projection.start, projection.end) <= 0) projection else projection.reverse
    }
    
    protected def comparePoints(v1: HasDoubleDimensions, v2: HasDoubleDimensions) = {
        if (v1.x < v2.x) { -1 }
        else if (v1.x > v2.x) { 1 }
        else {
            if (v1.y < v2.y) { -1 }
            else if (v1.y > v2.y) { 1 }
            else 0
        }
    }
}