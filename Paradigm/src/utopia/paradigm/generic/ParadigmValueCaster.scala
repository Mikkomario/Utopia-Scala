package utopia.paradigm.generic

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueCaster
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.enumeration.ConversionReliability.{ContextLoss, Dangerous, DataLoss, MeaningLoss, Perfect}
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.immutable.{Conversion, Model, Value}
import utopia.flow.generic.model.mutable.DataType._
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.angular.{Angle, DirectionalRotation, Rotation}
import utopia.paradigm.color.{Color, Hsl, Rgb}
import ParadigmDataType._
import utopia.flow.generic.model.mutable.DataType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.{LinearAcceleration, LinearVelocity}
import utopia.paradigm.motion.motion2d.{Acceleration2D, Velocity2D}
import utopia.paradigm.motion.motion3d.{Acceleration3D, Velocity3D}
import utopia.paradigm.motion.template.Change
import utopia.paradigm.shape.shape2d._
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.area.polygon.{Polygon, Polygonic}
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
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
        immutable.Conversion(ColorType, IntType, ContextLoss),
        // Conversions to Double
        immutable.Conversion(Vector2DType, DoubleType, DataLoss),
        immutable.Conversion(Vector3DType, DoubleType, DataLoss),
        immutable.Conversion(AngleType, DoubleType, ContextLoss),
        immutable.Conversion(RotationType, DoubleType, ContextLoss),
        immutable.Conversion(LinearVelocityType, DoubleType, ContextLoss),
        immutable.Conversion(LinearAccelerationType, DoubleType, ContextLoss),
        // Conversions to LocalTime
        immutable.Conversion(AngleType, LocalTimeType, MeaningLoss),
        // Conversions to Duration
        immutable.Conversion(RotationType, DurationType, MeaningLoss),
        // Conversions to Vector
        immutable.Conversion(Vector2DType, VectorType, ContextLoss),
        immutable.Conversion(Vector3DType, VectorType, ContextLoss),
        immutable.Conversion(PointType, VectorType, ContextLoss),
        immutable.Conversion(SizeType, VectorType, ContextLoss),
        immutable.Conversion(LineType, VectorType, ContextLoss),
        immutable.Conversion(PolygonType, VectorType, ContextLoss),
        immutable.Conversion(Matrix2DType, VectorType, ContextLoss),
        immutable.Conversion(Matrix3DType, VectorType, ContextLoss),
        immutable.Conversion(RgbType, VectorType, ContextLoss),
        // Conversions to Pair
        immutable.Conversion(Vector2DType, PairType, ContextLoss),
        immutable.Conversion(Vector3DType, PairType, DataLoss),
        immutable.Conversion(PointType, PairType, ContextLoss),
        immutable.Conversion(SizeType, PairType, ContextLoss),
        immutable.Conversion(LineType, PairType, ContextLoss),
        immutable.Conversion(BoundsType, PairType, ContextLoss),
        immutable.Conversion(Matrix2DType, PairType, ContextLoss),
        immutable.Conversion(LinearVelocityType, PairType, ContextLoss),
        immutable.Conversion(Velocity2DType, PairType, ContextLoss),
        immutable.Conversion(Velocity3DType, PairType, ContextLoss),
        immutable.Conversion(LinearAccelerationType, PairType, ContextLoss),
        immutable.Conversion(Acceleration2DType, PairType, ContextLoss),
        immutable.Conversion(Acceleration3DType, PairType, ContextLoss),
        // Conversions to Model
        immutable.Conversion(Vector2DType, ModelType, ContextLoss),
        immutable.Conversion(Vector3DType, ModelType, ContextLoss),
        immutable.Conversion(PointType, ModelType, ContextLoss),
        immutable.Conversion(SizeType, ModelType, ContextLoss),
        immutable.Conversion(LineType, ModelType, ContextLoss),
        immutable.Conversion(CircleType, ModelType, ContextLoss),
        immutable.Conversion(BoundsType, ModelType, ContextLoss),
        immutable.Conversion(LinearVelocityType, ModelType, ContextLoss),
        immutable.Conversion(Velocity2DType, ModelType, ContextLoss),
        immutable.Conversion(Velocity3DType, ModelType, ContextLoss),
        immutable.Conversion(LinearAccelerationType, ModelType, ContextLoss),
        immutable.Conversion(Acceleration2DType, ModelType, ContextLoss),
        immutable.Conversion(Acceleration3DType, ModelType, ContextLoss),
        immutable.Conversion(RgbType, ModelType, ContextLoss),
        immutable.Conversion(HslType, ModelType, ContextLoss),
        // Conversions to Vector2D
        immutable.Conversion(VectorType, Vector2DType, Dangerous),
        immutable.Conversion(PairType, Vector2DType, Dangerous),
        immutable.Conversion(Vector3DType, Vector2DType, DataLoss),
        immutable.Conversion(PointType, Vector2DType, ContextLoss),
        immutable.Conversion(SizeType, Vector2DType, ContextLoss),
        immutable.Conversion(LineType, Vector2DType, DataLoss),
        immutable.Conversion(Velocity2DType, Vector2DType, ContextLoss),
        immutable.Conversion(Acceleration2DType, Vector2DType, ContextLoss),
        immutable.Conversion(ModelType, Vector2DType, Dangerous),
        // Conversions to Vector3D
        immutable.Conversion(VectorType, Vector3DType, Dangerous),
        immutable.Conversion(Vector2DType, Vector3DType, Perfect),
        immutable.Conversion(Velocity3DType, Vector3DType, ContextLoss),
        immutable.Conversion(Acceleration3DType, Vector3DType, ContextLoss),
        immutable.Conversion(ModelType, Vector3DType, Dangerous),
        immutable.Conversion(RgbType, Vector3DType, ContextLoss),
        immutable.Conversion(HslType, Vector3DType, ContextLoss),
        // Conversions to Point
        immutable.Conversion(Vector2DType, PointType, Perfect),
        immutable.Conversion(SizeType, PointType, Perfect),
        immutable.Conversion(BoundsType, PointType, DataLoss),
        immutable.Conversion(ModelType, PointType, Dangerous),
        // Conversions to Size
        immutable.Conversion(Vector2DType, SizeType, Perfect),
        immutable.Conversion(PointType, SizeType, ContextLoss),
        immutable.Conversion(BoundsType, SizeType, DataLoss),
        immutable.Conversion(ModelType, SizeType, Dangerous),
        // Conversions to Line
        immutable.Conversion(PairType, LineType, Dangerous),
        immutable.Conversion(BoundsType, LineType, MeaningLoss),
        immutable.Conversion(Matrix2DType, LineType, MeaningLoss),
        immutable.Conversion(ModelType, LineType, Dangerous),
        // Conversions to Circle
        immutable.Conversion(PairType, CircleType, Dangerous),
        immutable.Conversion(ModelType, CircleType, Dangerous),
        // Conversion to Polygon
        immutable.Conversion(VectorType, PolygonType, Dangerous),
        immutable.Conversion(LineType, PolygonType, MeaningLoss),
        immutable.Conversion(CircleType, PolygonType, DataLoss),
        // Conversions to Bounds
        immutable.Conversion(PairType, BoundsType, Dangerous),
        immutable.Conversion(PolygonType, BoundsType, DataLoss),
        immutable.Conversion(LineType, BoundsType, MeaningLoss),
        immutable.Conversion(CircleType, BoundsType, DataLoss),
        immutable.Conversion(ModelType, BoundsType, Dangerous),
        // Conversions to Matrix2D
        immutable.Conversion(PairType, Matrix2DType, Dangerous),
        immutable.Conversion(LineType, Matrix2DType, ContextLoss),
        immutable.Conversion(BoundsType, Matrix2DType, MeaningLoss),
        immutable.Conversion(Matrix3DType, Matrix2DType, DataLoss),
        immutable.Conversion(RotationType, Matrix2DType, ContextLoss),
        immutable.Conversion(LinearTransformationType, Matrix2DType, DataLoss),
        immutable.Conversion(VectorType, Matrix3DType, Dangerous),
        immutable.Conversion(Matrix2DType, Matrix3DType, Perfect),
        immutable.Conversion(AffineTransformationType, Matrix3DType, DataLoss),
        // Conversions to Angle
        immutable.Conversion(DoubleType, AngleType, Perfect),
        immutable.Conversion(LocalTimeType, AngleType, DataLoss),
        immutable.Conversion(Vector2DType, AngleType, DataLoss),
        immutable.Conversion(Vector3DType, AngleType, MeaningLoss),
        immutable.Conversion(RotationType, AngleType, DataLoss),
        immutable.Conversion(Velocity2DType, AngleType, DataLoss),
        immutable.Conversion(Velocity3DType, AngleType, MeaningLoss),
        immutable.Conversion(Acceleration2DType, AngleType, DataLoss),
        immutable.Conversion(Acceleration3DType, AngleType, MeaningLoss),
        immutable.Conversion(HslType, AngleType, DataLoss),
        immutable.Conversion(StringType, AngleType, Dangerous),
        // Conversions to Rotation
        immutable.Conversion(DoubleType, RotationType, Perfect),
        immutable.Conversion(DurationType, RotationType, ContextLoss),
        immutable.Conversion(AngleType, RotationType, ContextLoss),
        immutable.Conversion(LinearTransformationType, RotationType, DataLoss),
        immutable.Conversion(StringType, RotationType, Dangerous),
        // Conversions to LinearVelocity
        immutable.Conversion(DoubleType, LinearVelocityType, Perfect),
        immutable.Conversion(PairType, LinearVelocityType, Dangerous),
        immutable.Conversion(LinearAccelerationType, LinearVelocityType, ContextLoss),
        immutable.Conversion(Velocity2DType, LinearVelocityType, DataLoss),
        immutable.Conversion(Velocity3DType, LinearVelocityType, DataLoss),
        immutable.Conversion(ModelType, LinearVelocityType, Dangerous),
        // Conversions to Velocity2D
        immutable.Conversion(PairType, Velocity2DType, Dangerous),
        immutable.Conversion(Vector2DType, Velocity2DType, Perfect),
        immutable.Conversion(Velocity3DType, Velocity2DType, DataLoss),
        immutable.Conversion(Acceleration2DType, Velocity2DType, ContextLoss),
        immutable.Conversion(ModelType, Velocity2DType, Dangerous),
        // Conversions to Velocity3D
        immutable.Conversion(PairType, Velocity3DType, Dangerous),
        immutable.Conversion(Vector3DType, Velocity3DType, Perfect),
        immutable.Conversion(Velocity2DType, Velocity3DType, Perfect),
        immutable.Conversion(Acceleration3DType, Velocity3DType, ContextLoss),
        immutable.Conversion(ModelType, Velocity3DType, Dangerous),
        // Conversions to LinearAcceleration
        immutable.Conversion(DoubleType, LinearAccelerationType, Perfect),
        immutable.Conversion(PairType, LinearAccelerationType, Dangerous),
        immutable.Conversion(LinearVelocityType, LinearAccelerationType, ContextLoss),
        immutable.Conversion(Acceleration2DType, LinearAccelerationType, DataLoss),
        immutable.Conversion(Acceleration3DType, LinearAccelerationType, DataLoss),
        immutable.Conversion(ModelType, LinearAccelerationType, Dangerous),
        // Conversions to Acceleration2D
        immutable.Conversion(PairType, Acceleration2DType, Dangerous),
        immutable.Conversion(Vector2DType, Acceleration2DType, Perfect),
        immutable.Conversion(Velocity2DType, Acceleration2DType, ContextLoss),
        immutable.Conversion(Acceleration3DType, Acceleration2DType, DataLoss),
        immutable.Conversion(ModelType, Acceleration2DType, Dangerous),
        // Conversions to Acceleration3D
        immutable.Conversion(PairType, Acceleration3DType, Dangerous),
        immutable.Conversion(Vector3DType, Acceleration3DType, Perfect),
        immutable.Conversion(Velocity3DType, Acceleration3DType, ContextLoss),
        immutable.Conversion(Acceleration2DType, Acceleration3DType, Perfect),
        immutable.Conversion(ModelType, Acceleration3DType, Dangerous),
        // Conversions to LinearTransformation
        immutable.Conversion(RotationType, LinearTransformationType, Perfect),
        immutable.Conversion(AffineTransformationType, LinearTransformationType, DataLoss),
        immutable.Conversion(ModelType, LinearTransformationType, Dangerous),
        // Conversions to AffineTransformation
        immutable.Conversion(LinearTransformationType, AffineTransformationType, Perfect),
        immutable.Conversion(ModelType, AffineTransformationType, Dangerous),
        // Conversions to RGB
        immutable.Conversion(Vector3DType, RgbType, DataLoss),
        immutable.Conversion(HslType, RgbType, Perfect),
        immutable.Conversion(ColorType, RgbType, Perfect),
        immutable.Conversion(ModelType, RgbType, Dangerous),
        // Conversions to HSL
        immutable.Conversion(Vector3DType, HslType, DataLoss),
        immutable.Conversion(AngleType, HslType, ContextLoss),
        immutable.Conversion(RgbType, HslType, Perfect),
        immutable.Conversion(ColorType, HslType, Perfect),
        immutable.Conversion(ModelType, HslType, Dangerous),
        // Conversions to Color
        immutable.Conversion(StringType, ColorType, Dangerous),
        immutable.Conversion(IntType, ColorType, MeaningLoss),
        immutable.Conversion(RgbType, ColorType, Perfect),
        immutable.Conversion(HslType, ColorType, Perfect)
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
        case RotationType => Some(value.getRotation.clockwise.radians)
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
            val rot = value.getRotation.clockwise.circles
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
    private def vectorOf(vectorLike: HasDoubleDimensions): Vector[Value] =
        vectorLike.dimensions.map { x => if (x ~== 0.0) 0.0.toValue else x.toValue }.toVector
    
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
            Some(Pair[Value](b.position: Value, b.size: Value))
        case Matrix2DType => Some(value.getMatrix2D.xyPair.map { v => v })
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
    private def pairOf(vector: HasDoubleDimensions): Pair[Value] = vector.xyPair.map { d => d }
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
        case VectorType => value.tryVectorWith { _.tryDouble }.toOption.map(Vector2D.from)
        case PairType => value.tryPairWith { _.tryDouble }.toOption.map(Vector2D.apply)
        case Vector3DType => Some(value.getVector3D.toVector2D)
        case PointType => Some(value.getPoint.toVector)
        case SizeType => Some(value.getSize.toVector)
        case LineType => Some(value.getLine.vector)
        case Velocity2DType => Some(value.getVelocity2D.perMilliSecond)
        case Acceleration2DType => Some(value.getAcceleration2D.perMilliSecond.perMilliSecond)
        case ModelType => Vector2D(value.getModel).toOption
        case _ => None
    }
    
    private def vector3DOf(value: Value): Option[Vector3D] = value.dataType match {
        case VectorType => value.tryVectorWith { _.tryDouble }.toOption.map(Vector3D.from)
        case Vector2DType => Some(value.getVector2D.toVector3D)
        case Velocity3DType => Some(value.getVelocity3D.perMilliSecond)
        case Acceleration3DType => Some(value.getAcceleration3D.perMilliSecond.perMilliSecond)
        case ModelType => Vector3D(value.getModel).toOption
        case RgbType =>
            val rgb = value.getRgb
            Some(Vector3D(rgb.red, rgb.green, rgb.blue))
        case HslType =>
            val hsl = value.getHsl
            Some(Vector3D.lenDir(hsl.saturation, hsl.hue).withZ(hsl.luminosity - 0.5))
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
        case Matrix2DType => Some(Line(value.getMatrix2D.xyPair.map { _.toPoint }))
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
        case LineType => Some(Polygon(value.getLine.ends.toVector))
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
        case LineType => Some(Matrix2D(value.getLine.ends.map { _.toVector }))
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
        case DoubleType => Some(Angle.radians(value.getDouble))
        case LocalTimeType => Some(Angle.up + Rotation.clockwise.circles(value.getLocalTime.toDuration.toPreciseHours / 12))
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
                s.double.map(Angle.radians)
            else {
                val (numPart, typePart) = s.splitAt(firstLetterIndex)
                typePart.toLowerCase.take(3) match {
                    case "rad" => numPart.trim.double.map(Angle.radians)
                    case "deg" => numPart.trim.double.map(Angle.degrees)
                    case _ => None
                }
            }
        case _ => None
    }
    
    private def rotationOf(value: Value): Option[DirectionalRotation] = value.dataType match {
        case DoubleType => Some(Rotation.clockwise.radians(value.getDouble))
        case DurationType => Some(Rotation.clockwise.circles(value.getDuration.toPreciseHours / 24))
        case AngleType => Some(value.getAngle.toShortestRotation)
        case LinearTransformationType => Some(value.getLinearTransformation.rotation)
        case StringType =>
            val s = value.getString
            val firstLetterIndex = s.indexWhere { _.isLetter }
            if (firstLetterIndex < 0)
                s.double.map(Rotation.clockwise.radians)
            else {
                val (numPart, typePart) = s.splitAt(firstLetterIndex)
                typePart.toLowerCase.take(3) match {
                    case "rad" => numPart.trim.double.map(Rotation.clockwise.radians)
                    case "deg" => numPart.trim.double.map(Rotation.clockwise.degrees)
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
            Some(Hsl(v.direction, v.toVector2D.length, v.z + 0.5))
        case AngleType => Some(Hsl(value.getAngle))
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