package utopia.conflict.collision

import utopia.genesis.shape.Vector3D
import utopia.genesis.shape.shape2D._
import utopia.genesis.util.Extensions._
import utopia.conflict.collision.Extensions._

object CollisionShape
{
    /**
     * Wraps a polygon into a collision shape
     */
    def apply(polygon: Polygonic) = new CollisionShape(polygon.convexParts, Vector(), 12)
    
    /**
     * Wraps a circle into a collision shape with the specified precision
     * @param circle the circle that is wrapped
     * @param circleToPolygonEdges the amount of edges used when approximating a circle or an ellipsoid 
 	 * with a polygon
     */
    def apply(circle: Circle, circleToPolygonEdges: Int = 12) = new CollisionShape(Vector(), Vector(circle), circleToPolygonEdges)
}

/**
 * Collision shapes are used when testing collisions between objects. The shapes may consist of 
 * polygons and / or circles. A bounding box is also provided
 * @author Mikko Hilpinen
 * @since 1.8.2017
 * @param convexPolygons The convex polygon (parts) that form this shape, or parts of it. An empty vector if 
 * the shape only consists of circles
 * @param circles The circle(s) that are part of this collision shape. An empty vector if the shape only 
 * consists of a polygon / polygons
 * @param circleToPolygonEdges the amount of edges used when approximating a circle or an ellipsoid 
 * with a polygon
 */
case class CollisionShape(convexPolygons: Vector[Polygonic], circles: Vector[Circle],
                          circleToPolygonEdges: Int) extends TransformProjectable[CollisionShape]
{
    // ATTRIBUTES    -------------------------
    
    /**
     * A bounding box around the whole shape. It is recommended that a bounding box is checked 
     * for multipart shapes when performing complex operations.
     */
    lazy val bounds = Bounds.around(convexPolygons.map { _.bounds } ++ circles.map { _.bounds })
    
    
    // COMPUTED PROPERTIES    ----------------
    
    /**
     * Whether this shape consists of more than a single part. A bounding box should be used for 
     * multipart shapes
     */
    def isMultiPart = circles.size + convexPolygons.size > 1
    
    /**
     * Converts the shape circle portions to polygons using as many edges as defined in 
     * <i>circleToPolygonEdges</i>
     */
    def circlesAsPolygons = circles.map { _.toPolygon(circleToPolygonEdges) }
    
    
    // IMPLEMENTED METHODS    ----------------
    
    override def transformedWith(transformation: Transformation) =
    {
        val transformedPolygons = convexPolygons.map { transformation(_) }
        
        // Some transformations allow the circles to retain their shape while others will not
        if ((transformation.shear ~== Vector3D.zero) && (transformation.scaling.x ~== transformation.scaling.y))
        {
            val transformedCircles = circles.map { original => 
                    Circle(transformation(original.origin), original.radius * transformation.scaling.x) }
            new CollisionShape(transformedPolygons, transformedCircles, circleToPolygonEdges)
        }
        else 
        {
            val transformedCirclePolygons = circlesAsPolygons.map { _.transformedWith(transformation) }
            new CollisionShape(transformedPolygons ++ transformedCirclePolygons, Vector(), circleToPolygonEdges)
        }
    }
    
    
    // OTHER METHODS    --------------------
    
    /**
     * Makes a collision check between the two shapes. Returns collision data if there is a collision. 
     * Doesn't always check all of the parts of the shapes if has found a collision between other parts already
     */
    def checkCollisionWith(other: CollisionShape) = 
    {
        // For multipart collisionShapes, bounds must be in collision for the check to continue
        if ((!isMultiPart && !other.isMultiPart) || checkBoundsCollisionWith(other).isDefined)
        {
            // Check order:
            // 1) Polygons
            // 2) Circles
            // 3) Polygons to circles
            // 4) Circles to polygons
            checkPolygonCollisionWith(other).orElse(
                    checkCircleCollisionWith(other)).orElse(
                    checkPolygonToCircleCollisionWith(other)).orElse(
                    checkCircleToPolygonCollisionWith(other))
        }
        else
            None
    }
    
    private def checkBoundsCollisionWith(other: CollisionShape) = bounds.checkCollisionWith(other.bounds)
    
    // TODO: Instead of combining the collisions, one could filter only those which push the shapes apart
    private def checkPolygonCollisionWith(other: CollisionShape) = convexPolygons.flatMap { 
            myPolygon => other.convexPolygons.flatMap { 
            myPolygon.checkCollisionWithConvex(_) } }.reduceOption { _ + _ }
            
    private def checkCircleCollisionWith(other: CollisionShape) = circles.flatMap { 
            myCircle => other.circles.flatMap { myCircle.checkCollisionWith(_) } }.reduceOption { _ + _ }
            
    private def checkPolygonToCircleCollisionWith(other: CollisionShape) = convexPolygons.flatMap { 
            myPolygon => other.circles.flatMap { myPolygon.checkCollisionWith(_) } }.reduceOption { _ + _ }
            
    private def checkCircleToPolygonCollisionWith(other: CollisionShape) = 
            other.checkPolygonToCircleCollisionWith(this).map { -_ }
}