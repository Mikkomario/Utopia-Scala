package utopia.genesis.view

import utopia.flow.parse.AutoClose._
import utopia.genesis.graphics.{Drawer, Priority2}
import utopia.genesis.handling.drawing.{Drawable2, DrawableHandler2, RepaintListener}
import utopia.genesis.view.ScalingPolicy.{Crop, Project}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.{HasMutableSize, Size}

import java.awt.event.{ComponentAdapter, ComponentEvent}
import java.awt.{Color, Graphics, Graphics2D}
import javax.swing.JPanel

/**
 * Visualizes a set of drawable components within an AWT/Swing panel.
  * This panel rescales itself when the size changes to display only the specified view world area.
 * @author Mikko Hilpinen
 * @since 28.12.2016
  * @param originalViewSize The original size of the "view world" displayed in this canvas
  * @param handler The handler whose contents are drawn
  * @param scalingPolicy How this panel handles scaling
  * @param clearPrevious Whether the results of previous draws should be cleared before the next redraw (default = true)
 */
class AwtCanvas(originalViewSize: Size, val handler: DrawableHandler2 = DrawableHandler2.empty,
                val scalingPolicy: ScalingPolicy = Project, clearPrevious: Boolean = true)
    extends JPanel(null)
{
    // ATTRIBUTES    -----------------
    
    private var _viewSize = originalViewSize
    private var _scaling = 1.0
    
    
    // INITIAL CODE ----------------
    
    handler.addRepaintListener(HandlerRepaintListener)
    
    
    // COMPUTED --------------------
    
    def view = ViewArea
    
    @deprecated("Please use .handler instead", "v4.0")
    def drawableHandler = handler
    
    @deprecated("Please use .view.size instead", "v4.0")
    def gameWorldSize = _viewSize
    
    /**
      * @return The preferred game world size of this canvas
      */
    def prefferedGameWorldSize = view.size
    def prefferedGameWorldSize_=(newSize: Size) = view.size = newSize
    
    /**
      * @return The current scaling used by this canvas. 1 retains measurements while > 1 enlarges them
      *         and < 1 shrinks them. 0 Doesn't draw at all.
      */
    def scaling = _scaling
    
    
    // INITIAL CODE    ---------------
    
    setSize(originalViewSize.toDimension)
    setBackground(Color.WHITE)
    
    // Adds a component adapter that updates scaling whenever this panel is resized
    addComponentListener(new ComponentAdapter {
        override def componentResized(e: ComponentEvent) = updateScaling()
    })
    
    
    // IMPLEMENTED METHODS    --------
    
    override def paintComponent(g: Graphics) = {
        super.paintComponent(g)

        Drawer(g.asInstanceOf[Graphics2D]).consume { drawer =>
            // Clears the previous drawings
            if (clearPrevious)
                drawer.clear(Bounds(0, 0, getWidth, getHeight))
    
            // View world drawings are scaled, then drawn
            handler.paintWith(drawer.scaled(scaling))
        }
    }
    
    
    // OTHER    --------------
    
    private def updateScaling() = {
        val size = Size(getSize())
        
        val visibleSize = {
            if (scalingPolicy == Project)
                _viewSize projectedOver size.toVector
            else {
                val prefferedXYRatio = _viewSize.xyPair.merge { _ / _ }
                val newXYRatio = size.width / size.height
                
                val preserveX = if (scalingPolicy == Crop) prefferedXYRatio <= newXYRatio else prefferedXYRatio > newXYRatio
                
                if (preserveX)
                    _viewSize * Vector2D(1, size.height / size.width)
                else
                    _viewSize * Vector2D(size.width / size.height, 1)
            }
        }
        
        _scaling = size.x / visibleSize.x
    }
    
    
    // NESTED   -------------------------
    
    object ViewArea extends HasMutableSize
    {
        override def size: Size = _viewSize
        def size_=(newSize: Size) = {
            _viewSize = newSize
            updateScaling()
        }
    }
    
    private object HandlerRepaintListener extends RepaintListener
    {
        override def repaint(item: Drawable2, subRegion: Option[Bounds], priority: Priority2) = {
            subRegion match {
                case Some(region) => AwtCanvas.this.repaint(region.toAwt)
                case None => AwtCanvas.this.repaint()
            }
        }
    }
}