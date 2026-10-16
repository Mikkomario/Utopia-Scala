package utopia.paradigm.test

import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.EqualsFunction
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.enumeration.{Axis, Axis2D}
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.paradigm.measurement.DistanceUnit.{KiloMeter, MilliMeter}
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.template.DimensionalFactory.factoryToBuildFrom
import utopia.paradigm.shape.template.{DimensionalFactory, Dimensions, HasDimensions}

import scala.collection.BuildFrom

/**
  * Tests public functions of Dimensions, HasDimensions, and constructors in DimensionalFactory and Dimensions.
  * @author Mikko Hilpinen
  * @since 16.10.2026, v1.7
  */
object DimensionsTest extends App
{
	implicit val intEquals: EqualsFunction[Int] = EqualsFunction.default
	
	// ==========================================
	// 1. DIMENSIONAL FACTORY CONSTRUCTORS & API
	// ==========================================
	
	val factory = Dimensions.int
	
	// empty
	assert(factory.empty.length == 0)
	assert(factory.empty.isZero)
	
	// apply()
	assert(factory().length == 0)
	assert(factory() == factory.empty)
	
	// apply(x) - 1D
	val f1 = factory(10)
	assert(f1.length == 1)
	assert(f1.values == Single(10))
	assert(f1.x == 10)
	
	// apply(x, y) - 2D
	val f2 = factory(10, 20)
	assert(f2.length == 2)
	assert(f2.values == Pair(10, 20))
	assert(f2.x == 10 && f2.y == 20)
	
	// apply(x, y, z) - 3D
	val f3 = factory(10, 20, 30)
	assert(f3.length == 3)
	assert(f3.values == Vector(10, 20, 30))
	assert(f3.x == 10 && f3.y == 20 && f3.z == 30)
	
	// apply(parallel, perpendicular, along: Axis2D) - 2D
	assert(factory(10, 20, X) == factory(10, 20))
	assert(factory(10, 20, Y) == factory(20, 10))
	
	// twice(coordinate) - 2D
	assert(factory.twice(15) == factory(15, 15))
	
	// apply(IndexedSeq) - 1D, 2D, 3D, 4D
	assert(factory(Vector(1)) == factory(1))
	assert(factory(Vector(1, 2)) == factory(1, 2))
	assert(factory(Vector(1, 2, 3)) == factory(1, 2, 3))
	val f4 = factory(Vector(1, 2, 3, 4))
	assert(f4.length == 4)
	assert(f4.values == Vector(1, 2, 3, 4))
	
	// apply(Map[Axis, D])
	assert(factory(Map[Axis, Int]()).isEmpty)
	assert(factory(Map[Axis, Int](X -> 5)) == factory(5))
	assert(factory(Map[Axis, Int](X -> 5, Y -> 6)) == factory(5, 6))
	assert(factory(Map[Axis, Int](X -> 5, Y -> 6, Z -> 7)) == factory(5, 6, 7))
	assert(factory(Map[Axis, Int](Y -> 6)) == factory(0, 6))
	assert(factory(Map[Axis, Int](Z -> 7)) == factory(0, 0, 7))
	assert(factory(Map[Axis, Int](X -> 5, Z -> 7)) == factory(5, 0, 7))
	
	// from(IterableOnce) - 1D, 2D, 3D, 4D
	assert(factory.from(List(1)) == factory(1))
	assert(factory.from(List(1, 2)) == factory(1, 2))
	assert(factory.from(List(1, 2, 3)) == factory(1, 2, 3))
	assert(factory.from(List(1, 2, 3, 4)) == f4)
	assert(factory.from(f4) eq f4) // returns existing Dimensions as-is
	
	// fill(length)(elem) - 1D, 2D, 3D, 4D
	assert(factory.fill(1)(7) == factory(7))
	assert(factory.fill(2)(7) == factory(7, 7))
	assert(factory.fill(3)(7) == factory(7, 7, 7))
	assert(factory.fill(4)(7) == factory(Vector(7, 7, 7, 7)))
	
