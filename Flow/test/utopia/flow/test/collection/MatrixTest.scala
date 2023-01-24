package utopia.flow.test.collection

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Matrix, Pair}

/**
 * A simple matrix test
 * @author Mikko Hilpinen
 * @since 22.1.2023, v2.0
 */
object MatrixTest extends App
{
	val width = 8
	val matrix = Matrix.fill(Pair.twice(width)) { case Pair(x, y) => y * width + x }
	
	def printMatrix(m: Matrix[_], name: String) = {
		println(s"\n--- $name ---")
		m.rows.foreach { row => println(row.mkString(", ")) }
		println("---")
	}
	
	printMatrix(matrix, "all")
	printMatrix(matrix.viewBetween(Pair(Pair.twice(1), Pair.twice(3))), "1-3")
	
	val cropped = matrix.view(Pair(7, 3).map { p => NumericSpan(p - 1, p + 1) })
	printMatrix(cropped, "cropped sub-region")
	println(cropped.iterator.mkString(", "))
	
	printMatrix(matrix.crop(Pair(1, 2)), "cropped")
}
