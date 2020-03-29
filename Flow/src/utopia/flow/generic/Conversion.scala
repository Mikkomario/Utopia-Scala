package utopia.flow.generic

/**
 * A conversion contains information about a conversion between two data types, including the 
 * reliability of the conversion
 * @author Mikko Hilpinen
 * @since 7.11.2016
 * @param source The source data type
 * @param target The target data type
 * @param reliability The reliability of the conversion
 */
case class Conversion(val source: DataType, val target: DataType, val reliability: ConversionReliability)
{
    // COMP. PROPERTIES    -------
    
    /**
     * The cost of this conversion in an arbitrary relative unit
     */
    def cost = reliability.cost
    
    
    // IMPLEMENTED METHODS    ----
    
    override def toString = s"conversion from $source to $target ($reliability)"
}