	// iterate(length)(f) - 1D, 2D, 3D, 4D
	assert(factory.iterate(1)(_.index * 2) == factory(0))
	assert(factory.iterate(2)(_.index * 2) == factory(0, 2))
	assert(factory.iterate(3)(_.index * 2) == factory(0, 2, 4))
	assert(factory.iterate(4)(_.index * 2) == factory(0, 2, 4)) // Axis.values has 3 axes
	
	// fromFunction2D(f)
	assert(factory.fromFunction2D(a => if (a == X) 10 else 20) == factory(10, 20))
	
	// fromFunction3D(f)
	assert(factory.fromFunction3D(a => (a.index + 1) * 10) == factory(10, 20, 30))
	
	// newBuilder, update, +=, ++=, result
	val b1 = factory.newBuilder
	b1 += 1
	b1 += 2
	assert(b1.result() == factory(1, 2))
	b1.clear()
	b1 += (X -> 10)
	b1 += (Z -> 30)
	assert(b1.result() == factory(10, 0, 30))
	b1.clear()
	b1 ++= List(Y -> 20, X -> 5)
	assert(b1.result() == factory(5, 20))
	
	// mapResult
	val mappedFactory = factory.mapResult { d => d.values.sum }
	assert(mappedFactory(Vector(1, 2, 3, 4)) == 10)
	assert(mappedFactory(Map[Axis, Int](X -> 10, Y -> 20)) == 30)
	assert(mappedFactory.from(List(1, 2, 3)) == 6)
	val mappedBuilder = mappedFactory.newBuilder
	mappedBuilder += 5
	mappedBuilder += 15
	assert(mappedBuilder.result() == 20)
	
	// DimensionalFactory.factoryToBuildFrom
	val buildFrom: BuildFrom[Any, Int, Dimensions[Int]] = factoryToBuildFrom(factory)
	val builtViaBuildFrom = buildFrom.fromSpecific(Vector())(Vector(1, 2, 3, 4))
	assert(builtViaBuildFrom == f4)
	val buildFromBuilder = buildFrom.newBuilder(Vector())
	buildFromBuilder += 1
	buildFromBuilder += 2
	assert(buildFromBuilder.result() == factory(1, 2))
	
	
	// ==========================================
	// 2. DIMENSIONS COMPANION OBJECT & CONSTRUCTORS
	// ==========================================
	
	// Dimensions.int
	assert(Dimensions.int.zero == 0)
	
	// Dimensions.double
	assert(Dimensions.double.zero == 0.0)
	assert(Dimensions.double(1.0, 2.0).values == Pair(1.0, 2.0))
	
	// Dimensions.apply[A](zero)
	val customFactory = Dimensions("default")
	assert(customFactory.zero == "default")
	assert(customFactory("a", "b").values == Pair("a", "b"))
	assert(customFactory("a")(1) == "default")
	
	// Dimensions.optional
	val optFactory = Dimensions.optional[Int]
	assert(optFactory.zero.isEmpty)
	assert(optFactory(Some(1), None).values == Pair(Some(1), None))
	
	// Dimensions.distance
	val distDims = Dimensions.distance(Distance(10.0, MilliMeter), Distance(20.0, MilliMeter))
	assert(distDims.values == Pair(Distance(10.0, MilliMeter), Distance(20.0, MilliMeter)))
	assert(distDims.zeroValue == Distance(0.0, MilliMeter))
	
	// Dimensions.in(DistanceUnit)
	val kmFactory = Dimensions.in(KiloMeter)
	val kmDims = kmFactory(1.0, 2.5)
	assert(kmDims.values == Pair(Distance(1.0, KiloMeter), Distance(2.5, KiloMeter)))
	assert(kmDims.zeroValue == Distance(0.0, KiloMeter))
	
	// DimensionsFactory specific properties & methods
	assert(factory.zero == 0)
	assert(factory.empty == Dimensions(0)())
	assert(factory.zero1D == factory(0))
	assert(factory.zero2D == factory(0, 0))
	assert(factory.zero3D == factory(0, 0, 0))
	assert(factory(X, 10) == factory(10))
	assert(factory(Y, 20) == factory(0, 20))
	assert(factory(Z, 30) == factory(0, 0, 30))
	
