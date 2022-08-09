package utopia.flow.generic

/**
 * A conversion reliability defines how costly a conversion between two data types is
 * @author Mikko Hilpinen
 * @since 7.11.2016
 * @param cost The cost of this conversion (in some arbitrary relative units)
 */
sealed abstract class ConversionReliability(val cost: Int) extends Ordered[ConversionReliability]
{
    // OPERATORS    -----------
	
	override def compare(that: ConversionReliability) = that.cost - this.cost
	
	
	// OTHER	----------------
	
	def min(other: ConversionReliability) = if (this <= other) this else other
	
	def max(other: ConversionReliability) = if (this >= other) this else other
}

object ConversionReliability
{
    /**
	 * The data type doesn't have to be casted at all, and already represents the target type. 
	 * A cast from an integer to a number would be this kind of operation.
	 */
    case object NO_CONVERSION extends ConversionReliability(5)
    /**
	 * The data type cast will never fail and preserves the value so that when the value 
	 * is cast back to the original data type, the value would stay equal. A conversion 
	 * from an integer to a double number would be a perfect conversion (1 -> 1.0).
	 */
    case object PERFECT extends ConversionReliability(11)
	/**
	  * The data type cast will not fail, nor lose significant data, but may be difficult to backtrack. For example,
	  * when converting items to models, no significant data is lost, but it is no longer programmatically easy
	  * to determine which type of item produced the model in the first place.
	  */
	case object CONTEXT_LOSS extends ConversionReliability(23)
    /**
	 * The data type cast will never fail, but the value may lose some of its data. The 
	 * remaining data preserves its meaning and will work properly, however.
	 * A conversion from a double number to an integer would be a reliable conversion 
	 * (1.23 -> 1).
	 */
    case object DATA_LOSS extends ConversionReliability(29)
    /**
	 * The data type cast will never fail, but the meaning of the data may be lost. A conversion 
	 * from a String representing an integer 2015 to boolean would be this kind of conversion 
	 * ("2015" -> false)
	 */
    case object MEANING_LOSS extends ConversionReliability(59)
    /**
	 * The data type cast may fail, depending from the casted value. The meaning of the 
	 * value may also be lost. A conversion from a String "Jones" to a double would be this kind 
	 * of conversion conversion ("Jones" -> ).
	 */
    case object DANGEROUS extends ConversionReliability(83)
    
    
    // OTHER METHODS    -------------
    
    /**
     * Finds the smaller of the two reliabilities
     * @param first The first reliability
     * @param second The second reliability
     * @return The smallest / weakest of the two reliabilities
     */
	@deprecated("Please use instance level min / max", "v1.4")
    def min(first: ConversionReliability, second: ConversionReliability) = if (first <= second) first else second
    
    /**
     * Finds the stronger of the two reliabilities
     * @param first The first reliability
     * @param second The second reliability
     * @return The largest / strongest of the two reliabilities
     */
	@deprecated("Please use instance level min / max", "v1.4")
    def max(first: ConversionReliability, second: ConversionReliability) = if (first >= second) first else second
}