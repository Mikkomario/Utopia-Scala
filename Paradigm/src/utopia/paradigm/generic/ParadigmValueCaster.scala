package utopia.paradigm.generic

import utopia.flow.generic.{Conversion, DataType, DoubleType, IntType, LocalTimeType, ModelType, PairType, StringType, ValueCaster, ValueConvertible, VectorType}
import utopia.flow.datastructure.immutable.{Model, Pair, Value}
import utopia.flow.generic.ConversionReliability._
import utopia.flow.generic.ValueConversions._
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import ParadigmValue._
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.color.{Color, Hsl, Rgb}
import utopia.paradigm.motion.motion1d.{LinearAcceleration, LinearVelocity}
import utopia.paradigm.motion.motion2d.{Acceleration2D, Velocity2D}
import utopia.paradigm.motion.motion3d.{Acceleration3D, Velocity3D}
import utopia.paradigm.motion.template.Change
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line, Matrix2D, Point, Polygon, Polygonic, Size, Vector2D, Vector2DLike}
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.paradigm.shape.template.VectorLike
import utopia.paradigm.transform.{AffineTransformation, LinearTransformation}

import java.util.concurrent.TimeUnit

/**
 * This object handles casting of Genesis-specific data types
 * @author Mikko Hilpinen
 * @since Genesis 12.1.2017
 */
object ParadigmValueCaster extends ValueCaster
{
    // ATTRIBUTES    --------------
    
    private implicit val timeUnit: TimeUnit = TimeUnit.MILLISECONDS
    
    override lazy val conversions = Set[Conversion](
        // Conversions to Int
        Conversion(ColorType, IntType, CONTEXT_LOSS),
        // Conversions to Double
        Conversion(Vector2DType, DoubleType, DATA_LOSS),
        Conversion(Vector3DType, DoubleType, DATA_LOSS),
        Conversion(AngleType, DoubleType, CONTEXT_LOSS),
        Conversion(RotationType, DoubleType, CONTEXT_LOSS),
        Conversion(LinearVelocityType, DoubleType, CONTEXT_LOSS),
        Conversion(LinearAccelerationType, DoubleType, CONTEXT_LOSS),
        // Conversions to Vector
        Conversion(Vector2DType, VectorType, CONTEXT_LOSS),
        Conversion(Vector3DType, VectorType, CONTEXT_LOSS),
        Conversion(PointType, VectorType, CONTEXT_LOSS),
        Conversion(SizeType, VectorType, CONTEXT_LOSS),
        Conversion(LineType, VectorType, CONTEXT_LOSS),
        Conversion(PolygonType, VectorType, CONTEXT_LOSS),
        Conversion(Matrix2DType, VectorType, CONTEXT_LOSS),
        Conversion(Matrix3DType, VectorType, CONTEXT_LOSS),
        Conversion(RgbType, VectorType, CONTEXT_LOSS),
        // TODO: Continue with to Pair, and then downwards
        // Conversions to Model
        Conversion(Vector3DType, ModelType, PERFECT),
        Conversion(PointType, ModelType, PERFECT),
        Conversion(SizeType, ModelType, PERFECT),
        Conversion(LineType, ModelType, PERFECT),
        Conversion(CircleType, ModelType, PERFECT),
        Conversion(BoundsType, ModelType, PERFECT),
        // Conversions to Vector3D
        Conversion(PointType, Vector3DType, PERFECT),
        Conversion(SizeType, Vector3DType, PERFECT),
        Conversion(VectorType, Vector3DType, MEANING_LOSS),
        Conversion(ModelType, Vector3DType, MEANING_LOSS),
        Conversion(LineType, Vector3DType, DATA_LOSS),
        // Conversions to Point
        Conversion(Vector3DType, PointType, DATA_LOSS),
        Conversion(SizeType, PointType, PERFECT),
        Conversion(VectorType, PointType, MEANING_LOSS),
        Conversion(ModelType, PointType, MEANING_LOSS),
        // Conversions to Size
        Conversion(Vector3DType, SizeType, DATA_LOSS),
        Conversion(PointType, SizeType, PERFECT),
        Conversion(VectorType, SizeType, MEANING_LOSS),
        Conversion(ModelType, SizeType, MEANING_LOSS),
        // Conversions to Line
        Conversion(BoundsType, LineType, DATA_LOSS),
        Conversion(VectorType, LineType, MEANING_LOSS),
        Conversion(ModelType, LineType, MEANING_LOSS),
        // Conversions to Circle
        Conversion(ModelType, CircleType, MEANING_LOSS),
        // Conversions to Bounds
        Conversion(LineType, BoundsType, DATA_LOSS),
        Conversion(ModelType, BoundsType, MEANING_LOSS)
    )
    
    
    // IMPLEMENTED METHODS    -----
    
