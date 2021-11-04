package utopia.flow.generic

import utopia.flow.datastructure.immutable.Model
import utopia.flow.parse.JsonConvertible

/**
 * ModelConvertible instances can be represented as model data when necessary
 * @author Mikko Hilpinen
 * @since 23.6.2017
 */
trait ModelConvertible extends JsonConvertible
{
    // ABSTRACT METHODS & PROPERTIES    -----------------
    
    /**
     * A model representation of this instance
     */
    def toModel: Model
    
    
    // IMPLEMENTED    ---------------------------
    
    override def toJson = toModel.toJson
    
    override def toString = toModel.toString
}