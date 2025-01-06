package utopia.paradigm.test

import utopia.flow.operator.Identity
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D

object VectorTest extends App
{
	ParadigmDataType.setup()
	
	val v1 = Vector3D(1, 1)
	
	println(s"$v1 * ${ Vector3D.identity } = ${ v1 * Vector3D.identity }")
	
	assert(Vector3D(1, 0, 0) == Vector3D(1))
	assert(Vector3D(1, 0, 0) == X(1).in3D)
	
	assert(v1.map(Identity) == v1)
	assert(v1 == Vector3D(1, 1))
	assert(v1 * 1 == v1)
	assert(v1 * Vector3D.identity == v1)
	assert(v1 / 1 == v1, v1 / 1)
	assert(v1 / Vector3D.identity == v1)
	assert(v1.length > 1)
	assert(v1 - v1 == Vector3D.zero)
	assert(v1.toUnit.length ~== 1.0)
	
	assert(v1.scalarProjection(Vector3D(1)) == 1)
	assert(v1.projectedOver(Vector3D(1)) == Vector3D(1))
	assert(v1.projectedOver(Vector3D(0, 1)) == Vector3D(0, 1))
	assert(v1.projectedOver(v1) == v1)
	assert(v1.projectedOver(-v1) == v1)
	
	assert(v1 isParallelWith Vector3D(2, 2))
	assert(v1 isParallelWith Vector3D(-1, -1))
	assert(v1 isPerpendicularTo Vector3D(1, -1))
	
	assert(v1 == Vector3D(1) + Vector3D(0, 1))
	
	assert(Vector3D.lenDir(1, Angle.right) == Vector3D(1))
	assert(Vector3D.lenDir(1, Angle.down) ~== Vector3D(0, 1))
	assert(Vector3D.lenDir(1, Angle.left) ~== Vector3D(-1))
	
	val v2 = Vector3D(1)
	
	assert(v2.rotated(Rotation.clockwise.degrees(90)) ~== Vector3D(0, 1))
	
	assert(v1.angleDifference(Vector3D(1)).degrees ~== 45.0)
	assert((Vector3D(1) angleDifference Vector3D(0, 1)).degrees ~== 90.0)
	
	// (-1, 7, 0) x (-5, 8, 0) = (0, 0, 27)
	println(Vector3D(-1, 7) cross Vector3D(-5, 8))
	assert(Vector3D(-1, 7) cross Vector3D(-5, 8) ~== Vector3D(0, 0, 27))
	// (-1, 7, 4) x (-5, 8, 4) = (-4, -16, 27)
	println(Vector3D(-1, 7, 4) cross Vector3D(-5, 8, 4))
	assert(Vector3D(-1, 7, 4) cross Vector3D(-5, 8, 4) ~== Vector3D(-4, -16, 27))
	// (10, 0, 0) x (10, 0, -2) = (0, 20, 0)
	println(Vector3D(10) cross Vector3D(10, 0, -2))
	assert(Vector3D(10) cross Vector3D(10, 0, -2) ~== Vector3D(0, 20))
	
	// Tests normals
	assert(v1.normal2D isPerpendicularTo v1)
	assert(v1.normal isPerpendicularTo v1)
	
	val v3 = Vector3D(1, 1, 1)
	
	assert(v3.normal isPerpendicularTo v3)
	
	// Tests projection sign handling
	val v4 = Vector2D(1)
	
	assert(Vector2D(1, 1).scalarProjection(v4) ~== 1.0)
	assert(Vector2D(-1, 1).scalarProjection(v4) ~== -1.0, Vector2D(-1, 1).scalarProjection(v4))
	
	// Tests parallel checking for Z vectors
	private val v5 = Vector3D(-0, 0, 99881.08846238609)
	private val v6 = Vector3D(-0, 0, 658977574.161545)
	
	assert(!v5.isAboutZero)
	assert(v5.angleDifference(v6).isAboutZero, v5.angleDifference(v6).radians)
	// assert(v5.crossProductLength(v6) ~== 0.0, v5.crossProductLength(v6))
	assert(v5.isParallelWith(v6))
	
	println("Success")
}