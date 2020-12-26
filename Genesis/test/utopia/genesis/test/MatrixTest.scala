package utopia.genesis.test

import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.shape2D.Matrix2D
import utopia.genesis.shape.shape3D.{Matrix3D, Vector3D}

/**
  * A test for 2x2 and 3x3 matrix operations
  * @author Mikko Hilpinen
  * @since 16.7.2020, v2.3
  */
object MatrixTest extends App
{
	GenesisDataType.setup()
	
	// Testing matrix multiplication
	val m1 = Matrix3D(
		10, 20, 10,
		4, 5, 6,
		2, 3, 5
	)
	val m2 = Matrix3D(
		3, 2, 4,
		3, 3, 9,
		4, 4, 2
	)
	assert(m1(m2) == Matrix3D(
		130, 120, 240,
		51, 47, 73,
		35, 33, 45
	))
	
	// Testing matrix vector multiplication
	val m3 = Matrix3D(
		1, 2, 3,
		4, 5, 6,
		7, 8, 9
	)
	val v1 = Vector3D(2, 1, 3)
	assert(m3(v1) == Vector3D(13, 31, 49))
	
	// Testing 2D matrix determinant
	val m5 = Matrix2D(
		4, 6,
		3, 8
	)
	assert(m5.determinant == 14)
	
	// Testing 3D matrix determinant
	val m4 = Matrix3D(
		1, 5, 3,
		2, 4, 7,
		4, 6, 2
	)
	assert(m4.determinant == 74)
	
	// Testing 2D matrix inverse
	val m6 = Matrix2D(
		4, 7,
		2, 6
	)
	assert(m6.inverse.get ~== Matrix2D(
		0.6, -0.7,
		-0.2, 0.4
	))
	assert(m6(m6.inverse.get) ~== Matrix2D.identity)
	
	// Testing 3D matrix inverse
	val m7 = Matrix3D(
		1, 2, 3,
		0, 1, 4,
		5, 6, 0
	)
	assert(m7.mapWithIndices { (v, _, _) => v } == m7)
	assert(m7.dropTo2D(0, 0) == Matrix2D(
		1, 4,
		6, 0
	))
	assert(m7.dropTo2D(1, 0) == Matrix2D(
		0, 4,
		5, 0
	))
	assert(m7.dropTo2D(1, 2) == Matrix2D(
		1, 3,
		0, 4
	))
	assert(m7.dropTo2D(0, 1) == Matrix2D(
		2, 3,
		6, 0
	))
	assert(m7.determinant == 1.0)
	assert(m7.transposed == Matrix3D(
		1, 0, 5,
		2, 1, 6,
		3, 4, 0
	))
	assert(m7.adjugate == Matrix3D(
		-24, 18, 5,
		20, -15, -4,
		-5, 4, 1
	))
	assert(m7.inverse.get == Matrix3D(
		-24, 18, 5,
		20, -15, -4,
		-5, 4, 1
	))
	
	// Tests inverse matrix vector multiplication
	assert(m7.inverse.get(m7(v1)) ~== v1)
	
	println("Success!")
}
