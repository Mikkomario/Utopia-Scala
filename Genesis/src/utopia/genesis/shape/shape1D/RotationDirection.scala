package utopia.genesis.shape.shape1D

import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}

/**
* objects of rotation direction are used as enumerations to describe different circular directions
* @author Mikko Hilpinen
* @since 21.11.2018
**/
sealed trait RotationDirection
{
    // ABSTRACT --------------------------
    
    /**
      * @return Sign used with this direction
      */
    def sign: Direction1D
    
    /**
     * The opposite direction to this one
     */
    def opposite: RotationDirection
    
    
    // IMPLEMENTED  ----------------------
    
    /**
     * The sign modifier applied to rotation
     */
    def modifier = sign.modifier
}

object RotationDirection
{
    // ATTRIBUTES   ---------------------
    
    /**
      * All possible values of this trait / enum
      */
    val values = Vector[RotationDirection](Clockwise, Counterclockwise)
    
    
    // COMPUTED -------------------------
    
    /**
      * @return The positive rotation direction
      */
    def positive = Clockwise
    
    /**
      * @return The negative rotation direction
      */
    def negative = Counterclockwise
    
    
    // OTHER    -------------------------
    
    /**
      * @param sign A sign
      * @return A rotation direction with the same sign
      */
    def apply(sign: Direction1D) = sign match
    {
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