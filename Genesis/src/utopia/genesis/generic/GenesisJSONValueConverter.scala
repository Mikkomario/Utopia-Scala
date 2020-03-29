package utopia.genesis.generic

import scala.collection.immutable.HashSet
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.DataType
import utopia.flow.parse.ValueConverter
import utopia.genesis.generic.GenesisValue._

/**
 * This JSON value converter handles JSON conversion of Genesis-originated types
 * @author Mikko Hilpinen
 * @since 24.6.2017
 */
object GenesisJSONValueConverter extends ValueConverter[String]
{
    override def supportedTypes = HashSet(Vector3DType, PointType, SizeType, LineType, CircleType, BoundsType, TransformationType)
    
    override def apply(value: Value, dataType: DataType) = 
    {
        dataType match 
        {
            case Vector3DType => value.vector3DOr().toJSON
            case PointType => value.pointOr().toJSON
            case SizeType => value.sizeOr().toJSON
            case LineType => value.lineOr().toJSON
            case CircleType => value.circleOr().toJSON
            case BoundsType => value.boundsOr().toJSON
            case TransformationType => value.transformationOr().toJSON
            case _ => value.stringOr()
        }
    }
}