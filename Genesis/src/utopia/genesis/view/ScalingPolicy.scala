package utopia.genesis.view

/**
 * Scaling policies determine how the displayed game world area behaves when the aspect ratio of the
 * view changes.
 * @author Mikko Hilpinen
 * @since 28.12.2016
 */
sealed trait ScalingPolicy

object ScalingPolicy
{
    /**
     * The resulting game world size will always be at least as large as the preferred 
     * game world size.
     */
    case object Extend extends ScalingPolicy
    /**
     * The resulting game world size will at maximum be as large as the preferred game 
     * world size
     */
    case object Crop extends ScalingPolicy
    /**
     * Vector projection is used when determining the actual game world size. The game 
     * world size may be cut horizontally and increased vertically, or vice versa, when 
     * necessary. The total area of the in game world is preserved.
     */
    case object Project extends ScalingPolicy
}