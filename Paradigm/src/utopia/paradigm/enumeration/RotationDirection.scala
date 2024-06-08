package utopia.paradigm.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.sign.{BinarySigned, Sign}
import utopia.flow.operator.sign.Sign.{Negative, Positive}

/**
* objects of rotation direction are used as enumerations to describe different circular directions
* @author Mikko Hilpinen
* @since Genesis 21.11.2018
**/
sealed trait RotationDirection extends BinarySigned[RotationDirection]
{
    // ABSTRACT --------------------------
    
    /**
     * The opposite direction to this one
     */
    def opposite: RotationDirection
    
    
    // COMPUTED --------------------------
    
    /**
      * The sign modifier applied to rotation
      */
    def modifier = sign.modifier
    
    
    // IMPLEMENTED  ----------------------
    
    override def self: RotationDirection = this
    override def unary_- : RotationDirection = opposite
}

object RotationDirection
{
    // ATTRIBUTES   ---------------------
    
    /**
      * All possible values of this trait / enum
      */
    val values = Pair[RotationDirection](Clockwise, Counterclockwise)
    
    
    // COMPUTED -------------------------
    
    /**
      * @return The positive rotation direction (i.e. Clockwise)
      */
    def positive = Clockwise
    /**
      * @return The negative rotation direction (i.e. Counter-clockwise)
      */
    def negative = Counterclockwise
    
    
    // OTHER    -------------------------
    
    /**
      * @param sign A sign
      * @return A rotation direction with the same sign
      */
    def apply(sign: Sign): RotationDirection = sign match {
        case Positive => positive
        case Negative => negative
    }
    
    
    // NESTED   -------------------------
    
    /**
     * The clockwise direction, mathematically negative, programmatically positive
     */
    case object Clockwise extends RotationDirection
    {
        def opposite = Counterclockwise
        
        override def sign = Positive
    }
    
    /**
     * The counter-clockwise direction, mathematically positive, programmatically negative
     */
    case object Counterclockwise extends RotationDirection
    {
        def opposite = Clockwise
        
        override def sign = Negative
    }
}