	// Implicit doubleFactory conversion
	def acceptDoubleFactory(f: DimensionalFactory[Double, Dimensions[Double]]) = f(1.5, 2.5)
	assert(acceptDoubleFactory(Dimensions) == Dimensions.double(1.5, 2.5))
	
	
	// ==========================================
	// 3. HASDIMENSIONS PUBLIC FUNCTIONS
	// ==========================================
	
	// Setup 1D, 2D, 3D, and 4D instances
	val d1: Dimensions[Int] = factory(10)
	val d2: Dimensions[Int] = factory(10, 20)
	val d3: Dimensions[Int] = factory(10, 20, 30)
	val d4: Dimensions[Int] = factory(Vector(10, 20, 30, 40))
	
	// Also test with a custom non-Dimensions HasDimensions implementation
	case class CustomHasDimensions(dims: Dimensions[Int]) extends HasDimensions[Int] {
		override def dimensions: Dimensions[Int] = dims
	}
	val hd1: HasDimensions[Int] = CustomHasDimensions(d1)
	val hd2: HasDimensions[Int] = CustomHasDimensions(d2)
	val hd3: HasDimensions[Int] = CustomHasDimensions(d3)
	val hd4: HasDimensions[Int] = CustomHasDimensions(d4)
	
	// dimensions
	assert(hd1.dimensions == d1)
	assert(hd2.dimensions == d2)
	assert(hd3.dimensions == d3)
	assert(hd4.dimensions == d4)
	
	// x, y, z
	assert(hd1.x == 10 && hd1.y == 0 && hd1.z == 0)
	assert(hd2.x == 10 && hd2.y == 20 && hd2.z == 0)
	assert(hd3.x == 10 && hd3.y == 20 && hd3.z == 30)
	assert(hd4.x == 10 && hd4.y == 20 && hd4.z == 30)
	
	// xyPair
	assert(hd1.xyPair == Pair(10, 0))
	assert(hd2.xyPair == Pair(10, 20))
	assert(hd3.xyPair == Pair(10, 20))
	assert(hd4.xyPair == Pair(10, 20))
	
	// components
	assert(hd1.components.toVector == Vector(Dimension(X, 10, 0)))
	assert(hd2.components.toVector == Vector(Dimension(X, 10, 0), Dimension(Y, 20, 0)))
	assert(hd3.components.toVector == Vector(Dimension(X, 10, 0), Dimension(Y, 20, 0), Dimension(Z, 30, 0)))
	assert(hd4.components.toVector == Vector(Dimension(X, 10, 0), Dimension(Y, 20, 0), Dimension(Z, 30, 0)))
	
	// minDimension, maxDimension
	assert(hd1.minDimension == 10)
	assert(hd1.maxDimension == 10)
	assert(hd2.minDimension == 10)
	assert(hd2.maxDimension == 20)
	assert(hd3.minDimension == 10)
	assert(hd3.maxDimension == 30)
	assert(hd4.minDimension == 10)
	assert(hd4.maxDimension == 40)
	
	val hdNeg = CustomHasDimensions(factory(Vector(-5, 100, -20, 50)))
	assert(hdNeg.minDimension == -20)
	assert(hdNeg.maxDimension == 100)
	
	// apply(axis)
	assert(hd1(X) == 10 && hd1(Y) == 0 && hd1(Z) == 0)
	assert(hd2(X) == 10 && hd2(Y) == 20 && hd2(Z) == 0)
	assert(hd3(X) == 10 && hd3(Y) == 20 && hd3(Z) == 30)
	assert(hd4(X) == 10 && hd4(Y) == 20 && hd4(Z) == 30)
	
	// along(axis)
	assert(hd1.along(X) == Dimension(X, 10, 0))
	assert(hd1.along(Y) == Dimension(Y, 0, 0))
	assert(hd2.along(Y) == Dimension(Y, 20, 0))
	assert(hd3.along(Z) == Dimension(Z, 30, 0))
	assert(hd4.along(Z) == Dimension(Z, 30, 0))
	
