package utopia.genesis.view

import javax.swing.JFrame
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Toolkit
import java.awt.Dimension

import scala.collection.immutable.HashMap
import java.awt.event.{ComponentAdapter, ComponentEvent}
import java.awt.Component

import utopia.genesis.shape.shape2D.Size
import utopia.genesis.shape.shape2D.Point
import utopia.genesis.shape.shape2D.Bounds

/**
 * This class is used for displaying game contents in a frame. The implementation supports
 * borderless window (fullscreen) and resizing while keeping the aspect ratio.
 * @author Mikko Hilpinen
 * @since 25.12.2016
 */
class MainFrame(initialContent: Component, val originalSize: Size, title: String, 
        borderless: Boolean = false, val usePadding: Boolean = true) extends JFrame
{
    // ATTRIBUTES    ------------
    
    private var _content = initialContent
    
    /**
      * @return The current content displayed in this frame
      */
    def content = _content
    def content_=(newContent: Component) = 
    {
        remove(_content)
        _content = newContent
        add(newContent, BorderLayout.CENTER)
        updateContentSize()
    }
    
    private var paddings: Map[String, JPanel] = HashMap()
    
    
    // INITIAL CODE    ----------
    
    // Sets up the frame
    {
        if (borderless) setUndecorated(true)
        
        setTitle(title)
        setLayout(new BorderLayout())
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        
        getContentPane.setBackground(Color.BLACK)
        setVisible(true)
        
        val insets = getInsets
        setSize(originalSize.width.toInt + insets.left + insets.right,
                originalSize.height.toInt + insets.top + insets.bottom)
                
        setVisible(false)
        add(initialContent, BorderLayout.CENTER)
        
        addComponentListener(new ComponentAdapter
        {
            override def componentResized(e: ComponentEvent) = updateContentSize()
        })
    }
    
    
    // OTHER METHODS    ---------
    
    /**
     * Makes the frame fill the whole screen
     * @param showTaskBar Whether room should be left for the task bar. Defaults to false.
     */
    def setFullScreen(showTaskBar: Boolean = false) = 
    {
        var newSize = Size of Toolkit.getDefaultToolkit.getScreenSize
        var position = Point.origin
        
        if (showTaskBar)
        {
            val insets = Toolkit.getDefaultToolkit.getScreenInsets(getGraphicsConfiguration)
            newSize -= Size of insets
            position = Point(insets.left, insets.top)
        }
        
        setBounds(Bounds(position, newSize).toAwt)
    }
    
    /**
     * Displays the frame
     */
    def display() = setVisible(true)
    
    private def updateContentSize()
    {
        val insets = getInsets
        val actualSize = (Size of getSize()) - (Size of insets)
        
        if (usePadding)
        {
            val scaling = actualSize / originalSize
            
            // Calculates the content size and padding
            if (scaling.x > scaling.y)
            {
                val mainPanelSize = originalSize * scaling.y
                content.setSize(mainPanelSize.toDimension)
                
                val paddingSize = Size((actualSize.width - mainPanelSize.width) / 2, actualSize.height)
                setPadding(paddingSize.toDimension, BorderLayout.WEST, BorderLayout.EAST)
            }
            else if (scaling.y > scaling.x)
            {
                val mainPanelSize = originalSize * scaling.x
                content.setSize(mainPanelSize.toDimension)
                
                val paddingSize = Size(actualSize.width, (actualSize.height - mainPanelSize.height) / 2)
                setPadding(paddingSize.toDimension, BorderLayout.NORTH, BorderLayout.SOUTH)
            }
            else
            {
                content.setSize((originalSize * scaling).toDimension)
                if (paddings.nonEmpty) { paddings = HashMap() }
            }
        }
        else
        {
            content.setSize(actualSize.toDimension)
        }
    }
    
    private def setPadding(size: Dimension, directions: String*)
    {
        // Removes any padding that is not set
        paddings.foreach { case (dir, padding) => if (!directions.contains(dir)) remove(padding) }
        paddings = paddings.filterKeys { directions.contains(_) }
        
        // Modifies / adds the paddings
        for (direction <- directions)
        {
            val padding = paddings.getOrElse(direction, 
            { 
                val newPadding = new JPanel(null)
                add(newPadding, direction)
                paddings += direction -> newPadding
                
                newPadding.setOpaque(true)
                newPadding.setVisible(true)
                newPadding.setBackground(Color.BLACK)
                
                newPadding
            })
            
            padding.setPreferredSize(size)
            padding.setMaximumSize(size)
            padding.setMinimumSize(size)
            padding.setSize(size)
        }
    }
}