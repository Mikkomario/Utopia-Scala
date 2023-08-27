package utopia.paradigm.shape.shape2d.line

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.operator.HasLength
import utopia.paradigm.shape.template.FromDimensionsFactory
import utopia.paradigm.shape.template.vector.NumericVectorLike

import scala.math.Fractional.Implicits.infixFractionalOps

/**
  * Common trait for 2-dimensional lines that span the area between two two-dimensional points.
 * @author Mikko Hilpinen
 * @since 27.8.2023, v1.4
  * @tparam D Type of dimensions used in the endpoints
  * @tparam P Type of endpoints used
  * @tparam V Type of vector form used
  * @tparam VD Type of double-precision vector form used
  * @tparam Repr Type of this line
 */
trait LineLike[D, P <: NumericVectorLike[D, P, _], +V <: NumericVectorLike[D, V, VD], +VD, +Repr]
	extends HasInclusiveEnds[P] with HasLength // with Transformable[FromDoubles] // with HasBounds with Projectable
{
    // ABSTRACT ------------------------
    
    /**
      * @return Implicit numeric implementation for the used dimensions
      */
    implicit def n: Fractional[D]
    
    /**
      * @return Factory used for building copies of this line
      */
    protected def factory: LineFactoryLike[D, P, Repr]
    /**
      * @return Factory used for building vector forms of this line
      */
    protected def vectorFactory: FromDimensionsFactory[D, V]
    
    
    // COMPUTED ------------------------
    
    /**
      * The vector portion of this line (position information not included)
      */
    def vector = vectorFactory.from(end - start)
    
    /**
     * A function for calculating the y-coordinate on this line when the x-coordinate is known
     */
    def yForX: D => D = {
        val v = vector
        // y = kx + a
        // Where k = Vy / Vx where V is the vector format of this line
        // a is the y of this function at 0 x
        val k = v.y / v.x
        val a = start.y - k * start.x
        x: D => k * x + a
    }
    /**
     * A function for calculating the x-coordinate on this line when the y-coordinate is known
     */
    def xForY: D => D = {
        val v = vector
        val k = v.x / v.y
        val a = start.x - k * start.y
        y: D => k * y + a
    }
    
    
    // COMPUTED PROPERTIES    ----------
    
    /**
      * @return The direction of this line
      */
    def direction = vector.direction
    
    /**
      * The axes that are necessary to include when checking collisions for this line
      */
    def collisionAxes = Vector(vector.toDoublePrecision, vector.normal2D)
    
    /**
      * The center of the line segment
      */
    def center = (start + end) / n.fromInt(2)
    
    /**
     * This line with inverted / reversed direction
     */
    def reverse = factory(ends.reverse)
    
    
    // IMPLEMENTED METHODS    ----------
    
    override def length = vector.length
    
    // override def bounds = Bounds.between(start, end)
    
    // override def projectedOver(axis: Vector2D) = factory(start.projectedOver(axis), end.projectedOver(axis))
    
    
    // OTHER METHODS    ----------------
    
    /**
      * @param f A mapping function for both the start and end point of this line
      * @return A mapped copy of this line
      */
    def map(f: P => P) = factory(ends.map(f))
}