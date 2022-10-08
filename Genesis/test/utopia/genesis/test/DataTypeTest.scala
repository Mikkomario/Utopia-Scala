package utopia.genesis.test

import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line, Point, Size}
import utopia.paradigm.generic.ParadigmDataType.{BoundsType, CircleType, LineType, PointType, SizeType, Vector3DType}
import utopia.paradigm.generic.ParadigmValue._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType.{ModelType, VectorType}
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape3d.Vector3D

/**
 * This is a unit test for the new data type implementations
 * @author Mikko Hilpinen
 * @since 14.1.2017
 */
object DataTypeTest extends App
{
    ParadigmDataType.setup()
    
	private implicit val jsonParser: JsonParser = JsonReader
	
    val vector1 = Vector3D(1, 1, 1)
    val vector2 = Vector3D(3)
	val point1 = Point(1, 1)
	val point2 = Point(2, 2)
	val size1 = Size(1, 1)
    val line = Line(point1, point2)
    val circle = Circle(point2, 12.25)
    val rectangle = Bounds(point2, size1)
    
    val v1 = vector1.toValue
    val v2 = vector2.toValue
	val p1 = point1.toValue
	val p2 = point2.toValue
	val s1 = size1.toValue
    val l = line.toValue
    val c = circle.toValue
    val r = rectangle.toValue
    
    assert(v1.vectorOr().size == 3)
    assert(v1(0).doubleOr() == 1)
    assert(v1(2).doubleOr() == 1)
    assert(v1("x").doubleOr() == 1)
    assert(v1("y").doubleOr() == 1)
    
    assert(v2(0).doubleOr() == 3)
    assert(v2(1).doubleOr() == 0)
    assert(v2("z").doubleOr() == 0)
	
	assert(p1.vectorOr().size == 2)
	assert(p1(0).doubleOr() == 1)
	assert(p1(1).doubleOr() == 1)
	assert(p1("x").doubleOr() == 1)
	assert(p1("y").doubleOr() == 1)
	assert(p1.vector3DOr() == point1.in3D)
 
	assert(s1.vectorOr().size == 2)
	assert(s1(0).doubleOr() == 1)
	assert(s1(1).doubleOr() == 1)
	assert(s1("width").doubleOr() == 1)
	assert(s1("height").doubleOr() == 1)
	assert(s1.vector3DOr() == size1.toVector.in3D)
	
    assert(l.vector3DOr() ~== (point2 - point1).toVector)
    assert(l(0).pointOr() ~== point1)
    assert(l(1).pointOr() ~== point2)
    assert(l("start").pointOr() ~== point1)
    assert(l("end").pointOr() ~== point2)
    
    assert(c("origin") == p2)
    assert(c("radius").doubleOr() == 12.25)
    
    assert(r("position") == p2)
    assert(r("size") == s1)
    // assert(r.vector3DOr() ~== v1.vector3DOr())
    
    assert(v1.castTo(VectorType).get.castTo(Vector3DType).get == v1)
    assert(v1.castTo(ModelType).get.castTo(Vector3DType).get == v1)
	
	assert(p1.castTo(VectorType).get.castTo(PointType).get == p1)
	assert(p1.castTo(ModelType).get.castTo(PointType).get == p1)
	
	assert(s1.castTo(VectorType).get.castTo(SizeType).get == s1)
	assert(s1.castTo(ModelType).get.castTo(SizeType).get == s1)
	
    assert(l.castTo(VectorType).get.castTo(LineType).get == l)
    assert(l.castTo(ModelType).get.castTo(LineType).get == l)
    
    assert(c.castTo(ModelType).get.castTo(CircleType).get == c)
    
    assert(r.castTo(LineType).get.castTo(BoundsType).get == r)
    assert(r.castTo(ModelType).get.castTo(BoundsType).get == r)
    
    val model = Model(Vector(("vector", v1), ("Point", p1), ("Size", s1), ("line", l), ("circle", c), ("rectangle", r)))
    println(model.toJson)
    
    // Tests JSON parsing
	// TODO: Fix these assertions
	/*
    assert(Vector3D.fromJson(v1.vector3DOr().toJson) == v1.vector3D)
	assert(Point.fromJson(p1.pointOr().toJson) == p1.point)
	assert(Size.fromJson(s1.sizeOr().toJson) == s1.size)
    assert(Line.fromJson(l.lineOr().toJson) == l.line)
    assert(Circle.fromJson(c.circleOr().toJson) == c.circle)
    assert(Bounds.fromJson(r.boundsOr().toJson) == r.bounds)
    assert(Transformation.fromJson(t.transformationOr().toJson) == t.transformation)
    */
    println("Success")
}