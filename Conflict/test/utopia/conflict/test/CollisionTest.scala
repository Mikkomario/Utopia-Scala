package utopia.conflict.test

import utopia.conflict.test.TestCollisionGroups.Obstacle
import utopia.conflict.collision.Extensions._
import utopia.conflict.util.DefaultSetup
import utopia.flow.test.TestContext._
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point, Polygon, Size, Vector2D}
import utopia.genesis.shape.shape2D.transform.AffineTransformation

import scala.collection.immutable.HashSet

/**
 * This test visually displays collision data with interactive elements
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
object CollisionTest extends App
{
    // Sets up the program
    val worldSize = Size(800, 600)
    
    val setup = new DefaultSetup(worldSize, "Collision Test")
    
    
    val simplePolygon = Polygon(Point(-32, -32), Point(0, 64), Point(32, 32), Point.origin)
    val transformedPolygon = AffineTransformation(worldSize.toVector / 2, scaling = Vector2D(2, 2)).transform(simplePolygon)
    
    val nonConvexPolygon = Polygon(Point(-32, -32), Point(-0.5), Point(-32, 32), Point(32, 32), Point(0.5), Point(32, -32))
    
    val obstacle1 = new TestPolygonObstacle(transformedPolygon)
    val obstacle2 = new TestPolygonObstacle(Circle(Point(96, 228), 64).toPolygon(12))
    val obstacle3 = new TestPolygonObstacle(Bounds(worldSize.toPoint - Vector2D(128, 128), Size(64, 64)))
    val obstacle4 = new TestPolygonObstacle(AffineTransformation.translation(Vector2D(worldSize.x - 128, 32)).transform(nonConvexPolygon))
    
    val obstacle5 = new TestCircleObstacle(Circle(worldSize.toPoint - Vector2D(128, 128), 96))
    
    // val mouseObstacle = new MousePolygonObstacle(Polygon(Vector(Vector3D(24), Vector3D(0, -24), Vector3D(-24), Vector3D(0, 24))))
    val mouseObstacle = new MousePolygonObstacle(Bounds(Point(-32, -32), Size(64, 64)))
    
    val collisionDrawer = new CollisionDrawer(mouseObstacle, Some(HashSet(Obstacle)))
    val projectionDrawer = new ProjectionDrawer(transformedPolygon)
    
    setup.registerObjects(obstacle1, obstacle2, obstacle3, obstacle4, obstacle5, mouseObstacle, collisionDrawer, projectionDrawer)
    
    /*
    val grid = new GridDrawer(worldSize, Vector3D(80, 80))
    val numbers = new GridNumberDrawer(grid)
    val camera = new MagnifierCamera(64)
    
    handlers += grid
    handlers += numbers
    handlers += camera
    handlers += camera.drawHandler
    
    camera.drawHandler += grid
    camera.drawHandler += numbers
    */
    
    // Starts the program
    setup.start()
}