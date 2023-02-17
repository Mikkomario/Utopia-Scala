package utopia.flow.test.collection

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Matrix, Pair}
import utopia.flow.util.logging.TimeLogger

/**
 * Used for testing lazily initialized matrices
 * @author Mikko Hilpinen
 * @since 17.2.2023, v2.0.1
 */
object MatrixPerformanceTest extends App
{
	// val delay = 0.001.seconds
	// val waitLock = new AnyRef
	val log = new TimeLogger()
	
	log.checkPoint("Test 1")
	log.checkPoint("Test 2")
	log.checkPoint("Test 3")
	
	val mappedRange = (0 until 10000).map { i => i }
	
	log.checkPoint("Mapped a range")
	
	val s = mappedRange.size
	
	log.checkPoint(s"Range size is $s")
	
	val span1 = NumericSpan(100, 200)
	val span2 = NumericSpan(0, 3000)
	log.checkPoint("Spans created")
	
	println(s"Span overlap = ${ span1.overlapWith(span2) }")
	
	log.checkPoint("Overlap calculated")
	
	val callsIter = Iterator.iterate(1) { _ + 1 }
	val matrix = Matrix.lazyFill(Pair.twice(3000)) { _ => callsIter.next() }
	
	log.checkPoint("Matrix created")
	// println(s"Next number = ${ callsIter.next() }")
	
	println(s"Matrix size is ${ matrix.size}")
	
	log.checkPoint("Matrix size printed")
	
	val span = NumericSpan(100, 200)
	log.checkPoint("Span specified")
	val area = Pair.twice(span)
	log.checkPoint("Area specified")
	val view = matrix.view(area)
	
	log.checkPoint("View created")
	// println(s"Next number = ${ callsIter.next() }")
	
	val viewIter = view.iterator.foreach { _ => () }
	// println(s"${ viewIter.next() } - ${ viewIter.last }")
	// println(s"Next number = ${ callsIter.next() }")
	
	log.checkPoint("View iterated over")
	
	// val matrixIter = matrix.iterator.foreach { _ => () }
	// println(s"${matrixIter.next()} - ${matrixIter.last}")
	// println(s"Next number = ${ callsIter.next() }")
	
	// log.checkPoint("Matrix iterated over")
	
	log.print()
	println("Done!")
}
