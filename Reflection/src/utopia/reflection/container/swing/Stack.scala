package utopia.reflection.container.swing

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.color.Color
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.stack.{CachingStackable, Stackable}
import utopia.reflection.component.swing.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.container.stack.StackLayout.Fit
import utopia.reflection.container.stack.{StackLayout, StackLike}
import utopia.reflection.shape.StackLength
import utopia.reflection.util.ComponentContext

import scala.collection.immutable.VectorBuilder

object Stack
{
    // TYPES    --------------------
    
    type AwtStackable = Stackable with AwtComponentRelated
    
    
    // ATTRIBUTES   ----------------
    
    private val defaultMargin = StackLength.any
    private val defaultCap = StackLength.fixed(0)
    
    
    // OTHER    --------------------
    
    /**
      * Creates a new stack with a set of items to begin with
      * @param items Items to be added to stack
      * @param direction Direction of the stack
      * @param margin Margin between items
      * @param cap Cap at each end of the stack (default = no cap)
      * @param layout Stack layout (default = Fit)
      * @tparam C The type of items in the stack
      * @return A new stack
      */
    def withItems[C <: AwtStackable](items: TraversableOnce[C], direction: Axis2D, margin: StackLength = defaultMargin,
                                     cap: StackLength = defaultCap, layout: StackLayout = Fit) =
    {
        val stack = new Stack[C](direction, margin, cap, layout)
        stack ++= items
        stack
    }
    
    /**
      * Creates a new horizontal stack
      * @param margin The margin between items
      * @param cap The cap at each end of this stack (default = no cap)
      * @param layout The layout used (default = Fit)
      * @tparam C The type of items in this stack
      * @return A new stack
      */
    def row[C <: AwtStackable](margin: StackLength = defaultMargin, cap: StackLength = defaultCap, layout: StackLayout = Fit) =
        new Stack[C](X, margin, cap, layout)
    
    /**
      * Creates a new vertical stack
      * @param margin The margin between items
      * @param cap The cap at each end of this stack (default = no cap)
      * @param layout The layout used (default = Fit)
      * @tparam C The type of items in this stack
      * @return A new stack
      */
    def column[C <: AwtStackable](margin: StackLength = defaultMargin, cap: StackLength = defaultCap, layout: StackLayout = Fit) =
        new Stack[C](Y, margin, cap, layout)
    
    /**
      * Creates a new horizontal stack
      * @param items The items placed in this stack
      * @param margin The margin between items
      * @param cap The cap at each end of this stack (default = no cap)
      * @param layout The layout used (default = Fit)
      * @tparam C The type of items in this stack
      * @return A new stack
      */
    def rowWithItems[C <: AwtStackable](items: TraversableOnce[C], margin: StackLength = defaultMargin,
                                        cap: StackLength = defaultCap, layout: StackLayout = Fit) =
        withItems(items, X, margin, cap, layout)
    
    /**
      * Creates a new vertical stack
      * @param items The items placed in this stack
      * @param margin The margin between items
      * @param cap The cap at each end of this stack (default = no cap)
      * @param layout The layout used (default = Fit)
      * @tparam C The type of items in this stack
      * @return A new stack
      */
    def columnWithItems[C <: AwtStackable](items: TraversableOnce[C], margin: StackLength = defaultMargin,
                                           cap: StackLength = defaultCap, layout: StackLayout = Fit) =
        withItems(items, Y, margin, cap, layout)
    
    /**
      * Creates a stack by adding contents through a builder
      * @param direction Stack direction
      * @param margin Margin between items (default = any)
      * @param cap Cap at each end of this stack (default = no cap)
      * @param layout Stack layout used (default = Fit)
      * @param b A function for building stack contents
      * @return A new stack
      */
    def build(direction: Axis2D, margin: StackLength = defaultMargin, cap: StackLength = defaultCap,
              layout: StackLayout = Fit)(b: VectorBuilder[AwtStackable] => Unit) =
    {
        val itemBuffer = new VectorBuilder[AwtStackable]()
        b(itemBuffer)
        withItems(itemBuffer.result(), direction, margin, cap, layout)
    }
    
    /**
      * Creates a horizontal stack by adding contents through a builder
      * @param margin Margin between items (default = any)
      * @param cap Cap at each end of this stack (default = no cap)
      * @param layout Stack layout used (default = Fit)
      * @param b A function for building stack contents
      * @return A new stack
      */
    def buildRow(margin: StackLength = defaultMargin, cap: StackLength = defaultCap, layout: StackLayout = Fit)
                (b: VectorBuilder[AwtStackable] => Unit) =
        build(X, margin, cap, layout)(b)
    
    /**
      * Creates a vertical stack by adding contents through a builder
      * @param margin Margin between items (default = any)
      * @param cap Cap at each end of this stack (default = no cap)
      * @param layout Stack layout used (default = Fit)
      * @param b A function for building stack contents
      * @return A new stack
      */
    def buildColumn(margin: StackLength = defaultMargin, cap: StackLength = defaultCap, layout: StackLayout = Fit)
                   (b: VectorBuilder[AwtStackable] => Unit) =
        build(Y, margin, cap, layout)(b)
    