	// ~== and !~==
	assert(hd1 ~== d1)
	assert(hd1 ~== factory(10, 0)) // trailing zeroes match
	assert(hd1 ~== factory(10, 0, 0))
	assert(hd1 ~== factory(Vector(10, 0, 0, 0)))
	assert(hd1 !~== d2)
	assert(hd2 ~== d2)
	assert(hd2 ~== factory(Vector(10, 20, 0, 0)))
	assert(hd2 !~== d3)
	assert(hd3 ~== d3)
	assert(hd3 ~== factory(Vector(10, 20, 30, 0)))
	assert(hd3 !~== d4)
	assert(hd4 ~== d4)
	assert(hd4 !~== factory(Vector(10, 20, 30, 41)))
	
	// isPositiveAlong, isNegativeAlong, isZeroAlong
	val hdSigns = CustomHasDimensions(factory(Vector(10, -5, 0, -2)))
	assert(hdSigns.isPositiveAlong(X))
	assert(!hdSigns.isNegativeAlong(X))
	assert(!hdSigns.isZeroAlong(X))
	
	assert(!hdSigns.isPositiveAlong(Y))
	assert(hdSigns.isNegativeAlong(Y))
	assert(!hdSigns.isZeroAlong(Y))
	
	assert(hdSigns.isPositiveAlong(Z)) // 0 >= 0 is true
	assert(hdSigns.isNegativeAlong(Z)) // 0 <= 0 is true
	assert(hdSigns.isZeroAlong(Z))
	
	assert(hd1.isZeroAlong(Y)) // omitted dimensions evaluate to 0 (zeroValue)
	assert(hd1.isZeroAlong(Z))
	
	// forAllDimensionsWith
	assert(hd1.forAllDimensionsWith(factory(10, 0, 0))(_ == _))
	assert(hd2.forAllDimensionsWith(factory(5, 10))(_ > _))
	assert(!hd3.forAllDimensionsWith(factory(10, 20, 99))(_ == _))
	assert(hd4.forAllDimensionsWith(factory(Vector(10, 20, 30, 40)))(_ == _))
	assert(hd4.forAllDimensionsWith(factory(Vector(5, 10, 15, 20)))(_ > _))
	// when other has fewer dimensions
	assert(hd4.forAllDimensionsWith(factory(10, 20)) { (a, b) => (a == 10 && b == 10) || (a == 20 && b == 20) || (b == 0) })
	
	// existsDimensionWith
	assert(hd1.existsDimensionWith(factory(10, 99))(_ == _))
	assert(!hd1.existsDimensionWith(factory(99, 99))(_ == _))
	assert(hd2.existsDimensionWith(factory(99, 20))(_ == _))
	assert(hd3.existsDimensionWith(factory(99, 99, 30))(_ == _))
	assert(hd4.existsDimensionWith(factory(Vector(99, 99, 99, 40)))(_ == _))
	assert(!hd4.existsDimensionWith(factory(Vector(99, 99, 99, 99)))(_ == _))
	
	
	// ==========================================
	// 4. DIMENSIONS PUBLIC FUNCTIONS (1D, 2D, 3D, 4D)
	// ==========================================
	
	// lazyZeroValue & zeroValue
	assert(d1.lazyZeroValue.value == 0)
	assert(d1.zeroValue == 0)
	assert(d2.zeroValue == 0)
	assert(d3.zeroValue == 0)
	assert(d4.zeroValue == 0)
	
	// values
	assert(d1.values == Single(10))
	assert(d2.values == Pair(10, 20))
	assert(d3.values == Vector(10, 20, 30))
	assert(d4.values == Vector(10, 20, 30, 40))
	
	// in2D
	assert(d1.in2D == factory(10, 0) && d1.in2D.length == 2)
	assert(d2.in2D == factory(10, 20) && d2.in2D.length == 2)
	assert(d3.in2D == factory(10, 20) && d3.in2D.length == 2)
	assert(d4.in2D == factory(10, 20) && d4.in2D.length == 2)
	
	// in3D
	assert(d1.in3D == factory(10, 0, 0) && d1.in3D.length == 3)
	assert(d2.in3D == factory(10, 20, 0) && d2.in3D.length == 3)
	assert(d3.in3D == factory(10, 20, 30) && d3.in3D.length == 3)
	assert(d4.in3D == factory(10, 20, 30) && d4.in3D.length == 3)
	
