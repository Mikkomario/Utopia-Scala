package utopia.genesis.view

import utopia.flow.util.AutoClose._
import utopia.genesis.graphics.Drawer3
import utopia.genesis.handling.DrawableHandler2
import utopia.genesis.shape.shape2D.{Bounds, Size}
import utopia.genesis.shape.shape3D.Vector3D
import utopia.genesis.util.Fps
import utopia.genesis.view.ScalingPolicy.{Crop, Project}

import java.awt.{Color, Graphics, Graphics2D}
import java.awt.event.{ComponentAdapter, ComponentEvent}
import javax.swing.JPanel
import scala.concurrent.{ExecutionContext, Future}

/**
 * A Canvas works like any Swing panel except it's able to draw drawable object contents with a
 * certain frame rate. The panel also rescales itself when the size changes to display only the
 * specified game world area.
 * @author Mikko Hilpinen
 * @since 28.12.2016
  * @param drawHandler The handler used for drawing this canvas
  * @param originalGameWorldSize The original size of the "game world" displayed in this canvas (in pixels)
  * @param scalingPolicy How this panel handles scaling
  * @param clearPrevious Whether the results of previous draws should be cleared before the next redraw
 */
class Canvas2(val drawHandler: DrawableHandler2, originalGameWorldSize: Size,
              val scalingPolicy: ScalingPolicy = Project,
              var clearPrevious: Boolean = true)
    extends JPanel(null)
{
    // ATTRIBUTES    -----------------
    
    private var _gameWorldSize = originalGameWorldSize
    
    /**
      * @return The current size of the area displayed in this canvas (pixels)
      */
    def gameWorldSize = _gameWorldSize
    
    private var _prefferedGameWorldSize = originalGameWorldSize
    
    /**
      * @return The preferred game world size of this canvas
      */
    def prefferedGameWorldSize = _prefferedGameWorldSize
    def prefferedGameWorldSize_=(newSize: Size) = 
    {
        _prefferedGameWorldSize = newSize
        updateScaling()
    }
    
    private var _scaling = 1.0
    
    /**
      * @return The current scaling used by this canvas. 1 retains measurements while > 1 enlargens them
      *         and < 1 shrinks them. 0 Doesn't draw at all.
      */
    def scaling = _scaling
    
    private var refreshLoop: Option[RepaintLoop] = None
    
    
    // INITIAL CODE    ---------------
    
    setSize(originalGameWorldSize.toDimension)
    setBackground(Color.WHITE)
    
    // Adds a component adapter that updates scaling whenever this panel is resized
    addComponentListener(new ComponentAdapter
    {
        override def componentResized(e: ComponentEvent) = updateScaling()
    })
    
    
    // IMPLEMENTED METHODS    --------
    
    override def paintComponent(g: Graphics) =
    {
        super.paintComponent(g)

        Drawer3(g.asInstanceOf[Graphics2D]).consume { drawer =>
            // Clears the previous drawings
            if (clearPrevious)
                drawer.clear(Bounds(0, 0, getWidth, getHeight))
    
            // Game world drawings are scaled, then drawn
            drawHandler.draw(drawer.scaled(scaling))
        }
    }
    
    
    // OTHER METHODS    --------------
    
    /**
      * Starts asynchronously refreshing this canvas with static intervals
      * @param maxFPS The maximum frames (draws) per second
      * @param context Asynchronous execution context
      */
    def startAutoRefresh(maxFPS: Fps = Fps.default)(implicit context: ExecutionContext): Unit =
    {
        if (refreshLoop.isEmpty)
        {
            val loop = new RepaintLoop(this, maxFPS)
            loop.registerToStopOnceJVMCloses()
            loop.startAsync()
            refreshLoop = Some(loop)
        }
    }
    
    /**
      * Stops any automatic refresh on this canvas
      */
    def stopAutoRefresh() = refreshLoop.map { _.stop() } getOrElse Future.successful(())
    
    private def updateScaling() =
    {
        val size = Size of getSize()
        
        if (scalingPolicy == Project)
            _gameWorldSize = (prefferedGameWorldSize.toVector projectedOver size.toVector).toSize
        else
        {
            val prefferedXYRatio = prefferedGameWorldSize.width / prefferedGameWorldSize.height
            val newXYRatio = size.width / size.height
            
            val preserveX = if (scalingPolicy == Crop) prefferedXYRatio <= newXYRatio else prefferedXYRatio > newXYRatio
            
            if (preserveX)
            {
                _gameWorldSize = prefferedGameWorldSize * Vector3D(1, size.height / size.width, 1)
            }
            else
            {
                _gameWorldSize = prefferedGameWorldSize * Vector3D(size.width / size.height, 1, 1)
            }
        }
        
        _scaling = (size / gameWorldSize).x
    }
}