package utopia.genesis.test

import utopia.genesis.shape.Vector3D

import utopia.genesis.util.Extensions._
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.Angle

object VectorTest extends App
{
    GenesisDataType.setup()
    
    val v1 = Vector3D(1, 1)
    
    println(s"$v1 * ${ Vector3D.identity } = ${ v1 * Vector3D.identity }")
    
    assert(v1 == Vector3D(1, 1))
    assert(v1 * 1 == v1)
    assert(v1 * Vector3D.identity == v1)
    assert(v1 / 1 == v1)
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
    
    assert(v2.rotatedDegs(90) ~== Vector3D(0, 1))
    
    assert(v1.angleDifferenceRads(Vector3D(1)).toDegrees ~== 45.0)
    assert((Vector3D(1) angleDifferenceRads Vector3D(0, 1)).toDegrees ~== 90.0)
    
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
    
    println("Success")
}