    override def cast(value: Value, toType: DataType) = 
    {
        val newContent = toType match 
        {
            case VectorType => vectorOf(value)
            case ModelType => modelOf(value)
            case Vector3DType => vector3DOf(value)
            case PointType => pointOf(value)
            case SizeType => sizeOf(value)
            case LineType => lineOf(value)
            case CircleType => circleOf(value)
            case BoundsType => boundsOf(value)
            case _ => None
        }
        
        newContent.map { content => new Value(Some(content), toType) }
    }
    
    
    // OTHER METHODS    -----------
    
    private def intOf(value: Value): Option[Int] = value.dataType match {
        case ColorType => Some(value.getRgb.toInt)
        case _ => None
    }
    
    private def doubleOf(value: Value): Option[Double] = value.dataType match {
        case Vector2DType => Some(value.getVector2D.length)
        case Vector3DType => Some(value.getVector3D.length)
        case AngleType => Some(value.getAngle.radians)
        case RotationType => Some(value.getRotation.clockwiseRadians)
        case LinearVelocityType => Some(value.getLinearVelocity.perMilliSecond)
        case LinearAccelerationType => Some(value.getLinearAcceleration.perMilliSecond.perMilliSecond)
        case _ => None
    }
    
    private def vectorOf(value: Value): Option[Vector[Value]] = value.dataType match {
        case Vector2DType => Some(vectorOf(value.getVector2D))
        case Vector3DType => Some(vectorOf(value.getVector3D))
        case PointType => Some(vectorOf(value.getPoint))
        case SizeType => Some(vectorOf(value.getSize))
        case LineType =>
            val line = value.getLine
            Some(Vector[Value](line.start, line.end))
        case PolygonType => Some(value.getPolygon.corners.map { _.toValue })
        case Matrix2DType => Some(value.getMatrix2D.dimensions.map { _.toValue }.toVector)
        case Matrix3DType => Some(value.getMatrix3D.dimensions.toVector.map { _.toValue })
        case RgbType => Some(value.getRgb.toVector.map { r => r })
        case _ => None
    }
    private def vectorOf(vectorLike: VectorLike[_]): Vector[Value] = vectorLike.dimensions.map {
        x => if (x ~== 0.0) 0.0.toValue else x.toValue }.toVector
    
    private def pairOf(value: Value): Option[Pair[Value]] = value.dataType match {
        case Vector2DType => Some(pairOf(value.getVector2D))
        case Vector3DType => Some(pairOf(value.getVector3D))
        case PointType => Some(pairOf(value.getPoint))
        case SizeType => Some(pairOf(value.getSize))
        case LineType =>
            val l = value.getLine
            Some(Pair(l.start, l.end))
        case Matrix2DType => Some(value.getMatrix2D.dimensions.map { v => v })
        case LinearVelocityType =>
            val v = value.getLinearVelocity
            v.duration.finite.map { d => Pair(v.amount, d) }
        case Velocity2DType => pairOf(value.getVelocity2D)
        case Velocity3DType => pairOf(value.getVelocity3D)
        case LinearAccelerationType => pairOf(value.getLinearAcceleration)
        case Acceleration2DType => pairOf(value.getAcceleration2D)
        case Acceleration3DType => pairOf(value.getAcceleration3D)
        case _ => None
    }
    private def pairOf(vector: Vector2DLike[_]): Pair[Value] = vector.dimensions2D.map { d => d }
    private def pairOf[A <: ValueConvertible](change: Change[A, _]) =
        change.duration.finite.map { d => Pair[Value](change.amount.toValue, d) }
    
