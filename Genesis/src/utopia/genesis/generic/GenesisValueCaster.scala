package utopia.genesis.generic

import utopia.flow.generic.ValueCaster

import scala.collection.immutable.HashSet
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.DataType
import utopia.flow.generic.Conversion
import utopia.genesis.util.Extensions._
import utopia.genesis.generic.GenesisValue._
import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.immutable.Constant
import utopia.genesis.shape.{Vector3D, VectorLike}
import utopia.flow.generic.VectorType
import utopia.flow.generic.ModelType
import utopia.genesis.shape.shape2D.{Bounds, Circle, Line, Point, Size, Transformation}
import utopia.flow.generic.ConversionReliability._
import utopia.flow.generic.ValueConversions._

/**
 * This object handles casting of Genesis-specific data types
 * @author Mikko Hilpinen
 * @since 12.1.2017
 */
object GenesisValueCaster extends ValueCaster
{
    // ATTRIBUTES    --------------
    
    override lazy val conversions = HashSet[Conversion](
        Conversion(Vector3DType, VectorType, PERFECT),
        Conversion(LineType, VectorType, PERFECT),
        Conversion(PointType, VectorType, PERFECT),
        Conversion(SizeType, VectorType, PERFECT),
        Conversion(Vector3DType, ModelType, PERFECT),
        Conversion(PointType, ModelType, PERFECT),
        Conversion(SizeType, ModelType, PERFECT),
        Conversion(LineType, ModelType, PERFECT),
        Conversion(CircleType, ModelType, PERFECT),
        Conversion(BoundsType, ModelType, PERFECT),
        Conversion(TransformationType, ModelType, PERFECT),
        Conversion(PointType, Vector3DType, PERFECT),
        Conversion(SizeType, Vector3DType, PERFECT),
        Conversion(VectorType, Vector3DType, MEANING_LOSS),
        Conversion(ModelType, Vector3DType, MEANING_LOSS),
        Conversion(LineType, Vector3DType, DATA_LOSS),
        Conversion(Vector3DType, PointType, DATA_LOSS),
        Conversion(SizeType, PointType, PERFECT),
        Conversion(VectorType, PointType, MEANING_LOSS),
        Conversion(ModelType, PointType, MEANING_LOSS),
        Conversion(Vector3DType, SizeType, DATA_LOSS),
        Conversion(PointType, SizeType, PERFECT),
        Conversion(VectorType, SizeType, MEANING_LOSS),
        Conversion(ModelType, SizeType, MEANING_LOSS),
        Conversion(BoundsType, LineType, DATA_LOSS),
        Conversion(VectorType, LineType, MEANING_LOSS),
        Conversion(ModelType, LineType, MEANING_LOSS),
        Conversion(ModelType, CircleType, MEANING_LOSS),
        Conversion(LineType, BoundsType, DATA_LOSS),
        Conversion(ModelType, BoundsType, MEANING_LOSS),
        Conversion(ModelType, TransformationType, MEANING_LOSS))
    
    
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
            case TransformationType => transformationOf(value)
            case _ => None
        }
        
        newContent.map { content => new Value(Some(content), toType) }
    }
    
    
    // OTHER METHODS    -----------
    
    private def vectorOf(value: Value): Option[Vector[Value]] = 
    {
        value.dataType match 
        {
            case Vector3DType => Some(vectorOf(value.vector3DOr()))
            case PointType => Some(vectorOf(value.pointOr()))
            case SizeType => Some(vectorOf(value.sizeOr()))
            case LineType => 
                val line = value.lineOr()
                Some(Vector(line.start, line.end))
            case _ => None
        }
    }
    
    private def vectorOf(vectorLike: VectorLike[_]) = vectorLike.dimensions.map {
        x => if (x ~== 0.0) 0.0.toValue else x.toValue }
    
    private def modelOf(value: Value): Option[Model[Constant]] = 
    {
        value.dataType match 
        {
            case Vector3DType => Some(value.vector3DOr().toModel)
            case PointType => Some(value.pointOr().toModel)
            case SizeType => Some(value.sizeOr().toModel)
            case LineType => Some(value.lineOr().toModel)
            case CircleType => Some(value.circleOr().toModel)
            case BoundsType => Some(value.boundsOr().toModel)
            case TransformationType => Some(value.transformationOr().toModel)
            case _ => None
        }
    }
    
    private def vector3DOf(value: Value): Option[Vector3D] = 
    {
        value.dataType match 
        {
            case VectorType => Some(Vector3D(value(0).doubleOr(), value(1).doubleOr(), value(2).doubleOr()))
            case PointType => Some(value.pointOr().toVector)
            case SizeType => Some(value.sizeOr().toVector)
            case ModelType => Vector3D(value.modelOr()).toOption
            case LineType => Some(value.lineOr().vector)
            case _ => None
        }
    }
    
    private def pointOf(value: Value): Option[Point] =
    {
        value.dataType match
        {
            case VectorType => Some(Point(value(0).doubleOr(), value(1).doubleOr()))
            case Vector3DType => Some(value.vector3DOr().toPoint)
            case SizeType => Some(value.sizeOr().toPoint)
            case ModelType => Point(value.modelOr()).toOption
            case _ => None
        }
    }
    
    private def sizeOf(value: Value): Option[Size] =
    {
        value.dataType match
        {
            case VectorType => Some(Size(value(0).doubleOr(), value(1).doubleOr()))
            case Vector3DType => Some(value.vector3DOr().toSize)
            case PointType => Some(value.pointOr().toSize)
            case ModelType => Size(value.modelOr()).toOption
            case _ => None
        }
    }
    
    private def lineOf(value: Value): Option[Line] = 
    {
        value.dataType match 
        {
            case BoundsType => Some(value.boundsOr().diagonal)
            case VectorType => Some(Line(value(0).pointOr(), value(1).pointOr()))
            case ModelType => Line(value.modelOr()).toOption
            case _ => None
        }
    }
    
    private def circleOf(value: Value): Option[Circle] = 
    {
        if (value.dataType isOfType ModelType) Circle(value.modelOr()).toOption else None
    }
    
    private def boundsOf(value: Value): Option[Bounds] = 
    {
        value.dataType match 
        {
            case LineType => Some(Bounds.aroundDiagonal(value.lineOr()))
            case ModelType => Bounds(value.modelOr()).toOption
            case _ => None
        }
    }
    
    private def transformationOf(value: Value): Option[Transformation] = 
    {
        if (value.dataType isOfType ModelType) Transformation(value.modelOr()).toOption else None
    }
}