	// zipWithAxis
	assert(d1.zipWithAxis.toVector == Vector(10 -> X))
	assert(d2.zipWithAxis.toVector == Vector(10 -> X, 20 -> Y))
	assert(d3.zipWithAxis.toVector == Vector(10 -> X, 20 -> Y, 30 -> Z))
	assert(d4.zipWithAxis.toVector == Vector(10 -> X, 20 -> Y, 30 -> Z)) // limited by Axis.values count (3)
	
	// zipWithAxis2D
	assert(d1.zipWithAxis2D.toVector == Vector(10 -> X))
	assert(d2.zipWithAxis2D.toVector == Vector(10 -> X, 20 -> Y))
	assert(d3.zipWithAxis2D.toVector == Vector(10 -> X, 20 -> Y))
	assert(d4.zipWithAxis2D.toVector == Vector(10 -> X, 20 -> Y))
	
	// zipWithAxisIterator & zipWithAxis2DIterator
	assert(d1.zipWithAxisIterator.toVector == d1.zipWithAxis.toVector)
	assert(d3.zipWithAxisIterator.toVector == d3.zipWithAxis.toVector)
	assert(d4.zipWithAxisIterator.toVector == d4.zipWithAxis.toVector)
	assert(d1.zipWithAxis2DIterator.toVector == d1.zipWithAxis2D.toVector)
	assert(d4.zipWithAxis2DIterator.toVector == d4.zipWithAxis2D.toVector)
	
	// toMap
	assert(d1.toMap == Map(X -> 10))
	assert(d2.toMap == Map(X -> 10, Y -> 20))
	assert(d3.toMap == Map(X -> 10, Y -> 20, Z -> 30))
	assert(d4.toMap == Map(X -> 10, Y -> 20, Z -> 30))
	
	// isAboutZero & isNotAboutZero
	assert(!d1.isAboutZero && d1.isNotAboutZero)
	assert(!d2.isAboutZero && d2.isNotAboutZero)
	assert(!d3.isAboutZero && d3.isNotAboutZero)
	assert(!d4.isAboutZero && d4.isNotAboutZero)
	assert(factory.zero1D.isAboutZero && !factory.zero1D.isNotAboutZero)
	assert(factory.zero2D.isAboutZero && !factory.zero2D.isNotAboutZero)
	assert(factory.zero3D.isAboutZero && !factory.zero3D.isNotAboutZero)
	assert(factory.fill(4)(0).isAboutZero && !factory.fill(4)(0).isNotAboutZero)
	
	// self
	assert(d1.self eq d1)
	assert(d4.self eq d4)
	
	// empty
	assert(d1.empty.length == 0)
	assert(d4.empty.length == 0)
	
	// zero
	assert(d1.zero == factory(0) && d1.zero.length == 1)
	assert(d2.zero == factory(0, 0) && d2.zero.length == 2)
	assert(d3.zero == factory(0, 0, 0) && d3.zero.length == 3)
	assert(d4.zero == factory(Vector(0, 0, 0, 0)) && d4.zero.length == 4)
	
	// isZero
	assert(!d1.isZero)
	assert(!d4.isZero)
	assert(d1.zero.isZero)
	assert(d4.zero.isZero)
	
	// dimensions
	assert(d1.dimensions eq d1)
	assert(d4.dimensions eq d4)
	
	// length
	assert(d1.length == 1)
	assert(d2.length == 2)
	assert(d3.length == 3)
	assert(d4.length == 4)
	
	// toVector
	assert(d1.toVector == Vector(10))
	assert(d2.toVector == Vector(10, 20))
	assert(d3.toVector == Vector(10, 20, 30))
	assert(d4.toVector == Vector(10, 20, 30, 40))
	
	// toString
	assert(d1.toString == "[10]")
	assert(d2.toString == "[10, 20]")
	assert(d3.toString == "[10, 20, 30]")
	assert(d4.toString == "[10, 20, 30, 40]")
	
	// hashCode
	assert(d1.hashCode == factory(10).hashCode)
	assert(d4.hashCode == factory(Vector(10, 20, 30, 40)).hashCode)
	
