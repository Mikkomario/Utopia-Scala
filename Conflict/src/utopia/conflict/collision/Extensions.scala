package utopia.conflict.collision

import utopia.genesis.shape.shape2D.{Circle, Line, Polygon, Polygonic}
import utopia.genesis.shape.{Angle, Vector3D}

/**
 * This object contains extensions that are used in the conflict project
 * @author Mikko Hilpinen
 * @since 13.7.2017
 */
object Extensions
{
    implicit class CollisionCircle(val c: Circle) extends AnyVal
    {
        /**
         * Checks if there's collision between two circles. Returns collision data if there is
         * a collision.
         */
        def checkCollisionWith(other: Circle) =
        {
            val mtv = c.collisionMtvWith(other)
            mtv.map { new Collision(_, c.circleIntersection(other)) }
        }
        
        /**
         * Converts this circle into a polygon with the specified amount of edges. The higher the
         * amount of edges, the more accurate the representation but the more taxing any operations
         * will be.
         */
        def toPolygon(edgeAmount: Int) = Polygon((
                for { i <- 0 until edgeAmount } yield c.origin + Vector3D.lenDir(c.radius,
                new Angle(math.Pi * 2 * i / edgeAmount))).toVector)
    }
    
    implicit class CollisionPolygon(val p: Polygonic) extends AnyVal
    {
        /**
          * Checks if there's collision between these two polygon instances. Returns collision data if
          * there is collision
          */
        def checkCollisionWith(other: Polygonic) =
        {
            if (p.isConvex && other.isConvex)
                checkCollisionWithConvex(other)
            else
            {
                val myParts = p.convexParts
                val otherParts = other.convexParts
            
                myParts.flatMap { myPart => otherParts.flatMap {myPart.checkCollisionWithConvex(_) } }.reduceOption { _ + _ }
            }
        }
    
        /**
          * Checks if there's collision between the two polygon instances. Returns collision data if
          * there is collision. <b>Only works with convex polygons</b>
          */
        def checkCollisionWithConvex(other: Polygonic) =
        {
            // Uses collision axes from both polygons, doesn't need to repeat parallel axes
            val mtv = p.collisionMtvWith(other)
            mtv.map { mtv => new Collision(mtv, collisionPoints(other, mtv)) }
        }
    
        /**
          * Checks if there's collision between this polygon and the provided circle shape. Returns
          * collision data if there is a collision.
          */
        def checkCollisionWith(circle: Circle) =
        {
            val mtv = p.collisionMtvWith(circle, p.collisionAxes :+ (p.center - circle.origin).toVector)
            mtv.map { mtv => new Collision(mtv, circle.simpleCollisionPoints(-mtv)) }
        }
    
        /**
          * Checks if there's collision between this polygon and a line segment. Returns collision data
          * if there is a collision
          */
        def checkCollisionWith(line: Line) =
        {
            val mtv = p.collisionMtvWith(line, p.collisionAxes ++ line.collisionAxes)
            mtv.map { mtv => new Collision(mtv, collisionPoints(line, mtv)) }
        }
    
        /**
          * Finds the collision points between two (colliding) <b>convex</b> polygons
          * @param other The other polygon
          * @param collisionNormal A normal for the collision plane, usually the minimum translation
          * vector for this polygon
          */
        def collisionPoints(other: Polygonic, collisionNormal: Vector3D) =
        {
            val c: CollisionPolygon = other
            edgeCollisionClip(c.collisionEdge(-collisionNormal), collisionNormal)
        }
    
        /*
		 * Finds the collision points between this polygon and a circle. NB: This operation is somewhat
		 * slow (O(n)) and should be used sparingly and only when a collision has already been recognised.
		 */
        // def collisionPoints(circle: Circle) = edges.flatMap { _.circleIntersection(circle) }
    
        /**
          * Finds the collision points between this polygon and a line when collision normal (mtv) is
          * already known
          * @param line any line
          * @param collisionNormal a normal to the collision, pointing from the collision area towards
          * this polygon (ie. The collision mtv for this polygon)
          */
        def collisionPoints(line: Line, collisionNormal: Vector3D) =
        {
            // The collision edge always starts at the point closer to the collision area
            // (= more perpendicular to the collision normal)
            val otherEdge =
            {
                if ((line.start.toVector dot collisionNormal).abs < (line.end.toVector dot collisionNormal).abs)
                    line
                else
                    line.reverse
            }
            edgeCollisionClip(otherEdge, collisionNormal)
        }
    
        /**
          * Clips the collision points from two collision edges when the collision normal (mtv) is known
          * @param otherCollisionEdge the collision edge of the other shape in the collision
          * @param collisionNormal a normal for the collision plane, from the collision area towards
          * this polygon instance (ie. the mtv for this polygon)
          */
        private def edgeCollisionClip(otherCollisionEdge: Line, collisionNormal: Vector3D) =
        {
            // Finds the remaining (own) collision edge
            val myCollisionEdge = collisionEdge(collisionNormal)
            
            // The reference edge is the one that is more perpendicular to the collision normal
            if ((myCollisionEdge.vector dot collisionNormal).abs <= (otherCollisionEdge.vector dot collisionNormal).abs)
                clipCollisionPoints(myCollisionEdge, otherCollisionEdge, collisionNormal)
            else
                clipCollisionPoints(otherCollisionEdge, myCollisionEdge, -collisionNormal)
        }
    
        // Use minimum translation vector as normal (points towards this polygon from the collision area)
        // Doesn't work for polygons with < 2 vertices (surprise)
        private def collisionEdge(collisionNormal: Vector3D) =
        {
            // Finds the vertex closest to the collision direction
            val c = p.corners
            val closestVertexIndex = c.indices.minBy { c(_).toVector dot collisionNormal }
            
            // Uses the edge that is more perpendicular to the collision normal
            val (s1, s2) = p.sidesFrom(closestVertexIndex)
    
            if ((s1.vector dot collisionNormal) < (s2.vector dot collisionNormal))
                s1
            else
                s2
        }
    }
    
    private def clipCollisionPoints(reference: Line, incident: Line, referenceNormal: Vector3D) =
    {
        // First clips the incident edge from both sides
        val clipped = incident.clipped(reference.start, reference.vector).flatMap { _.clipped(reference.end, -reference.vector) }
        
        if (clipped.isDefined)
        {
            // Also removes any points past the third side
            val origin = reference.start.toVector dot referenceNormal
            val startDistance = clipped.get.start.toVector.dot(referenceNormal) - origin
            val endDistance = clipped.get.end.toVector.dot(referenceNormal) - origin
            
            if (startDistance < 0 && endDistance < 0)
                Vector()
            else if (startDistance < 0)
                Vector(clipped.get.end)
            else if (endDistance < 0)
                Vector(clipped.get.start)
            else
                Vector(clipped.get.start, clipped.get.end)
        }
        else
            Vector()
    }
}
