package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.range.HasInclusiveOrderedEnds
import utopia.flow.operator.filter.Filter
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.handling.template.Handleable2
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Direction2D, OriginType, RotationDirection}
import utopia.paradigm.enumeration.OriginType.Relative
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.shape.shape2d.area.Area2D

object MouseMoveListener2
{
    // TYPES    ----------------------
    
    /**
      * A filter that processes any type of mouse move events
      */
    type MouseMoveEventFilter = Filter[MouseMoveEventLike[_]]
    
    
    // NESTED   ----------------------
    
    trait MouseMoveFilterableFactory[+E <: MouseMoveEventLike[_], +Repr]
    {
        // ABSTRACT ------------------
        
        /**
          * @param filter A filter to apply
          * @return An item that applies the specified filter
          */
        protected def withFilter(filter: Filter[E]): Repr
        
        
        // COMPUTED ------------------
        
        /**
          * @return An item that only accepts mouse movements going up
          */
        def up = direction(Up)
        /**
          * @return An item that only accepts mouse movements going down
          */
        def down = direction(Down)
        /**
          * @return An item that only accepts mouse movements going left
          */
        def left = direction(Direction2D.Left)
        /**
          * @return An item that only accepts mouse movements going right
          */
        def right = direction(Direction2D.Right)
        
        
        // OTHER    ------------------
        
        /**
          * @param threshold A velocity threshold
          * @return An item that only accepts movements with greater (or equal) velocity than the one specified
          */
        def velocityOver(threshold: LinearVelocity) = withFilter { _.velocity.linear >= threshold }
        /**
          * @param center Center of the targeted range of
          * @param maximumVariance
          * @return
          */
        def directionWithin(center: Angle, maximumVariance: Rotation) = {
            val minimum = center + maximumVariance.counterclockwise
            val maximum = center + maximumVariance.clockwise
            if (minimum > maximum)
                withFilter { e =>
                    val angle = e.transition.direction
                    angle >= minimum || angle <= maximum
                }
            else
                withFilter { e =>
                    val angle = e.transition.direction
                    angle >= minimum && angle <= maximum
                }
        }
        def direction(direction: Direction2D, maximumVariance: Rotation = Rotation.circles(0.2)) =
            directionWithin(Angle(direction), maximumVariance)
        
        def startedOver(area: => Area2D, areaType: OriginType = Relative) =
            withFilter { _.startedOver(area, areaType) }
        def startedOutside(area: => Area2D, areaType: OriginType = Relative) =
            withFilter { _.startedOutside(area, areaType) }
        def entered(area: => Area2D, areaType: OriginType = Relative) = withFilter { _.entered(area, areaType) }
        def exited(area: => Area2D, areaType: OriginType = Relative) = withFilter { _.exited(area, areaType) }
    }
}

/**
 * MouseMoveListeners are interested in receiving mouse move events
 * @author Mikko Hilpinen
 * @since 21.1.2017
 */
trait MouseMoveListener2 extends Handleable2
{
    /**
     * This filter is applied over mouse move events the listener would receive.
      * Only events accepted by this filter should be delivered to this listener.
     */
    def mouseMoveEventFilter: Filter[MouseMoveEvent2]
    
    /**
     * This method is used for informing this listener of new mouse events.
      * This method should only be called for events that are accepted by this listener's event filter.
     * @param event The event that occurred.
     */
    def onMouseMove(event: MouseMoveEvent): Unit
}