	// apply(Int) - bounds handling
	assert(d4(0) == 10)
	assert(d4(1) == 20)
	assert(d4(2) == 30)
	assert(d4(3) == 40)
	assert(d4(-1) == 0) // negative index yields zeroValue
	assert(d4(4) == 0)  // index >= length yields zeroValue
	assert(d1(1) == 0)
	
	// apply(Axis)
	assert(d1(X) == 10 && d1(Y) == 0 && d1(Z) == 0)
	assert(d2(X) == 10 && d2(Y) == 20 && d2(Z) == 0)
	assert(d3(X) == 10 && d3(Y) == 20 && d3(Z) == 30)
	assert(d4(X) == 10 && d4(Y) == 20 && d4(Z) == 30)
	
	// slice
	assert(d4.slice(1, 3) == factory(20, 30))
	assert(d3.slice(0, 2) == factory(10, 20))
	assert(d2.slice(0, 1) == factory(10))
	assert(d1.slice(0, 0) == factory.empty)
	
	// map
	assert(d1.map(_ * 2) == factory(20))
	assert(d2.map(_ * 2) == factory(20, 40))
	assert(d3.map(_ * 2) == factory(20, 40, 60))
	assert(d4.map(_ * 2) == factory(Vector(20, 40, 60, 80)))
	assert(d1.map(_.toString).zeroValue == "0")
	
	// padTo(len, elem)
	assert(d1.padTo(3, 5) == factory(Vector(10, 5, 5)))
	assert(d2.padTo(4, 9) == factory(Vector(10, 20, 9, 9)))
	assert(d4.padTo(2, 9) == d4) // no truncation if length >= target
	
	// padTo(len) (using zeroValue)
	assert(d1.padTo(3) == factory(10, 0, 0))
	assert(d2.padTo(4) == factory(Vector(10, 20, 0, 0)))
	assert(d4.padTo(2) == d4)
	
	// withLength
	assert(d1.withLength(1) eq d1)
	assert(d1.withLength(3) == factory(10, 0, 0))
	assert(d4.withLength(2) == factory(10, 20))
	assert(d4.withLength(4) eq d4)
	assert(d4.withLength(5) == factory(Vector(10, 20, 30, 40, 0)))
	
	// canEqual
	assert(d1.canEqual(d2))
	assert(d1.canEqual(hd1))
	assert(!d1.canEqual("other"))
	
	// sameElements
	assert(d1.sameElements(Vector(10)))
	assert(d1.sameElements(Vector(10, 0, 0))) // trailing zeroes are treated as same
	assert(!d1.sameElements(Vector(10, 1)))
	assert(d2.sameElements(Vector(10, 20)))
	assert(d3.sameElements(Vector(10, 20, 30)))
	assert(d4.sameElements(Vector(10, 20, 30, 40)))
	
	// equals
	assert(d1 == factory(10))
	assert(d1 == factory(10, 0))       // equal despite trailing zeroes
	assert(d1 == factory(Vector(10, 0, 0, 0)))
	assert(d2 == factory(10, 20, 0))
	assert(d3 == factory(Vector(10, 20, 30, 0)))
	assert(d4 == factory(Vector(10, 20, 30, 40)))
	assert(d1 == hd1)                  // equals with HasDimensions
	assert(d4 == hd4)
	assert(d1 != factory(11))
	assert(!d1.equals("test"))
	assert(Dimensions(0)(Vector(1)) != Dimensions(5)(Vector(1))) // different zeroValue
	
	// iteratorOfLength
	assert(d1.iteratorOfLength(1).toVector == Vector(10))
	assert(d1.iteratorOfLength(3).toVector == Vector(10, 0, 0))
	assert(d2.iteratorOfLength(2).toVector == Vector(10, 20))
	assert(d3.iteratorOfLength(3).toVector == Vector(10, 20, 30))
	assert(d4.iteratorOfLength(2).toVector == Vector(10, 20))
	assert(d4.iteratorOfLength(4).toVector == Vector(10, 20, 30, 40))
	assert(d4.iteratorOfLength(5).toVector == Vector(10, 20, 30, 40, 0))
	
