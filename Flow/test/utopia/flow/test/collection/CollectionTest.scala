package utopia.flow.test.collection

import utopia.flow.collection.CollectionExtensions._

/**
 * A test for Flow collections / collection extensions
 * @author Mikko Hilpinen
 * @since 5.4.2019
 */
object CollectionTest extends App
{
	val words = Vector("Apina", "Banaani", "Car", "David")
	
	val lengthUnder4 = words.findMap {
		w =>
			val len = w.length
			if (len <= 3) Some(len) else None
	}
	
	assert(lengthUnder4.contains(3))
	
	// Tests bestMatch -feature
	val conditions1 = Vector[String => Boolean](_.length > 3, _.contains('n'))
	val conditions2 = Vector[String => Boolean](_.length <= 3, _.contains('n'))
	val conditions3 = Vector[String => Boolean](_.nonEmpty)
	
	val result1: Vector[String] = words.bestMatch(conditions1)
	val result2: Vector[String] = words.bestMatch(conditions2)
	val result3: Vector[String] = words.bestMatch(conditions3)
	
	assert(result1 == Vector("Apina", "Banaani"))
	assert(result2 == Vector("Car"))
	assert(result3 == words)
	
	assert(words.mapFirstWhere { _.startsWith("C") } { _.toUpperCase } == Vector("Apina", "Banaani", "CAR", "David"))
	
	val splitResult = words.splitToSegments(2)
	assert(splitResult.size == 2)
	assert(splitResult.forall { _.size == 2 })
	
	val numbers = Vector(1, 2, 3, 4, 5)
	assert(numbers.takeRightWhile { _ > 3 } == Vector(4, 5))
	assert(numbers.existsCount(3) { _ < 4 })
	assert(!numbers.existsCount(2) { _ > 4 })
	
	assert(numbers.findAndPop { _ > 2 } == (Some(3), Vector(1, 2, 4, 5)), numbers.findAndPop { _ > 2 })
	
	/*
	def lazyNumber(i: Int) = {
		println(s"generating $i")
		i
	}
	val lv = LazyVector(() => lazyNumber(1), () => lazyNumber(2), () => lazyNumber(3))
	val lv2 = lv.map { -_ }
	
	println(lv2(2))
	 */
	
	println("Success!")
}
