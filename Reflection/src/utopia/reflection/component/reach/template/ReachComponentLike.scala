package utopia.reflection.component.reach.template

import utopia.flow.datastructure.immutable.Tree
import utopia.flow.event.Changing
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.{Background, Foreground, Normal}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.template.layout.stack.Stackable2
import utopia.reflection.text.Font

/**
  * A common trait for "Reach" (no-swing) style components
  * @author Mikko Hilpinen
  * @since 3.10.2020, v2
  */
trait ReachComponentLike extends Stackable2
{
	// ABSTRACT	------------------------
	
	/**
	  * @return A pointer to the current position of this component
	  */
	def positionPointer: Changing[Point]
	
	/**
	  * @return A pointer to the current size of this component
	  */
	def sizePointer: Changing[Size]
	
	/**
	  * @return A pointer to the current bounds (position + size) of this component
	  */
	def boundsPointer: Changing[Bounds]
	
	/**
	  * @return Hierarchy containing all this component's parents. This hierarchy should be static/unchanging,
	  *         since it may be listened by other components.
	  */
	def parentHierarchy: ComponentHierarchy
	
	override def children: Seq[ReachComponentLike] = Vector()
	
	/**
	  * Paints the contents of this component on some level. This method is called three times in normal component
	  * painting process (once for each draw level)
	  * @param drawer    Drawer used for painting this component's area. (0, 0) coordinates should lie at the parent
	  *                  component's top-left corner.
	  * @param drawLevel Targeted draw level (background is drawn first, then normal,
	  *                  foreground is drawn above child components)
	  * @param clipZone  Limited drawing area. The drawing should be clipped / limited to that area, if specified.
	  */
	def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds] = None): Unit
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Window that contains this component
	  */
	def parentWindow = parentHierarchy.parentWindow
	
	/**
	  * @return The absolute (on-screen) position of this component. None if not connected to main component
	  *         hierarchy
	  */
	def absolutePosition = position + parentHierarchy.absolutePositionModifier
	
	/**
	  * @return The position of this component inside the so called top component
	  */
	def positionInTop = position + parentHierarchy.positionToTopModifier
	
	/**
	  * @return A tree representation of this component hierarchy (root node represents this component and branches
	  *         below it are this component's children)
	  */
	def toTree: Tree[ReachComponentLike] = Tree(this, children.toVector.map { _.toTree })
	
	
	// IMPLEMENTED	---------------------
	
	override def bounds = boundsPointer.value
	
	override def position = positionPointer.value
	
	override def size = sizePointer.value
	
	override def fontMetrics(font: Font) = parentHierarchy.fontMetrics(font)
	
	
	// OTHER	-------------------------
	
	/**
	  * Indicates that this component's and its hierarchy's layout should be updated
	  */
	def revalidate() =
	{
		// Resets the cached stack size of this and upper components
		resetCachedSize()
		parentHierarchy.revalidate(Vector(this))
	}
	
	/**
	  * Indicates that this component's and its hierarchy's layout should be updated. Calls the specified function
	  * once layout update has completed (if it has completed)
	  * @param f A function called when/if the layout has completed. This function will not get called at all if
	  *          this component is not connected to the main stack hierarchy
	  */
	def revalidateAndThen(f: => Unit) =
	{
		resetCachedSize()
		parentHierarchy.revalidateAndThen(Vector(this))(f)
	}
	
	/**
	  * Paints this component again
	  */
	def repaint() = parentHierarchy.repaint(bounds)
	
	/**
	  * Paints this component's parent again
	  */
	def repaintParent() = parentHierarchy.repaintBottom()
	
	/**
	  * Paints this component and its children
	  * @param drawer   Drawer to use for drawing this component. Origin coordinates (0,0) should be located at the
	  *                 top-left corner of the parent component.
	  * @param clipZone Bounds where the drawing is limited. None if whole component area should be drawn.
	  */
	def paintWith(drawer: Drawer, clipZone: Option[Bounds] = None): Unit = {
		// Paints background and normal levels
		paintContent(drawer, Background, clipZone)
		paintContent(drawer, Normal, clipZone)
		// Paints child components (only those that overlap with the clipping bounds)
		val components = children
		if (components.nonEmpty) {
			// Calculates new clipping zone and drawer origin
			val newClipZone = clipZone.map {_ - position}
			val remainingComponents = newClipZone match {
				case Some(zone) => components.filter {_.bounds.overlapsWith(zone)}
				case None => components
			}
			if (remainingComponents.nonEmpty)
				drawer.translated(position).disposeAfter { d =>
					remainingComponents.foreach {_.paintWith(d, newClipZone)}
				}
		}
		// Paints foreground
		paintContent(drawer, Foreground, clipZone)
	}
	
	/**
	  * Registers a new listener to be called when this component's hierarchy attachment status changes
	  * @param listener A listener function that will be called each time this component's hierarchy attachment
	  *                 status changes. The function accepts the new attachment status (true if attached, false if not)
	  * @tparam U Arbitrary result type
	  */
	def addHierarchyListener[U](listener: Boolean => U) = parentHierarchy.linkPointer.addListener { e =>
		listener(e.newValue) }
}
