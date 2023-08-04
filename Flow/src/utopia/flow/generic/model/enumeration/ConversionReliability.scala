package utopia.flow.generic.model.enumeration

import utopia.flow.operator.SelfComparable

/**
 * A conversion reliability defines how costly a conversion between two data types is
 * @author Mikko Hilpinen
 * @since 7.11.2016
 */
sealed trait ConversionReliability extends SelfComparable[ConversionReliability]
{
	// ABSTRACT ---------------
	
	/**
	  * @return The numeric cost of this conversion (relative to other reliability type costs)
	  */
	def cost: Int
	
	
    // IMPLEMENTED    -----------
	
	override def self = this
	
	override def compareTo(o: ConversionReliability) = o.cost - this.cost
}

object ConversionReliability
{
    /**
	 * The data type doesn't have to be casted at all, and already represents the target type. 
	 * A cast from an integer to a number would be this kind of operation.
	 */
    case object NoConversion extends ConversionReliability {
	    override val cost = 5
    }
    /**
	 * The data type cast will never fail and preserves the value so that when the value 
	 * is cast back to the original data type, the value would stay equal. A conversion 
	 * from an integer to a double number would be a perfect conversion (1 -> 1.0).
	 */
    case object Perfect extends ConversionReliability {
	    override val cost = 11
    }
	/**
	  * The data type cast will not fail, nor lose significant data, but may be difficult to backtrack. For example,
	  * when converting items to models, no significant data is lost, but it is no longer programmatically easy
	  * to determine which type of item produced the model in the first place.
	  */
	case object ContextLoss extends ConversionReliability {
		override val cost = 23
	}
    /**
	 * The data type cast will never fail, but the value may lose some of its data. The 
	 * remaining data preserves its meaning and will work properly, however.
	 * A conversion from a double number to an integer would be this kind of a conversion
	 * (1.23 -> 1).
	 */
    case object DataLoss extends ConversionReliability {
	    override val cost = 29
    }
    /**
	 * The data type cast will never fail, but the meaning of the data may be lost. A conversion 
	 * from a String representing an integer 2015 to boolean would be this kind of conversion 
	 * ("2015" -> false)
	 */
    case object MeaningLoss extends ConversionReliability {
	    override val cost = 59
    }
    /**
	 * The data type cast may fail, depending from the casted value. The meaning of the 
	 * value may also be lost. A conversion from a String "Jones" to a double would be this kind 
	 * of conversion conversion ("Jones" -> ).
	 */
    case object Dangerous extends ConversionReliability {
	    override val cost = 83
    }
}