    private def modelOf(value: Value): Option[Model] = value.dataType match {
        case Vector2DType => Some(value.getVector2D.toModel)
        case Vector3DType => Some(value.getVector3D.toModel)
        case PointType => Some(value.getPoint.toModel)
        case SizeType => Some(value.getSize.toModel)
        case LineType => Some(value.getLine.toModel)
        case CircleType => Some(value.getCircle.toModel)
        case BoundsType => Some(value.getBounds.toModel)
        case LinearVelocityType => Some(value.getLinearVelocity.toModel)
        case Velocity2DType => Some(value.getVelocity2D.toModel)
        case Velocity3DType => Some(value.getVelocity3D.toModel)
        case LinearAccelerationType => Some(value.getLinearAcceleration.toModel)
        case Acceleration2DType => Some(value.getAcceleration2D.toModel)
        case Acceleration3DType => Some(value.getAcceleration3D.toModel)
        case LinearTransformationType => Some(value.getLinearTransformation.toModel)
        case AffineTransformationType => Some(value.getAffineTransformation.toModel)
        case RgbType => Some(value.getRgb.toModel)
        case HslType => Some(value.getHsl.toModel)
        case _ => None
    }
    
    private def vector2DOf(value: Value): Option[Vector2D] = value.dataType match {
        case VectorType => value.tryVectorWith { _.tryDouble }.toOption.map(Vector2D.withDimensions)
        case PairType => value.tryPairWith { _.tryDouble }.toOption.map(Vector2D.apply)
        case Vector3DType => Some(value.getVector3D.in2D)
        case PointType => Some(value.getPoint.toVector)
        case SizeType => Some(value.getSize.toVector)
        case LineType => Some(value.getLine.vector)
        case Velocity2DType => Some(value.getVelocity2D.perMilliSecond)
        case Acceleration2DType => Some(value.getAcceleration2D.perMilliSecond.perMilliSecond)
        case ModelType => Vector2D(value.getModel).toOption
        case _ => None
    }
    
    private def vector3DOf(value: Value): Option[Vector3D] = value.dataType match {
        case VectorType => value.tryVectorWith { _.tryDouble }.toOption.map(Vector3D.withDimensions)
        case Vector2DType => Some(value.getVector2D.in3D)
        case Velocity3DType => Some(value.getVelocity3D.perMilliSecond)
        case Acceleration3DType => Some(value.getAcceleration3D.perMilliSecond.perMilliSecond)
        case ModelType => Vector3D(value.getModel).toOption
        case RgbType =>
            val rgb = value.getRgb
            Some(Vector3D(rgb.red, rgb.green, rgb.blue))
        case _ => None
    }
    
    private def pointOf(value: Value): Option[Point] = value.dataType match {
        case Vector2DType => Some(value.getVector2D.toPoint)
        case SizeType => Some(value.getSize.toPoint)
        case BoundsType => Some(value.getBounds.position)
        case ModelType => Point(value.getModel).toOption
        case _ => None
    }
    
    private def sizeOf(value: Value): Option[Size] = value.dataType match {
        case Vector2DType => Some(value.getVector2D.toSize)
        case PointType => Some(value.getPoint.toSize)
        case BoundsType => Some(value.getBounds.size)
        case ModelType => Size(value.getModel).toOption
        case _ => None
    }
    
    private def lineOf(value: Value): Option[Line] = value.dataType match {
        case PairType => value.tryPairWith { _.tryPoint }.toOption.map(Line.apply)
        case BoundsType => Some(value.getBounds.diagonal)
        case Matrix2DType => Some(Line(value.getMatrix2D.dimensions.map { _.toPoint }))
        case ModelType => Line(value.getModel).toOption
        case _ => None
    }
    
    private def circleOf(value: Value): Option[Circle] = value.dataType match {
        case PairType =>
            val p = value.getPair
            p.first.point.flatMap { origin => p.second.double.map { Circle(origin, _) } }
        case ModelType => Circle(value.getModel).toOption
        case _ => None
    }
    
    private def polygonOf(value: Value): Option[Polygonic] = value.dataType match {
        case VectorType => Some(Polygon(value.getVector.map { _.getPoint }))
        case LineType => Some(Polygon(value.getLine.points.toVector))
        case CircleType => Some(value.getCircle.toPolygon(12))
        case _ => None
    }
    
    private def boundsOf(value: Value): Option[Bounds] = value.dataType match {
        case PairType =>
            val p = value.getPair
            p.first.point.flatMap { position => p.second.size.map { Bounds(position, _) } }
        case PolygonType => Some(value.getPolygon.bounds)
        case LineType => Some(value.getLine.bounds)
        case CircleType => Some(value.getCircle.bounds)
        case ModelType => Bounds(value.getModel).toOption
        case _ => None
    }
    
