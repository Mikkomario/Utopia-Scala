package utopia.reflection.test

import java.awt.Color

import javax.swing.JLabel
import utopia.flow.util.TimeExtensions._
import utopia.flow.async.{Loop, ThreadPool}
import utopia.flow.generic.DataType
import utopia.genesis.shape.shape2D.Size
import utopia.reflection.component.stack.StackLeaf
import utopia.reflection.component.swing.{JStackableWrapper, JWrapper}
import utopia.reflection.container.stack.StackHierarchyManager
import utopia.reflection.container.swing.Stack
import utopia.reflection.container.swing.window.Frame
import utopia.reflection.container.swing.window.WindowResizePolicy.Program
import utopia.reflection.localization.{Localizer, NoLocalization}
import utopia.reflection.shape.{StackLength, StackSize}

import scala.concurrent.ExecutionContext

/**
 * This test creates a simple stack and sees whether the components are positioned properly
 * @author Mikko Hilpinen
 * @since 26.3.2019
 */
object StackHierarchyTest extends App
{
    DataType.setup()
    
    implicit val language: String = "en"
    implicit val localizer: Localizer = NoLocalization
    
    private class ChangingWrapper extends JStackableWrapper with StackLeaf
    {
        // ATTRIBUTES   -----------------
        
        val component = new JLabel()
        
        private var currentSize = StackSize.fixed(Size(64, 64))
        private var isBuffed = false
        
        
        // INITIAL CODE -----------------
        
        component.setBackground(Color.RED)
        component.setOpaque(true)
        
        
        // IMPLEMENTED  -----------------
        
        override def updateLayout() = Unit
    
        override def calculatedStackSize =
        {
            println("Requesting up-to-date stack size calculation")
            currentSize
        }
        
        
        // OTHER    ---------------------
        
        def pulse() =
        {
            if (isBuffed)
                currentSize /= 2
            else
                currentSize *= 2
            
            isBuffed = !isBuffed
            revalidate()
        }
    }
    
    // Creates the basic components & wrap as Stackable
    def makeItem() = 
    {
        val item = JWrapper(new JLabel()).withStackSize(StackSize.any(Size(64, 64)))
        item.background = Color.BLUE
        item
    }
    
    // Creates the stack
    private val item = new ChangingWrapper()
    val items = Vector.fill(3)(makeItem()) :+ item
    val stack = Stack.rowWithItems(items, StackLength.fixed(16), StackLength.fixed(16))
    
    stack.background = Color.ORANGE
    
    // Creates the frame
    val frame = Frame.windowed(stack, "Test", Program)
    frame.setToExitOnClose()
    
    // The last item will pulse every second
    implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
    
    val pulseLoop = Loop(1.seconds) { item.pulse() }
    pulseLoop.registerToStopOnceJVMCloses()
    
    // Start the program
    pulseLoop.startAsync()
    StackHierarchyManager.startRevalidationLoop()
    
    frame.isVisible = true
}