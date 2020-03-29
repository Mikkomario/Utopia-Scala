package utopia.genesis.util

/**
 * This object contains standard drawing depth values
 * @author Mikko Hilpinen
 * @since 28.12.2016
 */
object DepthRange
{
    /**
     * This depth should be used for items that are drawn a top of everything else, no matter the
     * context
     */
    val top = -10000
    /**
     * This depth should be used for HUD elements, which are drawn a top of most visual
     * elements
     */
    val hud = - 6000
    /**
     * This depth should be used for elements which should be drawn above the normal elements
     */
    val foreground = - 3000
    /**
     * This is the most commonly used depth value
     */
    val default = 0
    /**
     * This depth should be used for elements that should appear behind the normal elements
     */
    val behind = 3000
    /**
     * This depth should be used for the background layer
     */
    val background = 6000
    /**
     * This depth should be used for elements that must appear behind everything else, no matter 
     * the context
     */
    val bottom = 10000
}