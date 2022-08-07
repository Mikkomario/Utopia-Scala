package utopia.paradigm.generic

import utopia.flow.generic.DataType
import utopia.flow.generic.EnvironmentNotSetupException
import utopia.flow.generic.ConversionHandler
import utopia.flow.parse.JsonValueConverter
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line, Point, Size}
import utopia.paradigm.shape.shape3d.Vector3D

/**
 * Vectors are used for representing motion, force and coordinates
 */
object Vector3DType extends DataType("Vector3D", classOf[Vector3D]) with ParadigmDataType
/**
 * Lines are geometric 3D shapes that have a start and an end point
 */
object LineType extends DataType("Line", classOf[Line]) with ParadigmDataType
/**
 * Circles are geometric shapes that have an origin and a radius
 */
object CircleType extends DataType("Circle", classOf[Circle]) with ParadigmDataType
/**
 * Points represent 2 dimensional coordinates
 */
object PointType extends DataType("Point", classOf[Point]) with ParadigmDataType
/**
 * Size represents 2 dimensional widht + height
 */
object SizeType extends DataType("Size", classOf[Size]) with ParadigmDataType
/**
 * Bounds are geometric shapes / areas that have both position and size. Bounds are always aligned on X- and Y axes
 */
object BoundsType extends DataType("Bounds", classOf[Bounds]) with ParadigmDataType

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
        DataType.introduceTypes(Vector3DType, PointType, SizeType, LineType, CircleType)
        ConversionHandler.addCaster(ParadigmValueCaster)
        JsonValueConverter.introduce(ParadigmJsonValueConverter)
    }
}

sealed trait ParadigmDataType
{
    if (!ParadigmDataType.isSetup)
        throw EnvironmentNotSetupException("GenesisDataType.setup() must be called before using this data type.")
}