    private def matrix2DOf(value: Value): Option[Matrix2D] = value.dataType match {
        case PairType => value.tryPairWith { _.tryVector2D }.toOption.map(Matrix2D.apply)
        case LineType => Some(Matrix2D(value.getLine.points.map { _.toVector }))
        case BoundsType =>
            val b = value.getBounds
            Some(Matrix2D(b.position.toVector, b.size.toVector))
        case Matrix3DType => Some(value.getMatrix3D.in2D)
        case RotationType => Some(Matrix2D.rotation(value.getRotation))
        case LinearTransformationType => Some(value.getLinearTransformation.toMatrix)
        case _ => None
    }
    
    private def matrix3DOf(value: Value): Option[Matrix3D] = value.dataType match {
        case VectorType => value.tryVectorWith { _.tryVector3D }.toOption.map { v =>
            Matrix3D(v.headOption.getOrElse(Vector3D.zero), v.getOrElse(1, Vector3D.zero),
                v.getOrElse(2, Vector3D.zero))
        }
        case Matrix2DType => Some(value.getMatrix2D.to3D)
        case AffineTransformationType => Some(value.getAffineTransformation.toMatrix)
        case _ => None
    }
    
    private def angleOf(value: Value): Option[Angle] = value.dataType match {
        case DoubleType => Some(Angle.ofRadians(value.getDouble))
        case LocalTimeType => Some(Angle.up + Rotation.ofCircles(value.getLocalTime.toDuration.toPreciseHours / 24.0))
        case Vector2DType => Some(value.getVector2D.direction)
        case RotationType => Some(value.getRotation.toAngle)
        case HslType => Some(value.getHsl.hue)
        case StringType =>
            val s = value.getString
            val firstLetterIndex = s.indexWhere { _.isLetter }
            if (firstLetterIndex < 0)
                s.double.map(Angle.ofRadians)
            else {
                val (numPart, typePart) = s.splitAt(firstLetterIndex)
                typePart.toLowerCase.take(3) match {
                    case "rad" => numPart.double.map(Angle.ofRadians)
                    case "deg" => numPart.double.map(Angle.ofDegrees)
                    case _ => None
                }
            }
        case _ => None
    }
    
    private def rotationOf(value: Value): Option[Rotation] = value.dataType match {
        case DoubleType => Some(Rotation.ofRadians(value.getDouble))
        case LinearTransformationType => Some(value.getLinearTransformation.rotation)
        case StringType =>
            val s = value.getString
            val firstLetterIndex = s.indexWhere { _.isLetter }
            if (firstLetterIndex < 0)
                s.double.map { Rotation.ofRadians(_) }
            else {
                val (numPart, typePart) = s.splitAt(firstLetterIndex)
                typePart.toLowerCase.take(3) match {
                    case "rad" => numPart.double.map { Rotation.ofRadians(_) }
                    case "deg" => numPart.double.map { Rotation.ofDegrees(_) }
                    case _ => None
                }
            }
        case _ => None
    }
    
    private def linearVelocityOf(value: Value): Option[LinearVelocity] = value.dataType match {
        case DoubleType => Some(LinearVelocity(value.getDouble))
        case PairType =>
            val p = value.getPair
            p.first.double.flatMap { amount => p.second.duration.map { LinearVelocity(amount, _) } }
        case LinearAccelerationType => Some(value.getLinearAcceleration.perMilliSecond)
        case Velocity2DType => Some(value.getVelocity2D.linear)
        case Velocity3DType => Some(value.getVelocity3D.linear)
        case ModelType => LinearVelocity(value.getModel).toOption
        case _ => None
    }
    
    private def velocity2DOf(value: Value): Option[Velocity2D] = value.dataType match {
        case PairType =>
            value.tryTupleWith { _.tryVector2D } { _.tryDuration }.toOption
                .map { case (amount, duration) => Velocity2D(amount, duration) }
        case Vector2DType => Some(Velocity2D(value.getVector2D))
        case Velocity3DType => Some(value.getVelocity3D.in2D)
        case Acceleration2DType => Some(value.getAcceleration2D.perMilliSecond)
        case ModelType => Velocity2D(value.getModel).toOption
        case _ => None
    }
    
