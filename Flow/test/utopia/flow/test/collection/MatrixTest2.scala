package utopia.flow.test.collection

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Matrix, Pair}

/**
  * Tests matrix classes
  * @author Mikko Hilpinen
  * @since 20.11.2023, v2.3
  */
object MatrixTest2 extends App
{
	val m1 = Matrix(Vector(1,2,3,4,5,6,7,8,9), 3, 3, rowsToColumns = true)
	
	private def test3x3(m: Matrix[Int]) = {
		m.rows.foreach { row => println(row.mkString("|")) }
		
		assert(m.width == 3)
		assert(m.height == 3)
		
		assert(m(0, 0) == 1)
		assert(m(0, 1) == 4)
		assert(m(1, 0) == 2)
		assert(m(1, 1) == 5)
		
		assert(m.rowsView(0).toVector == Vector(1, 2, 3))
		assert(m.rowsView(1).toVector == Vector(4, 5, 6))
		assert(m.columnsView(0).toVector == Vector(1, 4, 7))
		assert(m.columnsView(1).toVector == Vector(2, 5, 8))
		
		assert(m.viewRow(0).toVector == Vector(1, 2, 3))
		assert(m.viewRow(1).toVector == Vector(4, 5, 6))
		assert(m.viewColumn(0).toVector == Vector(1, 4, 7))
		assert(m.viewColumn(1).toVector == Vector(2, 5, 8))
		
		assert(m.row(0) == Vector(1, 2, 3))
		assert(m.row(1) == Vector(4, 5, 6))
		assert(m.column(0) == Vector(1, 4, 7))
		assert(m.column(1) == Vector(2, 5, 8))
		
		assert(m.columnIndices == (0 until 3))
		assert(m.rowIndices == (0 until 3))
		
		assert(m.iteratorByColumns.toVector == Vector(1, 4, 7, 2, 5, 8, 3, 6, 9), m.iteratorByColumns.toVector)
		assert(m.iteratorByRows.toVector == Vector(1, 2, 3, 4, 5, 6, 7, 8, 9), m.iteratorByColumns.toVector)
		assert(m.indexIteratorByColumns.take(4).toVector == Vector(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0)),
			m.indexIteratorByColumns.take(4).toVector)
		assert(m.indexIteratorByRows.take(4).toVector == Vector(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1)),
			m.indexIteratorByRows.take(4).toVector)
		
		assert(m.isDefinedAt(Pair(1, 1)))
		assert(!m.isDefinedAt(3, 1))
		assert(!m.isDefinedAt(0, -1))
	}
	
	println("Testing rows-to-columns values")
	test3x3(m1)
	
	println("Testing columns-to-rows values")
	test3x3(Matrix(Vector(1,4,7,2,5,8,3,6,9), 3, 3))
	
	println("Testing rows")
	test3x3(Matrix.withRows(Vector(Vector(1,2,3), Vector(4,5,6), Vector(7,8,9))))
	
	println("Testing columns")
	test3x3(Matrix.withColumns(Vector(Vector(1,4,7), Vector(2,5,8), Vector(3,6,9))))
	
	val m2 = Matrix(Vector(1,2,3,4,5,6,7,8), 2, 4)
	
	assert(m2.width == 2)
	assert(m2.height == 4)
	
	val tm2 = m2.transpose
	
	assert(tm2.width == 4)
	assert(tm2.height == 2)
	assert(tm2.viewRow(0).toVector == Vector(1,2,3,4), tm2.viewRow(0).mkString(", "))
	assert(tm2.viewRow(1).toVector == Vector(5,6,7,8))
	assert(tm2.viewColumn(0).toVector == Vector(1,5))
	assert(tm2.viewColumn(1).toVector == Vector(2,6))
	assert(tm2.rows(0) == Vector(1,2,3,4))
	assert(tm2.columns(1) == Vector(2,6))
	assert(tm2(1,0) == 2)
	assert(tm2(0,1) == 5)
	assert(tm2(1,1) == 6)
	
	val v1 = m1.view(Pair(NumericSpan(1,2), NumericSpan(0,1))).map { _ + 4 }
	
	assert(v1.width == 2)
	assert(v1.height == 2)
	assert(v1.viewColumn(0).toVector == Vector(6,9))
	assert(v1.viewColumn(1).toVector == Vector(7,10))
	assert(v1.viewRow(0).toVector == Vector(6,7))
	assert(v1.viewRow(1).toVector == Vector(9,10))
	assert(v1(0,0) == 6)
	assert(v1(1,0) == 7)
	assert(v1(0,1) == 9)
	assert(v1(1,1) == 10)
	assert(!v1.isDefinedAt(Pair(-1,0)))
	
	println("Success!")
}
