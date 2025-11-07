package utopia.flow.test.collection

import utopia.flow.collection.CollectionExtensions.{iterableOperations, _}
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.ordering.SomeBeforeNone

import scala.collection.View

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
	assert(result2 == Vector("Car"), result2)
	assert(result3 == words)
	
	val numbers = Vector(1, 2, 3, 4, 5)
	val range = NumericSpan(2, 4)
	
	//noinspection ConvertibleToMethodValue
	assert(numbers.bestMatch { range.contains(_) } == Vector(2, 3, 4))
	
	assert(words.mapFirstWhere { _.startsWith("C") } { _.toUpperCase } == Vector("Apina", "Banaani", "CAR", "David"))
	
	val splitResult = words.splitToSegments(2)
	assert(splitResult.size == 2)
	assert(splitResult.forall { _.size == 2 })
	
	assert(numbers.takeRightWhile { _ > 3 } == Vector(4, 5))
	assert(numbers.existsCount(3) { _ < 4 })
	assert(!numbers.existsCount(2) { _ > 4 })
	
	assert(numbers.findAndPop { _ > 2 } == (Some(3), Vector(1, 2, 4, 5)), numbers.findAndPop { _ > 2 })
	
	// Tests withoutIndex
	val v1 = Vector(1, 2, 3, 4)
	assert(v1.withoutIndex(0) == Vector(2, 3, 4), v1.withoutIndex(0))
	assert(v1.withoutIndex(1) == Vector(1, 3, 4), v1.withoutIndex(1))
	assert(v1.withoutIndex(2) == Vector(1, 2, 4), v1.withoutIndex(2))
	assert(v1.withoutIndex(3) == Vector(1, 2, 3), v1.withoutIndex(3))
	assert(v1.withoutIndex(-1) == v1, v1.withoutIndex(-1))
	assert(v1.withoutIndex(4) == v1, v1.withoutIndex(4))
	
	// Tests oneOrMany
	assert(result1.oneOrMany == Right(result1))
	assert(result2.oneOrMany == Left("Car"))
	
	words.emptyOneOrMany match {
		case None => assert(false)
		case Some(Left(_)) => assert(false)
		case Some(Right(many)) => assert(many == words)
	}
	
	// Tests zipMap and zipFlatMap
	assert(v1.take(2).zipMap { _ + 1 } == Vector((1, 2), (2, 3)))
	assert(v1.zipFlatMap { i => if (i % 2 == 0) Some(i + 1) else None } == Vector((2, 3), (4, 5)))
	
	// Tests SomeBeforeNone
	assert(Vector[Option[Int]](None, Some(1), Some(2)).sorted[Option[Int]](SomeBeforeNone) == Vector(Some(1), Some(2), None))
	
	// Tests takeMax etc.
	assert(v1.takeMax(2).toSet == Set(3, 4))
	assert(v1.takeMin(2).toSet == Set(1, 2))
	// NB: Slightly ambiguous use-case
	assert(words.takeMaxBy(2) { _.length }.toSet == Set("Apina", "Banaani"))
	assert(words.takeMinBy(2) { _.length }.toSet == Set("Apina", "Car"))
	
	// Tests equality for custom collections
	assert(Empty == Vector())
	assert(Single(1) == Vector(1))
	assert(Pair(1, 2) == Vector(1, 2))
	
	// Tests consecutive grouping
	val joined = Vector(1, 3, 5, 2, 4, 1).groupConsecutiveWith { _.last % 2 == _ % 2 }
	
	assert(joined.size == 3)
	assert(joined.head == Vector(1, 3, 5))
	assert(joined(1) == Pair(2, 4))
	assert(joined(2) == Single(1))
	
	assert(Vector().groupConsecutiveWith { (_, _) => true }.isEmpty)
	assert(Vector(1).groupConsecutiveWith { (_, _) => true }.only.contains(Single(1)))
	
	// Tests OptimizedIndexedSeq.concat(...)
	
	assert(OptimizedIndexedSeq.concat(Pair(1, 2), Pair(3, 4)) == Vector(1, 2, 3, 4))
	assert(OptimizedIndexedSeq.concat(Single(1), Single(2)) == Pair(1, 2))
	assert(OptimizedIndexedSeq.concat(Single(1), Iterator.iterate(2) { _ + 1 }.takeTo { _ == 4 }.caching) == Vector(1, 2, 3, 4))
	assert(OptimizedIndexedSeq.concat(Single(1), View.fromIteratorProvider { () => Iterator.empty }) == Single(1))
	
	// Tests tryReduce
	
	assert(numbers.tryReduceIterator { (a, b) =>
		if (b % a == 0)
			Some(a + b)
		else
			None
	}.toVector == Vector(6, 4, 5))
	
	// Tests groupsUnderSize
	
	private val colls = Vector(Single(1), Pair(1, 2), Vector(1, 2, 3), Vector(1, 2, 3, 4), Vector(1, 2, 3, 4, 5))
	private val grouped = colls.groupsWithinSize(5)
	
	assert(grouped == Vector(Vector(1, 2, 3, 4, 5), Vector(1, 2, 3, 4, 1), Vector(1, 2, 3, 1, 2)), grouped)
	
	// Tests OptimizedIndexedSeq.concat
	
	assert(OptimizedIndexedSeq.concat(Single(1), Empty, Single(2)) == Pair(1, 2))
	
	// Tests groupMapReduce
	
	assert(Vector("a1", "b1", "c1", "a2", "b2", "a3").iterator.groupMapReduce { _.head } { _.tail } { _ ++ _ } ==
		Map('a' -> "123", 'b' -> "12", 'c' -> "1"))
	
	// Tests startingWith and endingWith
	private val numbers3 = Vector(1, 2, 3)
	assert(numbers3.startingWith(0) == Vector(0, 1, 2, 3))
	assert(numbers3.endingWith(4) == Vector(1, 2, 3, 4))
	assert(numbers3.startingWith(1) == numbers3)
	assert(numbers3.endingWith(3) == numbers3)
	
	assert(Vector(1.0, 2.0, 3.0).average ~== 2.0)
	
	// Tests padToFrom
	private var appendCalls = 0
	private val appendingIter = numbers3.padToFromIterator(5) {
		appendCalls += 1
		Pair(4, 5)
	}
	
	assert(appendCalls == 0)
	assert(appendingIter.hasNext)
	assert(appendingIter.next() == 1)
	assert(appendCalls == 0)
	assert(appendingIter.hasNext)
	assert(appendingIter.next() == 2)
	assert(appendCalls == 0)
	assert(appendingIter.hasNext)
	assert(appendingIter.next() == 3)
	assert(appendCalls == 0)
	assert(appendingIter.hasNext)
	assert(appendCalls == 1)
	assert(appendingIter.next() == 4)
	assert(appendCalls == 1)
	assert(appendingIter.hasNext)
	assert(appendingIter.next() == 5)
	assert(appendCalls == 1)
	assert(!appendingIter.hasNext)
	
	assert(numbers3.padToFrom(6) { Iterator.iterate(4) { _ + 1 } } == Vector(1, 2, 3, 4, 5, 6))
	assert(numbers3.padToFrom(2) { throw new IllegalStateException("Can't arrive here") } == numbers3)
	
	println("Success!")
}
