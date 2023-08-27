package utopia.paradigm.test

import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.shape.shape1d.vector.Vector1D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.Dimensions

/**
  * Tests basic axis and vector functions
  * @author Mikko Hilpinen
  * @since 15.11.2022, v1.2
  */
object AxisTest extends App
{
	ParadigmDataType.setup()
	
	// Tests Axis to Vector1D & Vector2D conversion
	
	assert(X.unit == Vector1D(1.0, X))
	assert(Y.unit == Vector1D(1.0, Y))
	assert(Z.unit == Vector1D(1.0, Z))
	
	assert(X.unit.dimensions == Dimensions.double(1.0), X.unit.dimensions)
	assert(Y.unit.dimensions == Dimensions.double(0.0, 1.0), Y.unit.dimensions)
	assert(Z.unit.dimensions == Dimensions.double(0.0, 0.0, 1.0), Z.unit.dimensions)
	
	val x2d = X.unit.toVector2D
	val y2d = Y.unit.toVector2D
	
	assert(x2d == Vector2D(1.0, 0.0), x2d)
	assert(y2d == Vector2D(0.0, 1.0), y2d)
	assert(Z.unit.toVector3D == Vector3D(0.0, 0.0, 1.0))
	
	assert(x2d.x == 1.0)
	assert(x2d.y == 0.0)
	assert(y2d.x == 0.0)
	assert(y2d.y == 1.0)
	
	println("Success")
}
