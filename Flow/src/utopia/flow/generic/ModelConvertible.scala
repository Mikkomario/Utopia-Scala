package utopia.flow.generic

import utopia.flow.datastructure.immutable.Value
import ValueConversions._
import utopia.flow.collection.value.typeless.{Model, Value}
import utopia.flow.parse.JsonConvertible

/**
 * ModelConvertible instances can be represented as model data when necessary
 * @author Mikko Hilpinen
 * @since 23.6.2017
 */
trait ModelConvertible extends JsonConvertible with ValueConvertible
{
    // ABSTRACT METHODS & PROPERTIES    -----------------
    
    /**
     * A model representation of this instance
     */
    def toModel: Model
    
    
    // IMPLEMENTED    ---------------------------
    
    override implicit def toValue: Value = toModel
    
    override def toJson = toModel.toJson
    
    override def toString = toModel.toString
    
    override def appendToJson(jsonBuilder: StringBuilder) = toModel.appendToJson(jsonBuilder)
}