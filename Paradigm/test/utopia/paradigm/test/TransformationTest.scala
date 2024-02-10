package utopia.paradigm.test

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.angular.Rotation
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.transform.{AffineTransformation, LinearTransformation}

/**
 * This test tests the basic transformation class features
 */
object TransformationTest extends App
{
	println(Matrix2D.quarterRotationCounterClockwise)
	println(Matrix2D.quarterRotationClockwise)
	
	val translation = AffineTransformation.translation(Vector2D(10))
	val scaling = LinearTransformation.scaling(Vector2D(2, 1))
	val rotation = LinearTransformation.rotation(Rotation.clockwise.degrees(90))
 
	// Tests - for transformations
	
	assert((-translation).position.x == -10)
	assert((-scaling).scaling.x == 0.5)
	assert((-rotation).rotation.clockwise.degrees ~== -90.0)
	
	// Tests transformation reversal (t * !t should be identity)
	
	assert(translation.toMatrix.inverse.get(translation.toMatrix) == Matrix3D.identity)
	assert(scaling.toMatrix.inverse.get(scaling.toMatrix) == Matrix2D.identity)
	assert(rotation.toMatrix.inverse.get(rotation.toMatrix) == Matrix2D.identity)
	
	// Tests transformation on Vector2D
	
	val position = Vector2D(10, 10)
	
	assert(translation(position) == Vector2D(20, 10))
	assert(scaling(position) == Vector2D(20, 10))
	assert(rotation(position) == Vector2D(-10, 10))
	
	// Tests inverse transformation on a vector
	
	assert(translation.invert(position).get == Vector2D(0, 10))
	assert(scaling.invert(position).get == Vector2D(5, 10))
	assert(rotation.invert(position).get == Vector2D(10, -10))
	
	// Tests combined transformations
	
	assert(position * translation * scaling == Vector2D(40, 10))
	assert(position * scaling * translation == Vector2D(30, 10))
	assert(position * rotation * scaling == Vector2D(-20, 10))
	assert(position * scaling * rotation ~== Vector2D(-10, 20))
	
	/*
	val combo = rotation.toMatrix.to3D
	
	println(s"${Matrix2D.quarterRotationClockwise} * ${Matrix2D.quarterRotationClockwise.inverse.get} = ${Matrix2D.quarterRotationClockwise.to3D * Matrix2D.quarterRotationClockwise.inverse.get.to3D}")
	println(s"$combo * ${combo.inverse.get} = ${combo * combo.inverse.get}")
	assert(combo * combo.inverse.get ~== Matrix3D.identity)
	assert(position * combo * combo.inverse.get ~== position)
	*/
	
	println("Success!")
	
    /*
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
    
     */
}