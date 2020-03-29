package utopia.genesis.test

import utopia.genesis.generic.GenesisValue._
import utopia.genesis.shape.{Rotation, Vector3D}
import utopia.genesis.shape.shape2D.{Bounds, Circle, Line, Point, Size, Transformation}
import utopia.genesis.generic.{BoundsType, CircleType, GenesisDataType, LineType, PointType, SizeType, TransformationType, Vector3DType}
import utopia.flow.generic.VectorType
import utopia.flow.generic.ModelType
import utopia.flow.datastructure.immutable.Model

/**
 * This is a unit test for the new data type implementations
 * @author Mikko Hilpinen
 * @since 14.1.2017
 */
object DataTypeTest extends App
{
    GenesisDataType.setup()
    
    val vector1 = Vector3D(1, 1, 1)
    val vector2 = Vector3D(3)
	val point1 = Point(1, 1)
	val point2 = Point(2, 2)
	val size1 = Size(1, 1)
    val line = Line(point1, point2)
    val circle = Circle(point2, 12.25)
    val rectangle = Bounds(point2, size1)
    val transformation = Transformation(vector2, vector2, Rotation.ofRadians(math.Pi), vector1)
    
    val v1 = vector1.toValue
    val v2 = vector2.toValue
	val p1 = point1.toValue
	val p2 = point2.toValue
	val s1 = size1.toValue
    val l = line.toValue
    val c = circle.toValue
    val r = rectangle.toValue
    val t = transformation.toValue
    
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
	assert(p1.vector3DOr() == point1.toVector)
 
	assert(s1.vectorOr().size == 2)
	assert(s1(0).doubleOr() == 1)
	assert(s1(1).doubleOr() == 1)
	assert(s1("width").doubleOr() == 1)
	assert(s1("height").doubleOr() == 1)
	assert(s1.vector3DOr() == size1.toVector)
	
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
    
    assert(t("translation") == v2)
    assert(t("scaling") == v2)
    assert(t("rotation").doubleOr() == math.Pi)
    assert(t("shear") == v1)
    
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
    
    assert(t.castTo(ModelType).get.castTo(TransformationType).get == t)
    
    val model = Model(Vector(("vector", v1), ("Point", p1), ("Size", s1), ("line", l), ("circle", c), ("rectangle", r),
            ("transformation", t)))
    println(model.toJSON)
    
    // Tests JSON parsing
    assert(Vector3D.fromJSON(v1.vector3DOr().toJSON) == v1.vector3D)
	assert(Point.fromJSON(p1.pointOr().toJSON) == p1.point)
	assert(Size.fromJSON(s1.sizeOr().toJSON) == s1.size)
    assert(Line.fromJSON(l.lineOr().toJSON) == l.line)
    assert(Circle.fromJSON(c.circleOr().toJSON) == c.circle)
    assert(Bounds.fromJSON(r.boundsOr().toJSON) == r.bounds)
    assert(Transformation.fromJSON(t.transformationOr().toJSON) == t.transformation)
    
    println("Success")
}