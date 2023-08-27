package utopia.paradigm.generic

import utopia.flow.generic.casting.ConversionHandler
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType.AnyType
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.color.{Color, Hsl, Rgb}
import utopia.paradigm.motion.motion1d.{LinearAcceleration, LinearVelocity}
import utopia.paradigm.motion.motion2d.{Acceleration2D, Velocity2D}
import utopia.paradigm.motion.motion3d.{Acceleration3D, Velocity3D}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.Polygonic
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.paradigm.transform.{AffineTransformation, LinearTransformation}

/**
 * This class is used for introducing and managing Genesis-specific data types
 * @author Mikko Hilpinen
 * @since Genesis 10.1.2017
 */
object ParadigmDataType
{
    /**
     * Sets up the Genesis-specific data type features, as well as the Flow data type features.
     * This method should be called before using any of the data types introduced in this project.
     */
    def setup() = ConversionHandler.addCaster(ParadigmValueCaster)
    
    object Vector2DType extends ParadigmDataType
    {
        override def name = "Vector2D"
        override lazy val supportedClass = classOf[Vector2D]
        override def superType = Some(AnyType)
    }
    object Vector3DType extends ParadigmDataType
    {
        override def name = "Vector3D"
        override lazy val supportedClass = classOf[Vector3D]
        override def superType = Some(AnyType)
    }
    object LineType extends ParadigmDataType
    {
        override def name = "Line"
        override lazy val supportedClass = classOf[Line]
        override def superType = Some(AnyType)
    }
    object CircleType extends ParadigmDataType
    {
        override def name = "Circle"
        override lazy val supportedClass = classOf[Circle]
        override def superType = Some(AnyType)
    }
    object PointType extends ParadigmDataType
    {
        override def name = "Point"
        override lazy val supportedClass = classOf[Point]
        override def superType = Some(AnyType)
    }
    object SizeType extends ParadigmDataType
    {
        override def name = "Size"
        override lazy val supportedClass = classOf[Size]
        override def superType = Some(AnyType)
    }
    object PolygonType extends ParadigmDataType
    {
        override def name = "Polygon"
        override lazy val supportedClass = classOf[Polygonic]
        override def superType = Some(AnyType)
    }
    object BoundsType extends ParadigmDataType
    {
        override def name = "Bounds"
        override lazy val supportedClass = classOf[Bounds]
        override def superType = Some(PolygonType)
    }
    object Matrix2DType extends ParadigmDataType
    {
        override def name = "Matrix2D"
        override lazy val supportedClass = classOf[Matrix2D]
        override def superType = Some(AnyType)
    }
    object Matrix3DType extends ParadigmDataType
    {
        override def name = "Matrix3D"
        override lazy val supportedClass = classOf[Matrix3D]
        override def superType = Some(AnyType)
    }
    object AngleType extends ParadigmDataType
    {
        override def name = "Angle"
        override lazy val supportedClass = classOf[Angle]
        override def superType = Some(AnyType)
    }
    object RotationType extends ParadigmDataType
    {
        override def name = "Rotation"
        override lazy val supportedClass = classOf[Rotation]
        override def superType = Some(AnyType)
    }
    object LinearVelocityType extends ParadigmDataType
    {
        override def name = "LinearVelocity"
        override lazy val supportedClass = classOf[LinearVelocity]
        override def superType = Some(AnyType)
    }
    object LinearAccelerationType extends ParadigmDataType
    {
        override def name = "LinearAcceleration"
        override lazy val supportedClass = classOf[LinearAcceleration]
        override def superType = Some(AnyType)
    }
    
    object Velocity2DType extends ParadigmDataType
    {
        override def name = "Velocity2D"
        override lazy val supportedClass = classOf[Velocity2D]
        override def superType = Some(AnyType)
    }
    object Acceleration2DType extends ParadigmDataType
    {
        override def name = "Acceleration2D"
        override lazy val supportedClass = classOf[Acceleration2D]
        override def superType = Some(AnyType)
    }
    object Velocity3DType extends ParadigmDataType
    {
        override def name = "Velocity3D"
        override lazy val supportedClass = classOf[Velocity3D]
        override def superType = Some(AnyType)
    }
    object Acceleration3DType extends ParadigmDataType
    {
        override def name = "Acceleration3D"
        override lazy val supportedClass = classOf[Acceleration3D]
        override def superType = Some(AnyType)
    }
    object LinearTransformationType extends ParadigmDataType
    {
        override def name = "LinearTransformation"
        override lazy val supportedClass = classOf[LinearTransformation]
        override def superType = Some(AnyType)
    }
    
    object AffineTransformationType extends ParadigmDataType
    {
        override def name = "AffineTransformation"
        override lazy val supportedClass = classOf[AffineTransformation]
        override def superType = Some(AnyType)
    }
    
    object RgbType extends ParadigmDataType
    {
        override def name = "RGB"
        override lazy val supportedClass = classOf[Rgb]
        override def superType = Some(AnyType)
    }
    object HslType extends ParadigmDataType
    {
        override def name = "HSL"
        override lazy val supportedClass = classOf[Hsl]
        override def superType = Some(AnyType)
    }
    object ColorType extends ParadigmDataType
    {
        override def name = "Color"
        override lazy val supportedClass = classOf[Color]
        override def superType = Some(AnyType)
    }
}

sealed trait ParadigmDataType extends DataType