package utopia.genesis.generic

import utopia.genesis.shape.Vector3D
import utopia.flow.generic.DataType
import utopia.genesis.shape.shape2D.{Bounds, Circle, Line, Point, Rectangle, Size, Transformation}
import utopia.flow.generic.AnyType
import utopia.flow.generic.EnvironmentNotSetupException
import utopia.flow.generic.ConversionHandler
import utopia.flow.parse.JSONValueConverter

/**
 * Vectors are used for representing motion, force and coordinates
 */
object Vector3DType extends DataType("Vector3D", classOf[Vector3D]) with GenesisDataType
/**
 * Lines are geometric 3D shapes that have a start and an end point
 */
object LineType extends DataType("Line", classOf[Line]) with GenesisDataType
/**
 * Circles are geometric shapes that have an origin and a radius
 */
object CircleType extends DataType("Circle", classOf[Circle]) with GenesisDataType
/**
 * Points represent 2 dimensional coordinates
 */
object PointType extends DataType("Point", classOf[Point]) with GenesisDataType
/**
 * Size represents 2 dimensional widht + height
 */
object SizeType extends DataType("Size", classOf[Size]) with GenesisDataType
/**
 * Bounds are geometric shapes / areas that have both position and size. Bounds are always aligned on X- and Y axes
 */
object BoundsType extends DataType("Bounds", classOf[Bounds]) with GenesisDataType
/**
 * Transformations are used for transforming object states like position and scale
 */
object TransformationType extends DataType("Transformation", classOf[Transformation]) with GenesisDataType

/**
 * This class is used for introducing and managing Genesis-specific data types
 * @author Mikko Hilpinen
 * @since 10.1.2017 
 */
object GenesisDataType
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
        DataType.introduceTypes(Vector3DType, PointType, SizeType, LineType, CircleType, TransformationType)
        ConversionHandler.addCaster(GenesisValueCaster)
        JSONValueConverter.introduce(GenesisJSONValueConverter)
    }
}

sealed trait GenesisDataType
{
    if (!GenesisDataType.isSetup)
        throw EnvironmentNotSetupException("GenesisDataType.setup() must be called before using this data type.")
}