package utopia.paradigm.generic

import utopia.flow.generic.{Conversion, DataType, DoubleType, DurationType, IntType, LocalTimeType, ModelType, PairType, StringType, ValueCaster, ValueConvertible, VectorType}
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

import java.time.LocalTime
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

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
        // Conversions to LocalTime
        Conversion(AngleType, LocalTimeType, MEANING_LOSS),
        // Conversions to Duration
        Conversion(RotationType, DurationType, MEANING_LOSS),
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
        // Conversions to Pair
        Conversion(Vector2DType, PairType, CONTEXT_LOSS),
        Conversion(Vector3DType, PairType, DATA_LOSS),
        Conversion(PointType, PairType, CONTEXT_LOSS),
        Conversion(SizeType, PairType, CONTEXT_LOSS),
        Conversion(LineType, PairType, CONTEXT_LOSS),
        Conversion(BoundsType, PairType, CONTEXT_LOSS),
        Conversion(Matrix2DType, PairType, CONTEXT_LOSS),
        Conversion(LinearVelocityType, PairType, CONTEXT_LOSS),
        Conversion(Velocity2DType, PairType, CONTEXT_LOSS),
        Conversion(Velocity3DType, PairType, CONTEXT_LOSS),
        Conversion(LinearAccelerationType, PairType, CONTEXT_LOSS),
        Conversion(Acceleration2DType, PairType, CONTEXT_LOSS),
        Conversion(Acceleration3DType, PairType, CONTEXT_LOSS),
        // Conversions to Model
        Conversion(Vector2DType, ModelType, CONTEXT_LOSS),
        Conversion(Vector3DType, ModelType, CONTEXT_LOSS),
        Conversion(PointType, ModelType, CONTEXT_LOSS),
        Conversion(SizeType, ModelType, CONTEXT_LOSS),
        Conversion(LineType, ModelType, CONTEXT_LOSS),
        Conversion(CircleType, ModelType, CONTEXT_LOSS),
        Conversion(BoundsType, ModelType, CONTEXT_LOSS),
        Conversion(LinearVelocityType, ModelType, CONTEXT_LOSS),
        Conversion(Velocity2DType, ModelType, CONTEXT_LOSS),
        Conversion(Velocity3DType, ModelType, CONTEXT_LOSS),
        Conversion(LinearAccelerationType, ModelType, CONTEXT_LOSS),
        Conversion(Acceleration2DType, ModelType, CONTEXT_LOSS),
        Conversion(Acceleration3DType, ModelType, CONTEXT_LOSS),
        Conversion(RgbType, ModelType, CONTEXT_LOSS),
        Conversion(HslType, ModelType, CONTEXT_LOSS),
        // Conversions to Vector2D
        Conversion(VectorType, Vector2DType, DANGEROUS),
        Conversion(PairType, Vector2DType, DANGEROUS),
        Conversion(Vector3DType, Vector2DType, DATA_LOSS),
        Conversion(PointType, Vector2DType, CONTEXT_LOSS),
        Conversion(SizeType, Vector2DType, CONTEXT_LOSS),
        Conversion(LineType, Vector2DType, DATA_LOSS),
        Conversion(Velocity2DType, Vector2DType, CONTEXT_LOSS),
        Conversion(Acceleration2DType, Vector2DType, CONTEXT_LOSS),
        Conversion(ModelType, Vector2DType, DANGEROUS),
        // Conversions to Vector3D
        Conversion(VectorType, Vector3DType, DANGEROUS),
        Conversion(Vector2DType, Vector3DType, PERFECT),
        Conversion(Velocity3DType, Vector3DType, CONTEXT_LOSS),
        Conversion(Acceleration3DType, Vector3DType, CONTEXT_LOSS),
        Conversion(ModelType, Vector3DType, DANGEROUS),
        Conversion(RgbType, Vector3DType, CONTEXT_LOSS),
        Conversion(HslType, Vector3DType, CONTEXT_LOSS),
        // Conversions to Point
        Conversion(Vector2DType, PointType, PERFECT),
        Conversion(SizeType, PointType, PERFECT),
        Conversion(BoundsType, PointType, DATA_LOSS),
        Conversion(ModelType, PointType, DANGEROUS),
        // Conversions to Size
        Conversion(Vector2DType, SizeType, PERFECT),
        Conversion(PointType, SizeType, CONTEXT_LOSS),
        Conversion(BoundsType, SizeType, DATA_LOSS),
        Conversion(ModelType, SizeType, DANGEROUS),
        // Conversions to Line
        Conversion(PairType, LineType, DANGEROUS),
        Conversion(BoundsType, LineType, MEANING_LOSS),
        Conversion(Matrix2DType, LineType, MEANING_LOSS),
        Conversion(ModelType, LineType, DANGEROUS),
        // Conversions to Circle
        Conversion(PairType, CircleType, DANGEROUS),
        Conversion(ModelType, CircleType, DANGEROUS),
        // Conversion to Polygon
        Conversion(VectorType, PolygonType, DANGEROUS),
        Conversion(LineType, PolygonType, MEANING_LOSS),
        Conversion(CircleType, PolygonType, DATA_LOSS),
        // Conversions to Bounds
        Conversion(PairType, BoundsType, DANGEROUS),
        Conversion(PolygonType, BoundsType, DATA_LOSS),
        Conversion(LineType, BoundsType, MEANING_LOSS),
        Conversion(CircleType, BoundsType, DATA_LOSS),
        Conversion(ModelType, BoundsType, DANGEROUS),
        // Conversions to Matrix2D
        Conversion(PairType, Matrix2DType, DANGEROUS),
        Conversion(LineType, Matrix2DType, CONTEXT_LOSS),
        Conversion(BoundsType, Matrix2DType, MEANING_LOSS),
        Conversion(Matrix3DType, Matrix2DType, DATA_LOSS),
        Conversion(RotationType, Matrix2DType, CONTEXT_LOSS),
        Conversion(LinearTransformationType, Matrix2DType, DATA_LOSS),
        Conversion(VectorType, Matrix3DType, DANGEROUS),
        Conversion(Matrix2DType, Matrix3DType, PERFECT),
        Conversion(AffineTransformationType, Matrix3DType, DATA_LOSS),
        // Conversions to Angle
        Conversion(DoubleType, AngleType, PERFECT),
        Conversion(LocalTimeType, AngleType, DATA_LOSS),
        Conversion(Vector2DType, AngleType, DATA_LOSS),
        Conversion(Vector3DType, AngleType, MEANING_LOSS),
        Conversion(RotationType, AngleType, DATA_LOSS),
        Conversion(Velocity2DType, AngleType, DATA_LOSS),
        Conversion(Velocity3DType, AngleType, MEANING_LOSS),
        Conversion(Acceleration2DType, AngleType, DATA_LOSS),
        Conversion(Acceleration3DType, AngleType, MEANING_LOSS),
        Conversion(HslType, AngleType, DATA_LOSS),
        Conversion(StringType, AngleType, DANGEROUS),
        // Conversions to Rotation
        Conversion(DoubleType, RotationType, PERFECT),
        Conversion(DurationType, RotationType, CONTEXT_LOSS),
        Conversion(AngleType, RotationType, CONTEXT_LOSS),
        Conversion(LinearTransformationType, RotationType, DATA_LOSS),
        Conversion(StringType, RotationType, DANGEROUS),
        // Conversions to LinearVelocity
        Conversion(DoubleType, LinearVelocityType, PERFECT),
        Conversion(PairType, LinearVelocityType, DANGEROUS),
        Conversion(LinearAccelerationType, LinearVelocityType, CONTEXT_LOSS),
        Conversion(Velocity2DType, LinearVelocityType, DATA_LOSS),
        Conversion(Velocity3DType, LinearVelocityType, DATA_LOSS),
        Conversion(ModelType, LinearVelocityType, DANGEROUS),
        // Conversions to Velocity2D
        Conversion(PairType, Velocity2DType, DANGEROUS),
        Conversion(Vector2DType, Velocity2DType, PERFECT),
        Conversion(Velocity3DType, Velocity2DType, DATA_LOSS),
        Conversion(Acceleration2DType, Velocity2DType, CONTEXT_LOSS),
        Conversion(ModelType, Velocity2DType, DANGEROUS),
        // Conversions to Velocity3D
        Conversion(PairType, Velocity3DType, DANGEROUS),
        Conversion(Vector3DType, Velocity3DType, PERFECT),
        Conversion(Velocity2DType, Velocity3DType, PERFECT),
        Conversion(Acceleration3DType, Velocity3DType, CONTEXT_LOSS),
        Conversion(ModelType, Velocity3DType, DANGEROUS),
        // Conversions to LinearAcceleration
        Conversion(DoubleType, LinearAccelerationType, PERFECT),
        Conversion(PairType, LinearAccelerationType, DANGEROUS),
        Conversion(LinearVelocityType, LinearAccelerationType, CONTEXT_LOSS),
        Conversion(Acceleration2DType, LinearAccelerationType, DATA_LOSS),
        Conversion(Acceleration3DType, LinearAccelerationType, DATA_LOSS),
        Conversion(ModelType, LinearAccelerationType, DANGEROUS),
        // Conversions to Acceleration2D
        Conversion(PairType, Acceleration2DType, DANGEROUS),
        Conversion(Vector2DType, Acceleration2DType, PERFECT),
        Conversion(Velocity2DType, Acceleration2DType, CONTEXT_LOSS),
        Conversion(Acceleration3DType, Acceleration2DType, DATA_LOSS),
        Conversion(ModelType, Acceleration2DType, DANGEROUS),
        // Conversions to Acceleration3D
        Conversion(PairType, Acceleration3DType, DANGEROUS),
        Conversion(Vector3DType, Acceleration3DType, PERFECT),
        Conversion(Velocity3DType, Acceleration3DType, CONTEXT_LOSS),
        Conversion(Acceleration2DType, Acceleration3DType, PERFECT),
        Conversion(ModelType, Acceleration3DType, DANGEROUS),
        // Conversions to LinearTransformation
        Conversion(RotationType, LinearTransformationType, PERFECT),
        Conversion(AffineTransformationType, LinearTransformationType, DATA_LOSS),
        Conversion(ModelType, LinearTransformationType, DANGEROUS),
        // Conversions to AffineTransformation
        Conversion(LinearTransformationType, AffineTransformationType, PERFECT),
        Conversion(ModelType, AffineTransformationType, DANGEROUS),
        // Conversions to RGB
        Conversion(Vector3DType, RgbType, DATA_LOSS),
        Conversion(HslType, RgbType, PERFECT),
        Conversion(ColorType, RgbType, PERFECT),
        Conversion(ModelType, RgbType, DANGEROUS),
        // Conversions to HSL
        Conversion(Vector3DType, HslType, DATA_LOSS),
        Conversion(AngleType, HslType, CONTEXT_LOSS),
        Conversion(RgbType, HslType, PERFECT),
        Conversion(ColorType, HslType, PERFECT),
        Conversion(ModelType, HslType, DANGEROUS),
        // Conversions to Color
        Conversion(StringType, ColorType, DANGEROUS),
        Conversion(IntType, ColorType, MEANING_LOSS),
        Conversion(RgbType, ColorType, PERFECT),
        Conversion(HslType, ColorType, PERFECT)
    )
    
    
    // IMPLEMENTED METHODS    -----
    
    override def cast(value: Value, toType: DataType) = 
    {
        val newContent = toType match 
        {
            case IntType => intOf(value)
            case DoubleType => doubleOf(value)
            case LocalTimeType => localTimeOf(value)
            case DurationType => durationOf(value)
            case VectorType => vectorOf(value)
            case PairType => pairOf(value)
            case ModelType => modelOf(value)
            case Vector2DType => vector2DOf(value)
            case Vector3DType => vector3DOf(value)
            case PointType => pointOf(value)
            case SizeType => sizeOf(value)
            case LineType => lineOf(value)
            case CircleType => circleOf(value)
            case PolygonType => polygonOf(value)
            case BoundsType => boundsOf(value)
            case AngleType => angleOf(value)
            case RotationType => rotationOf(value)
            case Matrix2DType => matrix2DOf(value)
            case Matrix3DType => matrix3DOf(value)
            case LinearTransformationType => linearTransformationOf(value)
            case AffineTransformationType => affineTransformationOf(value)
            case LinearVelocityType => linearVelocityOf(value)
            case Velocity2DType => velocity2DOf(value)
            case Velocity3DType => velocity3DOf(value)
            case LinearAccelerationType => linearAccelerationOf(value)
            case Acceleration2DType => acceleration2DOf(value)
            case Acceleration3DType => acceleration3DOf(value)
            case RgbType => rgbOf(value)
            case HslType => hslOf(value)
            case ColorType => colorOf(value)
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
    
    private def localTimeOf(value: Value): Option[LocalTime] = value.dataType match {
        case AngleType =>
            val angle = value.getAngle.degrees
            val clockAngle = if (angle > 270) angle - 270 else angle + 90
            Some(LocalTime.MIDNIGHT + (clockAngle * 2).minutes)
        case _ => None
    }
    
    private def durationOf(value: Value): Option[FiniteDuration] = value.dataType match {
        case RotationType =>
            // Each 360 degree rotation represents 24 hours
            val rot = value.getRotation.clockwiseCircles
            Some((rot * 24).hours)
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
        case PolygonType => Some(value.getPolygon.corners.map { _.toValue }.toVector)
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
        case BoundsType =>
            val b = value.getBounds
            Some(Pair(b.position, b.size))
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
        case HslType =>
            val hsl = value.getHsl
            Some(Vector2D.lenDir(hsl.saturation, hsl.hue).withZ(hsl.luminosity - 0.5))
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
        case LocalTimeType => Some(Angle.up + Rotation.ofCircles(value.getLocalTime.toDuration.toPreciseHours / 12))
        case Vector2DType => Some(value.getVector2D.direction)
        case Vector3DType => Some(value.getVector3D.direction)
        case RotationType => Some(value.getRotation.toAngle)
        case Velocity2DType => Some(value.getVelocity2D.direction)
        case Velocity3DType => Some(value.getVelocity3D.direction)
        case Acceleration2DType => Some(value.getAcceleration2D.direction)
        case Acceleration3DType => Some(value.getAcceleration3D.direction)
        case HslType => Some(value.getHsl.hue)
        case StringType =>
            val s = value.getString
            val firstLetterIndex = s.indexWhere { _.isLetter }
            if (firstLetterIndex < 0)
                s.double.map(Angle.ofRadians)
            else {
                val (numPart, typePart) = s.splitAt(firstLetterIndex)
                typePart.toLowerCase.take(3) match {
                    case "rad" => numPart.trim.double.map(Angle.ofRadians)
                    case "deg" => numPart.trim.double.map(Angle.ofDegrees)
                    case _ => None
                }
            }
        case _ => None
    }
    
    private def rotationOf(value: Value): Option[Rotation] = value.dataType match {
        case DoubleType => Some(Rotation.ofRadians(value.getDouble))
        case DurationType => Some(Rotation.ofCircles(value.getDuration.toPreciseHours / 24))
        case AngleType => Some(value.getAngle.toRotation)
        case LinearTransformationType => Some(value.getLinearTransformation.rotation)
        case StringType =>
            val s = value.getString
            val firstLetterIndex = s.indexWhere { _.isLetter }
            if (firstLetterIndex < 0)
                s.double.map { Rotation.ofRadians(_) }
            else {
                val (numPart, typePart) = s.splitAt(firstLetterIndex)
                typePart.toLowerCase.take(3) match {
                    case "rad" => numPart.trim.double.map { Rotation.ofRadians(_) }
                    case "deg" => numPart.trim.double.map { Rotation.ofDegrees(_) }
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
        case Vector3DType =>
            val v = value.getVector3D
            Some(Hsl(v.direction, v.in2D.length, v.z + 0.5))
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