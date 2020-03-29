package utopia.conflict.test

import utopia.genesis.color.Color
import utopia.genesis.util.Drawer
import utopia.genesis.event.MouseButtonStateEvent
import utopia.genesis.event.MouseButton
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.handling.mutable.MouseButtonStateListener
import utopia.genesis.handling.{Drawable, MouseMoveListener}
import utopia.genesis.shape.shape2D.{Line, Point, Projectable, Transformation}

/**
 * This object visually displays shape projection on a line drawn by the user
 * @author Mikko Hilpinen
 * @since 5.8.2017
 */
class ProjectionDrawer(val target: Projectable) extends Drawable with MouseButtonStateListener
        with MouseMoveListener
{
    // ATTRIBUTES    ---------------------
    
    private var lastClickPosition = Point.origin
    private var mouseLine = Line.zero
    private var projection = Line.zero
    
    
    // IMPLEMENTED PROPERTIES    ---------
    
    override val mouseButtonStateEventFilter = MouseButtonStateEvent.buttonFilter(MouseButton.Left)
    
    
    // INITIAL CODE    -------------------
    
    // Mouse clicks are always listened while other events are ignored while the mouse is not down
    isReceivingMouseButtonStateEvents = false
    defaultHandlingState = false
    
    
    // IMPLEMENTED METHODS    ------------
    
    override def draw(drawer: Drawer) = 
    {
        drawer.withEdgeColor(Color.gray(0.5)).draw(mouseLine)
        drawer.withEdgeColor(Color.red).draw(projection)
    }
    
    override def onMouseButtonState(event: MouseButtonStateEvent) = 
    {
        if (event.isDown)
        {
            // On mouse press, starts listening to other events again, resets drawn lines
            lastClickPosition = event.mousePosition
            mouseLine = Line.zero
            projection = Line.zero
            
            defaultHandlingState = true
        }
        else
        {
            defaultHandlingState = false
        }
        None
    }
    
    override def onMouseMove(event: MouseMoveEvent) = 
    {
        // Creates a new projection
        mouseLine = Line(lastClickPosition, event.mousePosition)
        projection = Transformation.translation(lastClickPosition.toVector)(target.projectedOver(mouseLine.vector))
    }
}