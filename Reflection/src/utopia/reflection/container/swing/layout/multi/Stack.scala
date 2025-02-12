package utopia.reflection.container.swing.layout.multi

import utopia.firmament.context.base.StaticBaseContext
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.enumeration.StackLayout.Fit
import utopia.firmament.model.stack.StackLength
import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.template.{AwtComponentRelated, AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackable}
import utopia.reflection.container.stack.template.layout.ReflectionStackLike
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}

import scala.collection.immutable.VectorBuilder

object Stack
{
    // TYPES    --------------------
    
    type AwtStackable = ReflectionStackable with AwtComponentRelated
    
    
    // ATTRIBUTES   ----------------
    
    private val defaultMargin = StackLength.any
    private val defaultCap = StackLength.fixedZero
    
    
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
    def withItems[C <: AwtStackable](items: IterableOnce[C], direction: Axis2D, margin: StackLength = defaultMargin,
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
    def rowWithItems[C <: AwtStackable](items: IterableOnce[C], margin: StackLength = defaultMargin,
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
    def columnWithItems[C <: AwtStackable](items: IterableOnce[C], margin: StackLength = defaultMargin,
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
      * @param cap Cap at each end of this stack (default = no cap)
      * @param layout Stack layout used (default = Fit)
      * @param isRelated Whether the items in this stack should be considered closely related (default = false)
      * @param b A function for building stack contents
      * @param context Component creation context (implicit)
      * @return A new stack
      */
    def buildWithContext(direction: Axis2D, cap: StackLength = defaultCap, layout: StackLayout = Fit,
                         isRelated: Boolean = false)(b: VectorBuilder[AwtStackable] => Unit)
                        (implicit context: StaticBaseContext) =
        build(direction, if (isRelated) context.smallStackMargin else context.stackMargin, cap, layout)(b)
    
    /**
      * Creates a horizontal stack by adding contents through a builder. Uses component creation context.
      * @param cap Cap at each end of this stack (default = no cap)
      * @param layout Stack layout used (default = Fit)
      * @param isRelated Whether the items in this stack should be considered closely related (default = false)
      * @param b A function for building stack contents
      * @param context Component creation context (implicit)
      * @return A new stack
      */
    def buildRowWithContext(cap: StackLength = defaultCap, layout: StackLayout = Fit, isRelated: Boolean = false)
                           (b: VectorBuilder[AwtStackable] => Unit)
                           (implicit context: StaticBaseContext) =
        buildWithContext(X, cap, layout, isRelated)(b)
    
    /**
      * Creates a vertical stack by adding contents through a builder. Uses component creation context.
      * @param cap Cap at each end of this stack (default = no cap)
      * @param layout Stack layout used (default = Fit)
      * @param isRelated Whether the items in this stack should be considered closely related (default = false)
      * @param b A function for building stack contents
      * @param context Component creation context (implicit)
      * @return A new stack
      */
    def buildColumnWithContext(cap: StackLength = defaultCap, layout: StackLayout = Fit, isRelated: Boolean = false)
                              (b: VectorBuilder[AwtStackable] => Unit)
                              (implicit context: StaticBaseContext) =
        buildWithContext(Y, cap, layout, isRelated)(b)
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
                                     override val cap: StackLength = StackLength.fixedZero, override val layout: StackLayout = Fit)
    extends CachingReflectionStackable with ReflectionStackLike[C] with AwtComponentWrapperWrapper with SwingComponentRelated
        with AwtContainerRelated with MutableCustomDrawableWrapper
{
	// ATTRIBUTES    --------------------
    
    private val panel = new Panel[C]()
    
    
    // INITIAL CODE    ------------------
    
    // Each time size changes, also updates content (doesn't reset stack sizes at this time)
    addResizeListener(updateLayout())
    
    
    // IMPLEMENTED    -------------------
    
    override def children = components
    
    override def drawable = panel
    
    override def component = panel.component
    
    override protected def wrapped = panel
    
    override protected def updateVisibility(visible: Boolean) = panel.visible = visible
    
    override def visible_=(isVisible: Boolean) = super[CachingReflectionStackable].visible_=(isVisible)
    
    override protected def addToWrapped(component: C, index: Int): Unit = panel.insert(component, index)
    override protected def removeFromWrapped(component: C): Unit = panel -= component
    
    
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
        override def opaque = true
    
        override def drawLevel = DrawLevel.Background
    
        override def draw(drawer: Drawer, bounds: Bounds) = {
            if (count > 1) {
                val settings = colors.map(DrawSettings.onlyFill).repeatingIterator()
    
                val b = bounds.size(direction.perpendicular)
                var lastStart = 0.0
                var lastComponentBottom = components.head.maxAlong(direction)
                components.tail.foreach { c =>
                    val componentPosition = c.position(direction)
        
                    val margin = (componentPosition - lastComponentBottom) / 2
                    val lastSegmentLength = lastComponentBottom - lastStart + margin
        
                    // Draws the previous segment area
                    drawer.draw(Bounds(Point(lastStart, 0, direction) + bounds.position,
                        Size(lastSegmentLength, b, direction)))(settings.next())
        
                    // Prepares for the next segment
                    lastStart = componentPosition - margin
                    lastComponentBottom = componentPosition + c.lengthAlong(direction)
                }
    
                // Draws the last segment
                drawer.draw(Bounds.between(Point(lastStart, 0, direction) + bounds.position, bounds.bottomRight))(
                    settings.next())
            }
            else
                drawer.draw(bounds)(DrawSettings.onlyFill(colors.head))
        }
    }
}