    /**
      * Creates a stack by adding contents through a builder. Uses component creation context.
      * @param direction Stack direction
      * @param layout Stack layout used (default = Fit)
      * @param isRelated Whether the items in this stack should be considered closely related (default = false)
      * @param b A function for building stack contents
      * @param context Component creation context (implicit)
      * @return A new stack
      */
    def buildWithContext(direction: Axis2D, layout: StackLayout = Fit, isRelated: Boolean = false)
                        (b: VectorBuilder[AwtStackable] => Unit)(implicit context: ComponentContext) =
        build(direction, if (isRelated) context.relatedItemsStackMargin else context.stackMargin, context.stackCap, layout)(b)
    
    /**
      * Creates a horizontal stack by adding contents through a builder. Uses component creation context.
      * @param layout Stack layout used (default = Fit)
      * @param isRelated Whether the items in this stack should be considered closely related (default = false)
      * @param b A function for building stack contents
      * @param context Component creation context (implicit)
      * @return A new stack
      */
    def buildRowWithContext(layout: StackLayout = Fit, isRelated: Boolean = false)
                           (b: VectorBuilder[AwtStackable] => Unit)(implicit context: ComponentContext) =
        buildWithContext(X, layout, isRelated)(b)
    
    /**
      * Creates a vertical stack by adding contents through a builder. Uses component creation context.
      * @param layout Stack layout used (default = Fit)
      * @param isRelated Whether the items in this stack should be considered closely related (default = false)
      * @param b A function for building stack contents
      * @param context Component creation context (implicit)
      * @return A new stack
      */
    def buildColumnWithContext(layout: StackLayout = Fit, isRelated: Boolean = false)
                              (b: VectorBuilder[AwtStackable] => Unit)(implicit context: ComponentContext) =
        buildWithContext(Y, layout, isRelated)(b)
}

/**
* A stack holds multiple stackable components in a stack-like manner either horizontally or vertically
* @author Mikko Hilpinen
* @since 25.2.2019
  * @param direction The direction of this stack (X = row, Y = column)
  * @param margin The margin placed between items
  * @param cap The cap at each end of this stack (default = no cap)
  * @param layout The layout of this stack's components perpendicular to the 'direction' (default = Fit)
**/
class Stack[C <: Stack.AwtStackable](override val direction: Axis2D, override val margin: StackLength = StackLength.any,
                                     override val cap: StackLength = StackLength.fixed(0), override val layout: StackLayout = Fit)
    extends StackLike[C] with AwtComponentWrapperWrapper with CachingStackable with SwingComponentRelated
        with AwtContainerRelated with CustomDrawableWrapper
{
	// ATTRIBUTES    --------------------
    
    private val panel = new Panel[C]()
    
    
    // INITIAL CODE    ------------------
    
    // Each time size changes, also updates content (doesn't reset stack sizes at this time)
    addResizeListener(updateLayout())
    
    
    // IMPLEMENTED    -------------------
    
    override def children = super[StackLike].children
    
    override def drawable = panel
    
    override def component = panel.component
    
    override protected def wrapped = panel
    
    override protected def updateVisibility(visible: Boolean) = panel.isVisible = visible
    
    override def isVisible_=(isVisible: Boolean) = super[CachingStackable].isVisible_=(isVisible)
    
    protected def add(component: C) = panel += component
    
    protected def remove(component: C) = panel -= component
    
    
    // OTHER    -----------------------
    
    /**
      * Adds alterating row colors to this stack
      * @param firstColor The color of the first row
      * @param secondColor The color of the second row
      */
    def addAlternatingRowBackground(firstColor: Color, secondColor: Color) = addCustomDrawer(
        new AlternatingRowBackgroundDrawer(Vector(firstColor, secondColor)))
    
    
    // NESTED CLASSES   ---------------
    
    private class AlternatingRowBackgroundDrawer(val colors: Seq[Color]) extends CustomDrawer
    {
        override def drawLevel = DrawLevel.Background
    
        override def draw(drawer: Drawer, bounds: Bounds) =
        {
            if (count > 1)
            {
                val baseDrawer = drawer.noEdges
                val drawers = colors.map(baseDrawer.withFillColor).repeatingIterator()
    
                val b = bounds.size.perpendicularTo(direction)
                var lastStart = 0.0
                var lastComponentBottom = components.head.maxCoordinateAlong(direction)
                components.tail.foreach
                {
                    c =>
                        val componentPosition = c.coordinateAlong(direction)
            
                        val margin = (componentPosition - lastComponentBottom) / 2
                        val lastSegmentLength = lastComponentBottom - lastStart + margin
            
                        // Draws the previous segment area
                        drawers.next().draw(Bounds(Point(lastStart, 0, direction) + bounds.position,
                            Size(lastSegmentLength, b, direction)))
            
                        // Prepares for the next segment
                        lastStart = componentPosition - margin
                        lastComponentBottom = componentPosition + c.lengthAlong(direction)
                }
    
                // Draws the last segment
                drawers.next().draw(Bounds.between(Point(lastStart, 0, direction) + bounds.position, bounds.bottomRight))
            }
            else
                drawer.noEdges.withFillColor(colors.head).draw(bounds)
        }
    }
}