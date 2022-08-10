package utopia.reflection.container.stack

import utopia.paradigm.enumeration.LinearAlignment
import utopia.paradigm.enumeration.LinearAlignment.{Close, Far, Middle}

object StackLayout
{
    // NESTED   -----------------------
    
    /**
     * Leading layout where components are placed left / top
     */
    case object Leading extends StackLayout
    /**
     * Center layout where components are placed at the stack center
     */
    case object Center extends StackLayout
    /**
     * Trailing layout where components are placed right / bottom
     */
    case object Trailing extends StackLayout
    /**
     * Fit layout where components fill the stack area, if possible
     */
    case object Fit extends StackLayout
    
    
    // OTHER    -----------------------
    
    /**
      * @param alignment An alignment that determines item movement
      * @return A stack layout that matches that alignment (Leading, Center or Trailing)
      */
    def aligning(alignment: LinearAlignment) = alignment match {
        case Close => Leading
        case Middle => Center
        case Far => Trailing
    }
}

/**
* These are the different component layouts a stack may have
* @author Mikko Hilpinen
* @since 25.2.2019
**/
sealed trait StackLayout