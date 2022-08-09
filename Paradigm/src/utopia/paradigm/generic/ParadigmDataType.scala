package utopia.paradigm.generic

import utopia.flow.generic.DataType
import utopia.flow.generic.EnvironmentNotSetupException
import utopia.flow.generic.ConversionHandler
import utopia.flow.parse.JsonValueConverter
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.color.{Color, Hsl, Rgb}
import utopia.paradigm.motion.motion1d.{LinearAcceleration, LinearVelocity}
import utopia.paradigm.motion.motion2d.{Acceleration2D, Velocity2D}
import utopia.paradigm.motion.motion3d.{Acceleration3D, Velocity3D}
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line, Matrix2D, Point, Polygonic, Size, Vector2D}
import utopia.paradigm.shape.shape3d.{Matrix3D, Vector3D}
import utopia.paradigm.transform.{AffineTransformation, LinearTransformation}

object Vector2DType extends DataType("Vector2D", classOf[Vector2D]) with ParadigmDataType
object Vector3DType extends DataType("Vector3D", classOf[Vector3D]) with ParadigmDataType
object LineType extends DataType("Line", classOf[Line]) with ParadigmDataType
object CircleType extends DataType("Circle", classOf[Circle]) with ParadigmDataType
object PointType extends DataType("Point", classOf[Point]) with ParadigmDataType
object SizeType extends DataType("Size", classOf[Size]) with ParadigmDataType
object PolygonType extends DataType("Polygon", classOf[Polygonic]) with ParadigmDataType
object BoundsType extends DataType("Bounds", classOf[Bounds], Some(PolygonType)) with ParadigmDataType
object Matrix2DType extends DataType("Matrix2D", classOf[Matrix2D]) with ParadigmDataType
object Matrix3DType extends DataType("Matrix3D", classOf[Matrix3D]) with ParadigmDataType
object AngleType extends DataType("Angle", classOf[Angle]) with ParadigmDataType
object RotationType extends DataType("Rotation", classOf[Rotation]) with ParadigmDataType
object LinearVelocityType extends DataType("LinearVelocity", classOf[LinearVelocity]) with ParadigmDataType
object LinearAccelerationType extends DataType("LinearAcceleration", classOf[LinearAcceleration])
    with ParadigmDataType
object Velocity2DType extends DataType("Velocity2D", classOf[Velocity2D]) with ParadigmDataType
object Acceleration2DType extends DataType("Acceleration2D", classOf[Acceleration2D]) with ParadigmDataType
object Velocity3DType extends DataType("Velocity3D", classOf[Velocity3D]) with ParadigmDataType
object Acceleration3DType extends DataType("Acceleration3D", classOf[Acceleration3D]) with ParadigmDataType
object LinearTransformationType extends DataType("LinearTransformation", classOf[LinearTransformation])
    with ParadigmDataType
object AffineTransformationType extends DataType("AffineTransformation", classOf[AffineTransformation])
    with ParadigmDataType
object RgbType extends DataType("RGB", classOf[Rgb]) with ParadigmDataType
object HslType extends DataType("HSL", classOf[Hsl]) with ParadigmDataType
object ColorType extends DataType("Color", classOf[Color]) with ParadigmDataType

/**
 * This class is used for introducing and managing Genesis-specific data types
 * @author Mikko Hilpinen
 * @since Genesis 10.1.2017
 */
object ParadigmDataType
{
    private var isSetup = false
    
    /**
     * Sets up the Genesis-specific data type features, as well as the Flow data type features.
     * This method should be called before using any of the data types introduced in this project.
     */
    def setup() =
    {
        isSetup = true
        
        DataType.setup()
        DataType.introduceTypes(Vector2DType, Vector3DType, PointType, SizeType, LineType, CircleType, PolygonType,
            BoundsType, AngleType, RotationType, Matrix2DType, Matrix3DType, LinearTransformationType,
            AffineTransformationType, LinearVelocityType, LinearAccelerationType, Velocity2DType, Acceleration2DType,
            Velocity3DType, Acceleration3DType, RgbType, HslType, ColorType)
        ConversionHandler.addCaster(ParadigmValueCaster)
    }
}

sealed trait ParadigmDataType
{
    if (!ParadigmDataType.isSetup)
        throw EnvironmentNotSetupException("GenesisDataType.setup() must be called before using this data type.")
}