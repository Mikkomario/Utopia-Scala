package utopia.genesis.view

import javax.swing.JPanel
import utopia.genesis.shape.Vector3D
import utopia.genesis.view.ScalingPolicy.Project
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.{ComponentAdapter, ComponentEvent}

import utopia.genesis.view.ScalingPolicy.Crop
import utopia.genesis.util.{Drawer, FPS}
import utopia.genesis.shape.shape2D.Transformation
import utopia.genesis.shape.shape2D.Size
import utopia.genesis.handling.DrawableHandler

import scala.concurrent.{ExecutionContext, Future}

/**
 * A Canvas works like any Swing panel except it's able to draw drawable object contents with a
 * certain framerate. The panel also rescales itself when the size changes to display only the 
 * specified game world area.
 * @author Mikko Hilpinen
 * @since 28.12.2016
  * @param drawHandler The handler used for drawing this canvas
  * @param originalGameWorldSize The original size of the "game world" displayed in this canvas (in pixels)
  * @param scalingPolicy How this panel handles scaling
  * @param clearPrevious Whether the results of previous draws should be cleared before the next redraw
 */
class Canvas(val drawHandler: DrawableHandler, originalGameWorldSize: Size, val scalingPolicy: ScalingPolicy = Project,
             var clearPrevious: Boolean = true) extends JPanel(null)
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
    
    override def paintComponent(g: Graphics)
    {
        super.paintComponent(g)

        Drawer.use(g)
        {
            drawer =>
                // Clears the previous drawings
                if (clearPrevious)
                {
                    drawer.withCopy
                    {
                        d =>
                            d.graphics.clearRect(0, 0, getWidth, getHeight)
                            d.graphics.setColor(getBackground)
                            d.graphics.fillRect(0, 0, getWidth, getHeight)
                    }
                }
    
                // Game world drawings are scaled, then drawn
                drawHandler.draw(drawer.transformed(Transformation.scaling(scaling)))
        }
    }
    
    
    // OTHER METHODS    --------------
    
    /**
      * Starts asynchronously refreshing this canvas with static intervals
      * @param maxFPS The maximum frames (draws) per second
      * @param context Asynchronous execution context
      */
    def startAutoRefresh(maxFPS: FPS = FPS.default)(implicit context: ExecutionContext): Unit =
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
    def stopAutoRefresh() = refreshLoop.map { _.stop() } getOrElse Future.successful(Unit)
    
    private def updateScaling()
    {
        val size = Size of getSize()
        
        if (scalingPolicy == Project)
        {
            _gameWorldSize = (prefferedGameWorldSize.toVector projectedOver size.toVector).toSize
        }
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