	// contains(Dimension)
	assert(d1.contains(Dimension(X, 10, 0)))
	assert(d1.contains(Dimension(Y, 0, 0))) // missing axis is 0
	assert(!d1.contains(Dimension(X, 5, 0)))
	assert(d2.contains(Dimension(Y, 20, 0)))
	assert(d3.contains(Dimension(Z, 30, 0)))
	assert(d4.contains(Dimension(X, 10, 0)))
	assert(d4.contains(Dimension(Y, 20, 0)))
	assert(d4.contains(Dimension(Z, 30, 0)))
	
	// zipIteratorWith
	assert(d1.zipIteratorWith(d3).toVector == Vector((10, 10), (0, 20), (0, 30)))
	assert(d4.zipIteratorWith(d2).toVector == Vector((10, 10), (20, 20), (30, 0), (40, 0)))
	
	// zip
	val zipped13 = d1.zip(d3)
	assert(zipped13.values.toVector == Vector((10, 10), (0, 20), (0, 30)))
	assert(zipped13.zeroValue == (0, 0))
	val zipped42 = d4.zip(d2)
	assert(zipped42.values == Vector((10, 10), (20, 20), (30, 0), (40, 0)))
	
	// pairWith
	val paired13 = d1.pairWith(d3)
	assert(paired13.values.toVector == Vector(Pair(10, 10), Pair(0, 20), Pair(0, 30)))
	assert(paired13.zeroValue == Pair(0, 0))
	val paired42 = d4.pairWith(d2)
	assert(paired42.values == Vector(Pair(10, 10), Pair(20, 20), Pair(30, 0), Pair(40, 0)))
	
	// mergeWith(other)(merge)
	assert(d1.mergeWith(d3)(_ + _) == factory(20, 20, 30))
	assert(d2.mergeWith(d2)(_ * _) == factory(100, 400))
	assert(d4.mergeWith(d2)(_ + _) == factory(Vector(20, 40, 30, 40)))
	assert(d3.mergeWith(hd1)(_ + _) == factory(20, 20, 30)) // with HasDimensions
	
	// mergeWith(other, zero)(merge)
	val mergedCustomZero = d1.mergeWith(d2, "zero") { (a, b) => s"$a+$b" }
	assert(mergedCustomZero.values.toVector == Vector("10+10", "0+20"))
	assert(mergedCustomZero.zeroValue == "zero")
	
	// ~== and !~==
	assert(d1 ~== factory(10, 0, 0))
	assert(d2 ~== factory(10, 20))
	assert(d3 ~== factory(10, 20, 30))
	assert(d4 ~== factory(Vector(10, 20, 30, 40)))
	assert(d4 !~== factory(Vector(10, 20, 30, 41)))
	
	// withDimension(axis, dimension)
	assert(d1.withDimension(X, 15) == factory(15))
	assert(d1.withDimension(Y, 25) == factory(10, 25))
	assert(d1.withDimension(Z, 35) == factory(10, 0, 35))
	assert(d2.withDimension(X, 15) == factory(15, 20))
	assert(d3.withDimension(Y, 25) == factory(10, 25, 30))
	assert(d4.withDimension(Z, 35) == factory(Vector(10, 20, 35, 40)))
	
	// withX, withY, withZ
	assert(d1.withX(99) == factory(99))
	assert(d1.withY(99) == factory(10, 99))
	assert(d1.withZ(99) == factory(10, 0, 99))
	
	assert(d2.withX(99) == factory(99, 20))
	assert(d2.withY(99) == factory(10, 99))
	assert(d2.withZ(99) == factory(10, 20, 99))
	
	assert(d3.withX(99) == factory(99, 20, 30))
	assert(d3.withY(99) == factory(10, 99, 30))
	assert(d3.withZ(99) == factory(10, 20, 99))
	
	assert(d4.withX(99) == factory(Vector(99, 20, 30, 40)))
	assert(d4.withY(99) == factory(Vector(10, 99, 30, 40)))
	assert(d4.withZ(99) == factory(Vector(10, 20, 99, 40)))
	
	// withDimensions(IndexedSeq)
	assert(d1.withDimensions(Vector(1, 2)) == factory(1, 2))
	assert(d4.withDimensions(Vector(1, 2, 3)) == factory(1, 2, 3))
	assert(d2.withDimensions(Vector(1, 2, 3, 4)) == f4)
	