    private def velocity3DOf(value: Value): Option[Velocity3D] = value.dataType match {
        case PairType =>
            value.tryTupleWith { _.tryVector3D } { _.tryDuration }.toOption.map { case (a, d) => Velocity3D(a, d) }
        case Vector3DType => Some(Velocity3D(value.getVector3D))
        case Velocity2DType => Some(value.getVelocity2D.in3D)
        case Acceleration3DType => Some(value.getAcceleration3D.perMilliSecond)
        case ModelType => Velocity3D(value.getModel).toOption
        case _ => None
    }
    
    private def linearAccelerationOf(value: Value): Option[LinearAcceleration] = value.dataType match {
        case DoubleType => Some(LinearAcceleration(value.getDouble))
        case PairType =>
            value.tryTupleWith { _.tryLinearVelocity } { _.tryDuration }.toOption
                .map { case (a, d) => LinearAcceleration(a, d) }
        case LinearVelocityType => Some(LinearAcceleration(value.getLinearVelocity, 1.millis))
        case Acceleration2DType => Some(value.getAcceleration2D.linear)
        case Acceleration3DType => Some(value.getAcceleration3D.linear)
        case ModelType => LinearAcceleration(value.getModel).toOption
        case _ => None
    }
    
    private def acceleration2DOf(value: Value): Option[Acceleration2D] = value.dataType match {
        case PairType =>
            value.tryTupleWith { _.tryVelocity2D } { _.tryDuration }.toOption.map { case (a, d) => Acceleration2D(a, d) }
        case Vector2DType => Some(Acceleration2D(value.getVector2D))
        case Velocity2DType => Some(Acceleration2D(value.getVelocity2D, 1.millis))
        case Acceleration3DType => Some(value.getAcceleration3D.in2D)
        case ModelType => Acceleration2D(value.getModel).toOption
        case _ => None
    }
    
    private def acceleration3DOf(value: Value): Option[Acceleration3D] = value.dataType match {
        case PairType =>
            value.tryTupleWith { _.tryVelocity3D } { _.tryDuration }.toOption.map { case (a, d) => Acceleration3D(a, d) }
        case Vector3DType => Some(Acceleration3D(value.getVector3D))
        case Velocity3DType => Some(Acceleration3D(value.getVelocity3D, 1.millis))
        case Acceleration2DType => Some(value.getAcceleration2D.in3D)
        case ModelType => Acceleration3D(value.getModel).toOption
        case _ => None
    }
    
    private def linearTransformationOf(value: Value): Option[LinearTransformation] = value.dataType match {
        case RotationType => Some(LinearTransformation.rotation(value.getRotation))
        case AffineTransformationType => Some(value.getAffineTransformation.linear)
        case ModelType => LinearTransformation(value.getModel).toOption
        case _ => None
    }
    
    private def affineTransformationOf(value: Value): Option[AffineTransformation] = value.dataType match {
        case LinearTransformationType => Some(value.getLinearTransformation.toAffineTransformation)
        case ModelType => AffineTransformation(value.getModel).toOption
        case _ => None
    }
    
    private def rgbOf(value: Value): Option[Rgb] = value.dataType match {
        case Vector3DType =>
            val v = value.getVector3D
            if (v.dimensions.exists { _ > 1 })
                Some(Rgb.withValues(v.x.toInt, v.y.toInt, v.z.toInt))
            else
                Some(Rgb(v.x, v.y, v.z))
        case HslType => Some(value.getHsl.toRGB)
        case ColorType => Some(value.getColor.rgb)
        case ModelType => Rgb(value.getModel).toOption
        case _ => None
    }
    
    private def hslOf(value: Value): Option[Hsl] = value.dataType match {
        case AngleType => Some(Hsl(value.getAngle, 1, 0.5))
        case RgbType => Some(value.getRgb.toHSL)
        case ColorType => Some(value.getColor.hsl)
        case ModelType => Hsl(value.getModel).toOption
        case _ => None
    }
    
    private def colorOf(value: Value): Option[Color] = value.dataType match {
        case StringType => Color.fromHex(value.getString).toOption
        case IntType => Some(Color.fromInt(value.getInt))
        case RgbType => Some(value.getRgb)
        case HslType => Some(value.getHsl)
        case _ => None
    }
}