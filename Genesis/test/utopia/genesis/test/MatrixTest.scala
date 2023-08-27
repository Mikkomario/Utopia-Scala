package utopia.genesis.test

import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}

/**
  * A test for 2x2 and 3x3 matrix operations
  * @author Mikko Hilpinen
  * @since 16.7.2020, v2.3
  */
object MatrixTest extends App
{
	ParadigmDataType.setup()
	
	// Testing very basic 3D matrix functions
	val m3 = Matrix3D(
		1, 2, 3,
		4, 5, 6,
		7, 8, 9
	)
	assert(m3.columns.size == 3)
	assert(m3.rows.size == 3)
	assert(m3.columns.head == Vector3D(1, 4, 7))
	assert(m3.columns.last == Vector3D(3, 6, 9))
	assert(m3.rows.head == Vector3D(1, 2, 3))
	assert(m3.rows.last == Vector3D(7, 8, 9))
	assert(m3.row(1) == Vector3D(4, 5, 6))
	assert(m3.column(1) == Vector3D(2, 5, 8))
	
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
	), m1(m2))
	
	// Testing matrix vector multiplication
	val v1 = Vector3D(2, 1, 3)
	assert(m3(v1) == Vector3D(13, 31, 49), m3(v1))
	
	// Testing 2D matrix determinant
	val m5 = Matrix2D(
		4, 6,
		3, 8
	)
	assert(m5.determinant == 14, m5.determinant)
	
	// Testing 3D matrix dropTo2D
	val m7 = Matrix3D(
		1, 2, 3,
		0, 1, 4,
		5, 6, 0
	)
	assert(m7.mapWithIndices { (v, _, _) => v } == m7)
	assert(m7.dropTo2D(0, 0) == Matrix2D(
		1, 4,
		6, 0
	), m7.dropTo2D(0, 0))
	assert(m7.dropTo2D(1, 0) == Matrix2D(
		0, 4,
		5, 0
	), m7.dropTo2D(1, 0))
	assert(m7.dropTo2D(1, 2) == Matrix2D(
		1, 3,
		0, 4
	), m7.dropTo2D(1, 2))
	assert(m7.dropTo2D(0, 1) == Matrix2D(
		2, 3,
		6, 0
	), m7.dropTo2D(0, 1))
	
	// Testing 3D matrix determinant
	val m4 = Matrix3D(
		1, 5, 3,
		2, 4, 7,
		4, 6, 2
	)
	assert(m4.determinant == 74, m4.determinant)
	
	// Testing 2D matrix inverse
	val m6 = Matrix2D(
		4, 7,
		2, 6
	)
	assert(m6.inverse.get ~== Matrix2D(
		0.6, -0.7,
		-0.2, 0.4
	), m6.inverse)
	assert(m6(m6.inverse.get) ~== Matrix2D.identity, m6(m6.inverse.get))
	
	// Testing 3D matrix inverse
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
	
	val m8 = Matrix2D.scaling(2)
	
	assert(m8.inverse.get ~== Matrix2D.scaling(0.5))
	assert(m8.to3D.inverse.get ~== Matrix2D.scaling(0.5).to3D)
	
	// {X: [0.75,0.0,0.0], Y: [0.0,0.75,0.0], Z: [944.0,535.0,1.0]} =>
	// 		{X: [-1.3333333333333333,0.0,-0.0], Y: [0.0,-1.3333333333333333,0.0], Z: [1258.6666666666665,713.3333333333333,-1.0]}
	val m9 = Matrix3D.affineTransform(Matrix2D.scaling(0.75), Vector2D(944, 535))
	println(m9)
	println(m9.inverse.get)
	println(m9 * m9.inverse.get)
	// Currently results in wrong inverse:
	// [-1.333, 0, 1258,
	//  0, -1.333, 713,
	//  0, 0, -1]
	// Should be:
	// [1.333, 0, -1258,
	//  0, 1.333, -713,
	//  0, 0, 1]
	println(Matrix2D.scaling(0.75).inverse.get)
	println(Matrix2D.scaling(0.75).to3D.inverse.get)
	
	// Determinant should be 0.5625
	println(m9.determinant)
	
	println("Success!")
}
