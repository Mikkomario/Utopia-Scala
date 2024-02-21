package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Pair
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.enumeration.OriginType
import utopia.paradigm.enumeration.OriginType.Relative
import utopia.paradigm.shape.shape2d.area.Area2D
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.FiniteDuration

/**
 * Common trait for events that represent mouse movement
 * @author Mikko Hilpinen
 * @since 10.1.2017
 */
trait MouseMoveEventLike[+Repr] extends MouseEvent[Repr]
{
    // ABSTRACT -------------------------
    
    /**
      * @return The previous (first) and the current (second) mouse position
      */
    def positions: Pair[RelativePoint]
    
    /**
      * @return The duration of this movement (based on action events)
      */
    def duration: FiniteDuration
    
    /**
      * @param positions New mouse positions (start & end) to assign
      * @return Copy of this event with the specified positions
      */
    def withPositions(positions: Pair[RelativePoint]): Repr
    
    
    // COMPUTED     -----------
    
    /**
     * The movement vector for the mouse cursor for the duration of the event
     */
    def transition = positions.mapAndMerge { _.relative } { (s, e) => (e - s).toVector2D }
    /**
     * The velocity vector of the mouse cursor (in pixels)
     */
    def velocity = transition.traversedIn(duration)
    
    /**
     * The duration of this event in duration format
     */
    def durationMillis = duration.toPreciseMillis
    
    /**
      * @return The mouse position before this movement
      */
    def previousPosition = positions.first
    @deprecated("Please use .previousPosition instead", "v4.0")
    def previousMousePosition = previousPosition.relative
    
    /**
      * @return Previously recorded absolute mouse position (meaning a position in the screen pixel coordinate system)
      */
    @deprecated("Please use .previousPosition.absolute instead", "v4.0")
    def previousAbsoluteMousePosition = previousPosition.absolute
    
    
    // IMPLEMENTED  ---------------------
    
    override def position: RelativePoint = positions.second
    
    @deprecated("This function only affects the new mouse position. Please use .withPositions(Pair) instead")
    override def withPosition(position: RelativePoint): Repr = withPositions(positions.withSecond(position))
    /**
      * @param f A mapping function for relative mouse coordinates
      * @return Copy of this event with both movement start and movement end positions mapped
      */
    override def mapPosition(f: RelativePoint => RelativePoint) = withPositions(positions.map(f))
    
    
    // OTHER METHODS    -----------------
    
    /**
      * @param area Targeted area
      * @param areaType Whether the specified area is in the relative coordinate space (default),
      *                 or whether it is in the absolute coordinate space.
      * @return Whether the mouse was over the specified area at the start of this movement
      */
    def startedOver(area: Area2D, areaType: OriginType = Relative) = area.contains(previousPosition(areaType))
    /**
     * Checks whether the mouse position was previously over a specified area
     */
    @deprecated("Please use .startedOver(Area2D) instead", "v4.0")
    def wasOverArea(area: Area2D) = startedOver(area)
    /**
      * @param area Targeted area
      * @param areaType Whether the specified area is in the relative coordinate space (default),
      *                 or whether it is in the absolute coordinate space.
      * @return Whether the mouse was outside of the specified area at the start of this movement
      */
    def startedOutside(area: Area2D, areaType: OriginType = Relative) = !startedOver(area, areaType)
    /**
      * @param area Targeted area
      * @param areaType Whether the specified area is in the relative coordinate space (default),
      *                 or whether it is in the absolute coordinate space.
      * @return Whether the mouse entered the specified area during this movement
      */
    def entered(area: Area2D, areaType: OriginType = Relative) =
        startedOutside(area, areaType) && isOver(area, areaType)
    /**
      * @param area Targeted area
      * @param areaType Whether the specified area is in the relative coordinate space (default),
      *                 or whether it is in the absolute coordinate space.
      * @return Whether the mouse exited the specified area during this movement
      */
    def exited(area: Area2D, areaType: OriginType = Relative) = startedOver(area, areaType) && isOutside(area, areaType)
    @deprecated("Please use .entered(Area2D) instead", "v4.0")
    def enteredArea(area: Area2D) = entered(area)
    @deprecated("Please use .exited(Area2D) instead", "v4.0")
    def exitedArea(area: Area2D) = exited(area)
    /**
      * @param area Targeted area
      * @param areaType Whether the specified area is in the relative coordinate space (default),
      *                 or whether it is in the absolute coordinate space.
      * @return Whether the mouse moved into or out from the specified area during this event
      */
    def enteredOrExited(area: Area2D, areaType: OriginType = Relative) =
        positions.isAsymmetricBy { p => area.contains(p(areaType)) }
}