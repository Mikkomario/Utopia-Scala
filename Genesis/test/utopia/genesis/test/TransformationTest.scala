package utopia.genesis.test

import utopia.genesis.shape.shape2D.{Line, Point, Transformation}
import utopia.genesis.shape.{Rotation, Vector3D}
import utopia.genesis.util.Extensions._

/**
 * This test tests the basic transformation class features
 */
object TransformationTest extends App
{
    val translation = Transformation.translation(Vector3D(10))
    val scaling = Transformation.scaling(2)
    val rotation = Transformation.rotationDegs(90)
    
    assert(rotation.rotationRads ~== Math.PI / 2)
    
    assert((-translation).position.x == -10)
    assert((-scaling).scaling.x == 0.5)
    assert((-rotation).rotationDegs ~== -90.0)
    
    /*
    assert(translation - translation == Transformation.identity)
    assert(scaling - scaling == Transformation.identity)
    assert(rotation - rotation == Transformation.identity)
    */
    
    assert(scaling + Transformation.identity == scaling)
    
    val pos = Vector3D(10)
    
    assert(translation(pos).x == 20)
    assert(scaling(pos).x == 20)
    assert(rotation(pos).y == 10)
    
    assert(rotation(translation(pos)) == rotation(translation)(pos))
    
    val combo = translation + rotation + scaling
    
    assert(combo.invert(combo(pos)) == pos)
    assert(combo.invert(pos) == (-combo)(pos))
    assert(-(-combo) == combo)
    
    val rotated = translation.absoluteRotated(Rotation.ofDegrees(90), Point(20, 0))
    assert(rotated.position ~== Point(20, -10))
    assert(rotated.rotationDegs ~== 90.0)
    
    val pos2 = Vector3D(19, -23)
    val line = Line(pos.toPoint, pos2.toPoint)
    val transformedLine = combo(line)
    
    assert(transformedLine == Line(combo(pos).toPoint, combo(pos2).toPoint))
    assert(combo.invert(transformedLine) == line)
    
    println("Success")
}