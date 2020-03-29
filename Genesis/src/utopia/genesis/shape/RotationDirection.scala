package utopia.genesis.shape

/**
* objects of rotation direction are used as enumerations to describe different circular directions
* @author Mikko Hilpinen
* @since 21.11.2018
**/
sealed trait RotationDirection
{
    /**
     * The opposite direction to this one
     */
    def opposite: RotationDirection
    
    /**
     * The sign modifier applied to rotation
     */
    def modifier: Double
}

object RotationDirection
{
    /**
     * The clockwise direction, mathematically negative, programmatically positive
     */
    case object Clockwise extends RotationDirection
    {
        def opposite = Counterclockwise
        
        def modifier = 1.0
    }
    
    /**
     * The counter-clockwise direction, mathematically positive, programmatically negative
     */
    case object Counterclockwise extends RotationDirection
    {
        def opposite = Clockwise
        
        def modifier = -1.0
    }
    
    /**
     * All possible values of this trait / enum
     */
    val values = Vector[RotationDirection](Clockwise, Counterclockwise)
    
    /**
      * @return The positive rotation direction
      */
    def positive = Clockwise
    
    /**
      * @return The negative rotation direction
      */
    def negative = Counterclockwise
}