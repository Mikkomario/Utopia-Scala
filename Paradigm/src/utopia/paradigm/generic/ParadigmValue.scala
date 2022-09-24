package utopia.paradigm.generic

import utopia.flow.error.DataTypeException
import utopia.flow.generic.model.immutable.Value
import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.color.{Color, Hsl, Rgb}
import utopia.paradigm.motion.motion1d.{LinearAcceleration, LinearVelocity}
import utopia.paradigm.motion.motion2d.{Acceleration2D, Velocity2D}
import utopia.paradigm.motion.motion3d.{Acceleration3D, Velocity3D}
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line, Matrix2D, Point, Polygon, Polygonic, Size, Vector2D}
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.paradigm.transform.{AffineTransformation, LinearTransformation}

object ParadigmValue
{
    implicit class PValue(val v: Value) extends AnyVal
    {
        def vector2D = v.objectValue(Vector2DType).map { _.asInstanceOf[Vector2D] }
        def vector3D = v.objectValue(Vector3DType).map { _.asInstanceOf[Vector3D] }
        def point = v.objectValue(PointType).map { _.asInstanceOf[Point] }
        def size = v.objectValue(SizeType).map { _.asInstanceOf[Size] }
        def line = v.objectValue(LineType).map { _.asInstanceOf[Line] }
        def circle = v.objectValue(CircleType).map { _.asInstanceOf[Circle] }
        def polygon = v.objectValue(PolygonType).map { _.asInstanceOf[Polygonic] }
        def bounds = v.objectValue(BoundsType).map { _.asInstanceOf[Bounds] }
        def angle = v.objectValue(AngleType).map { _.asInstanceOf[Angle] }
        def rotation = v.objectValue(RotationType).map { _.asInstanceOf[Rotation] }
        def matrix2D = v.objectValue(Matrix2DType).map { _.asInstanceOf[Matrix2D] }
        def matrix3D = v.objectValue(Matrix3DType).map { _.asInstanceOf[Matrix3D] }
        def linearTransformation =
            v.objectValue(LinearTransformationType).map { _.asInstanceOf[LinearTransformation] }
        def affineTransformation = v.objectValue(AffineTransformationType).map { _.asInstanceOf[AffineTransformation] }
        def linearVelocity = v.objectValue(LinearVelocityType).map { _.asInstanceOf[LinearVelocity] }
        def velocity2D = v.objectValue(Velocity2DType).map { _.asInstanceOf[Velocity2D] }
        def velocity3D = v.objectValue(Velocity3DType).map { _.asInstanceOf[Velocity3D] }
        def linearAcceleration = v.objectValue(LinearAccelerationType).map { _.asInstanceOf[LinearAcceleration] }
        def acceleration2D = v.objectValue(Acceleration2DType).map { _.asInstanceOf[Acceleration2D] }
        def acceleration3D = v.objectValue(Acceleration3DType).map { _.asInstanceOf[Acceleration3D] }
        def rgb = v.objectValue(RgbType).map { _.asInstanceOf[Rgb] }
        def hsl = v.objectValue(HslType).map { _.asInstanceOf[Hsl] }
        def color = v.objectValue(ColorType).map { _.asInstanceOf[Color] }
        
        def vector2DOr(default: => Vector2D = Vector2D.zero) = vector2D.getOrElse(default)
        def vector3DOr(default: => Vector3D = Vector3D.zero) = vector3D.getOrElse(default)
        def pointOr(default: => Point = Point.origin) = point.getOrElse(default)
        def sizeOr(default: => Size = Size.zero) = size.getOrElse(default)
        def lineOr(default: => Line = Line.zero) = line.getOrElse(default)
        def circleOr(default: => Circle = Circle(Point.origin, 0)) = circle.getOrElse(default)
        def polygonOr(default: => Polygonic = Polygon(Vector())) = polygon.getOrElse(default)
        def boundsOr(default: => Bounds = Bounds.zero) = bounds.getOrElse(default)
        def angleOr(default: => Angle = Angle.zero) = angle.getOrElse(default)
        def rotationOr(default: => Rotation = Rotation.zero) = rotation.getOrElse(default)
        def matrix2DOr(default: => Matrix2D = Matrix2D.identity) = matrix2D.getOrElse(default)
        def matrix3DOr(default: => Matrix3D = Matrix3D.identity) = matrix3D.getOrElse(default)
        def linearTransformationOr(default: => LinearTransformation = LinearTransformation.identity) =
            linearTransformation.getOrElse(default)
        def affineTransformationOr(default: => AffineTransformation = AffineTransformation.identity) =
            affineTransformation.getOrElse(default)
        def linearVelocityOr(default: => LinearVelocity = LinearVelocity.zero) = linearVelocity.getOrElse(default)
        def velocity2DOr(default: => Velocity2D = Velocity2D.zero) = velocity2D.getOrElse(default)
        def velocity3DOr(default: => Velocity3D = Velocity3D.zero) = velocity3D.getOrElse(default)
        def linearAccelerationOr(default: => LinearAcceleration = LinearAcceleration.zero) =
            linearAcceleration.getOrElse(default)
        def acceleration2DOr(default: => Acceleration2D = Acceleration2D.zero) = acceleration2D.getOrElse(default)
        def acceleration3DOr(default: => Acceleration3D = Acceleration3D.zero) = acceleration3D.getOrElse(default)
        def rgbOr(default: => Rgb = Rgb.black) = rgb.getOrElse(default)
        def hslOr(default: => Hsl = Hsl(Angle.zero, 0.0, 0.0)) = hsl.getOrElse(default)
        def colorOr(default: => Color = Color.black) = color.getOrElse(default)
    
        def getVector2D = vector2DOr()
        def getVector3D = vector3DOr()
        def getPoint = pointOr()
        def getSize = sizeOr()
        def getLine = lineOr()
        def getCircle = circleOr()
        def getPolygon = polygonOr()
        def getBounds = boundsOr()
        def getAngle = angleOr()
        def getRotation = rotationOr()
        def getMatrix2D = matrix2DOr()
        def getMatrix3D = matrix3DOr()
        def getLinearTransformation = linearTransformationOr()
        def getAffineTransformation = affineTransformationOr()
        def getLinearVelocity = linearVelocityOr()
        def getVelocity2D = velocity2DOr()
        def getVelocity3D = velocity3DOr()
        def getLinearAcceleration = linearAccelerationOr()
        def getAcceleration2D = acceleration2DOr()
        def getAcceleration3D = acceleration3DOr()
        def getRgb = rgbOr()
        def getHsl = hslOr()
        def getColor = colorOr()
        
        def tryVector2D = getTry(vector2D)("Vector2D")
        def tryVector3D = getTry(vector3D)("Vector3D")
        def tryPoint = getTry(point)("Point")
        def trySize = getTry(size)("Size")
        def tryLine = getTry(line)("Line")
        def tryCircle = getTry(circle)("Circle")
        def tryPolygon = getTry(polygon)("Polygon")
        def tryBounds = getTry(bounds)("Bounds")
        def tryAngle = getTry(angle)("Angle")
        def tryRotation = getTry(rotation)("Rotation")
        def tryMatrix2D = getTry(matrix2D)("Matrix2D")
        def tryMatrix3D = getTry(matrix3D)("Matrix3D")
        def tryLinearTransformation = getTry(linearTransformation)("LinearTransformation")
        def tryAffineTransformation = getTry(affineTransformation)("AffineTransformation")
        def tryLinearVelocity = getTry(linearVelocity)("LinearVelocity")
        def tryVelocity2D = getTry(velocity2D)("Velocity2D")
        def tryVelocity3D = getTry(velocity3D)("Velocity3D")
        def tryLinearAcceleration = getTry(linearAcceleration)("LinearAcceleration")
        def tryAcceleration2D = getTry(acceleration2D)("Acceleration2D")
        def tryAcceleration3D = getTry(acceleration3D)("Acceleration3D")
        def tryRgb = getTry(rgb)("RGB")
        def tryHsl = getTry(hsl)("HSL")
        def tryColor = getTry(color)("Color")
        
        private def getTry[A](a: Option[A])(dataTypeName: => String) =
            a.toTry { DataTypeException(s"${v.description} can't be cast to $dataTypeName") }
    }
}