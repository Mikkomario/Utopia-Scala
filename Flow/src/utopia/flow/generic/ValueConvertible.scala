package utopia.flow.generic

import utopia.flow.collection.value.typeless.Value

/**
 * Objects extending this trait can easily be converted / wrapped to values
 * @author Mikko Hilpinen
 * @since 19.6.2017
 */
trait ValueConvertible extends Any
{
    // ABSTRACT METHODS / PROPERTIES    ------------------
    
    /**
     * A value representation of this instance. The casting may be done implicitly.
     */
    implicit def toValue: Value
}