package utopia.paradigm.generic

import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.DataType
import utopia.flow.parse.ValueConverter
import ParadigmValue._

/**
 * This JSON value converter handles JSON conversion of Genesis-originated types
 * @author Mikko Hilpinen
 * @since Genesis 24.6.2017
 */
object ParadigmJsonValueConverter extends ValueConverter[String]
{
    override def supportedTypes = Set(Vector3DType, PointType, SizeType, LineType, CircleType, BoundsType)
    
    override def apply(value: Value, dataType: DataType) = 
    {
        dataType match 
        {
            case Vector3DType => value.vector3DOr().toJson
            case PointType => value.pointOr().toJson
            case SizeType => value.sizeOr().toJson
            case LineType => value.lineOr().toJson
            case CircleType => value.circleOr().toJson
            case BoundsType => value.boundsOr().toJson
            case _ => value.stringOr()
        }
    }
}