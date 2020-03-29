package utopia.flow.generic

import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.parse.JSONConvertible

/**
 * ModelConvertible instances can be represented as model data when necessary
 * @author Mikko Hilpinen
 * @since 23.6.2017
 */
trait ModelConvertible extends JSONConvertible
{
    // ABSTRACT METHODS & PROPERTIES    -----------------
    
    /**
     * A model representation of this instance
     */
    def toModel: Model[Constant]
    
    
    // IMPLEMENTED    ---------------------------
    
    override def toJSON = toModel.toJSON
    
    override def toString = toModel.toString
}