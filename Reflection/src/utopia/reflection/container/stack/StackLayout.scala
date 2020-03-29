package utopia.reflection.container.stack

object StackLayout
{
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
}

/**
* These are the different component layouts a stack may have
* @author Mikko Hilpinen
* @since 25.2.2019
**/
sealed trait StackLayout