	// withDimensions(Map[Axis, B])
	assert(d1.withDimensions(Map[Axis, Int]()) eq d1)
	assert(d1.withDimensions(Map[Axis, Int](X -> 15)) == factory(15))
	assert(d1.withDimensions(Map[Axis, Int](Y -> 25)) == factory(10, 25))
	assert(d1.withDimensions(Map[Axis, Int](Z -> 35)) == factory(10, 0, 35))
	assert(d2.withDimensions(Map[Axis, Int](X -> 15, Y -> 25)) == factory(15, 25))
	assert(d3.withDimensions(Map[Axis, Int](Y -> 25, Z -> 35)) == factory(10, 25, 35))
	assert(d4.withDimensions(Map[Axis, Int](X -> 15, Z -> 35)) == factory(Vector(15, 20, 35, 40)))
	
	// mapWithZero
	val mappedZero = d4.mapWithZero("z")(_.toString)
	assert(mappedZero.values == Vector("10", "20", "30", "40"))
	assert(mappedZero.zeroValue == "z")
	
	// mapWithAxes
	val mappedAxes1 = d1.mapWithAxes { (v, axis) => s"$v-$axis" }
	assert(mappedAxes1.values.toVector == Vector("10-X"))
	val mappedAxes3 = d3.mapWithAxes { (v, axis) => s"$v-$axis" }
	assert(mappedAxes3.values == Vector("10-X", "20-Y", "30-Z"))
	val mappedAxes4 = d4.mapWithAxes { (v, axis) => s"$v-$axis" }
	assert(mappedAxes4.values == Vector("10-X", "20-Y", "30-Z"))
	
	// mapDimension
	assert(d1.mapDimension(X)(_ + 5) == factory(15))
	assert(d1.mapDimension(Y)(_ + 5) == factory(10, 5)) // d1(Y) is 0, 0 + 5 = 5
	assert(d2.mapDimension(Y)(_ * 2) == factory(10, 40))
	assert(d3.mapDimension(Z)(_ / 3) == factory(10, 20, 10))
	assert(d4.mapDimension(X)(_ + 10) == factory(Vector(20, 20, 30, 40)))
	
	// mapX, mapY, mapZ
	assert(d1.mapX(_ + 5) == factory(15))
	assert(d1.mapY(_ + 5) == factory(10, 5))
	assert(d1.mapZ(_ + 5) == factory(10, 0, 5))
	
	assert(d2.mapX(_ + 5) == factory(15, 20))
	assert(d2.mapY(_ + 5) == factory(10, 25))
	assert(d2.mapZ(_ + 5) == factory(10, 20, 5))
	
	assert(d3.mapX(_ + 5) == factory(15, 20, 30))
	assert(d3.mapY(_ + 5) == factory(10, 25, 30))
	assert(d3.mapZ(_ + 5) == factory(10, 20, 35))
	
	assert(d4.mapX(_ + 5) == factory(Vector(15, 20, 30, 40)))
	assert(d4.mapY(_ + 5) == factory(Vector(10, 25, 30, 40)))
	assert(d4.mapZ(_ + 5) == factory(Vector(10, 20, 35, 40)))
	
	// mapDimensions(IterableOnce[Axis])
	assert(d1.mapDimensions(List(X, Y))(_ + 5) == factory(15, 5))
	assert(d2.mapDimensions(List(X, Y))(_ * 2) == factory(20, 40))
	assert(d3.mapDimensions(List(X, Z))(_ * 2) == factory(20, 20, 60))
	assert(d4.mapDimensions(List(Y, Z))(_ + 1) == factory(Vector(10, 21, 31, 40)))
	
	// mapDimensions(Axis, Axis, Axis*)
	assert(d1.mapDimensions(X, Y)(_ + 5) == factory(15, 5))
	assert(d2.mapDimensions(X, Y)(_ * 2) == factory(20, 40))
	assert(d3.mapDimensions(X, Y, Z)(_ * 10) == factory(100, 200, 300))
	assert(d4.mapDimensions(X, Z)(_ + 5) == factory(Vector(15, 20, 35, 40)))
	
	println("All Dimensions and HasDimensions tests completed successfully!")
}
