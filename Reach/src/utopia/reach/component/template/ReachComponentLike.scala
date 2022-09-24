package utopia.reach.component.template

import utopia.flow.collection.immutable.Tree
import utopia.flow.view.template.eventful.ChangingLike
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size, Vector2D}
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.drawing.template.DrawLevel.{Background, Foreground, Normal}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.util.Priority
import utopia.reflection.component.template.layout.stack.Stackable2
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.Never
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.text.Font

/**
  * A common trait for "Reach" (no-swing) style components
  * @author Mikko Hilpinen
  * @since 3.10.2020, v0.1
  */
trait ReachComponentLike extends Stackable2
{
	// ABSTRACT	------------------------
	
	/**
	  * @return A pointer to the current position of this component
	  */
	def positionPointer: ChangingLike[Point]
	/**
	  * @return A pointer to the current size of this component
	  */
	def sizePointer: ChangingLike[Size]
	/**
	  * @return A pointer to the current bounds (position + size) of this component
	  */
	def boundsPointer: ChangingLike[Bounds]
	
	/**
	  * @return Hierarchy containing all this component's parents. This hierarchy should be static/unchanging,
	  *         since it may be listened by other components.
	  */
	def parentHierarchy: ComponentHierarchy
	
	override def children: Seq[ReachComponentLike] = Vector()
	
	/**
	  * @return Whether this component is partially or fully transparent
	  *         (I.e. will not paint over all of its bounds with 100% alpha)
	  */
	def transparent: Boolean
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
	  * @return Whether this component paints all of its bounds with 100% alpha
	  *         (I.e. can't be seen through at any point)
	  */
	def opaque = !transparent
	
	/**
	  * @return The canvas element that contains this component
	  */
	def parentCanvas = parentHierarchy.top
	/**
	  * @return Window that contains this component
	  */
	def parentWindow = parentHierarchy.parentWindow
	
	/**
	  * @return The absolute (on-screen) position of this component
	  */
	def absolutePosition = position + parentHierarchy.absolutePositionModifier
	/**
	  * @return The bounds of this component on the screen (provided this component is connected to a hierarchy
	  *         reaching a window)
	  */
	def absoluteBounds = Bounds(absolutePosition, size)
	
	/**
	  * @return The position of this component inside the so called top component (the canvas element)
	  */
	def positionInTop = position + parentHierarchy.positionToTopModifier
	/**
	  * @return The bounds of this component inside the top component (canvas element)
	  */
	def boundsInsideTop = Bounds(positionInTop, size)
	
	/**
	  * @return A tree representation of this component hierarchy (root node represents this component and branches
	  *         below it are this component's children)
	  */
	def toTree: Tree[ReachComponentLike] = Tree(this, children.toVector.map { _.toTree })
	
	/**
	  * @return An image of this component with its current size
	  */
	def toImage =
	{
		// Places the drawer so that after applying component position, drawer will draw to (0,0)
		if (size.isPositive)
			Image.paint(size) { d => paintWith(d.translated(-position)) }
		else
			Image.empty
	}
	
	
	// IMPLEMENTED	---------------------
	
	override def bounds = boundsPointer.value
	
	override def position = positionPointer.value
	
	override def size = sizePointer.value
	
	override def fontMetricsWith(font: Font) = parentHierarchy.fontMetricsWith(font)
	
	
	// OTHER	-------------------------
	
	/**
	  * @param another Another component
	  * @return Whether this component is a child (below in hierarchy) of the specified component
	  */
	def isChildOf(another: ReachComponentLike) = parentHierarchy.isChildOf(another)
	/**
	  * @param hierarchy A component hierarchy
	  * @return Whether this component is a child (below) of the specified component hierarchy
	  */
	def isChildOf(hierarchy: ComponentHierarchy) = parentHierarchy.isChildOf(hierarchy)
	
	/**
	  * @param parent A component higher up in this component's hierarchy
	  * @return This component's position within that component's coordinate system. None if this component is
	  *         not a child of that component.
	  */
	def positionRelativeTo(parent: ReachComponentLike) =
		parentHierarchy.positionInComponentModifier(parent).map { position + _ }
	
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
	  * Paints this component again
	  */
	def repaint(priority: Priority) = parentHierarchy.repaint(bounds, priority)
	/**
	  * Paints this component's parent again
	  * @param priority Priority to use for the repaint operation. Higher priority components are drawn first.
	  *                 (Default = Normal).
	  */
	def repaintParent(priority: Priority = Priority.Normal) = parentHierarchy.repaintBottom(priority)
	/**
	  * Repaints a sub-section of this component's area
	  * @param relativeArea An area to repaint (where (0,0) is located at the top left corner of this component)
	  * @param priority Priority to use for the repaint (default = normal)
	  */
	def repaintArea(relativeArea: Bounds, priority: Priority = Priority.Normal) =
		parentHierarchy.repaint(relativeArea + position, priority)
	/**
	  * Indicates that this component's and its hierarchy's layout should be updated. Once that has been done,
	  * repaints this component
	  * @param priority Priority to use for the repaint operation. Higher priority components are drawn first.
	  *                 (Default = Normal).
	  */
	def revalidateAndRepaint(priority: Priority = Priority.Normal) = revalidateAndThen { repaint(priority) }
	
	/**
	  * Paints movement of this component, translating it by specified amount
	  * @param movement Amount to translate this component in the drawn image.
	  *                 <b>Will only affect visual results and not touch the position information of this component</b>
	  */
	def paintMovement(movement: => Vector2D) = parentHierarchy.shiftArea(bounds, movement)
	/**
	  * Paints this component and its children
	  * @param drawer   Drawer to use for drawing this component. Origin coordinates (0,0) should be located at the
	  *                 top-left corner of the parent component.
	  * @param clipZone Bounds where the drawing is limited. None if whole component area should be drawn.
	  *                 The clip zone coordinate system matches that of the drawer (0,0) is at the top-left corner of
	  *                 this component's parent.
	  */
	def paintWith(drawer: Drawer, clipZone: Option[Bounds] = None): Unit =
	{
		// Calculates new clipping zone and drawer origin
		val childClipZone = clipZone.map { _ - position }
		val components = children
		
		// If one of the child components blocks the specified clip zone,
		// won't draw the underlying contents of this component
		val clipZoneIsCovered = childClipZone.exists { clipZone =>
			components.view.filter { _.bounds.contains(clipZone) }.exists { _.coversAllOf(clipZone) }
		}
		// Paints background and normal levels (if necessary)
		if (!clipZoneIsCovered)
		{
			paintContent(drawer, Background, clipZone)
			paintContent(drawer, Normal, clipZone)
		}
		
		// Paints child components (only those that overlap with the clipping bounds)
		if (components.nonEmpty)
		{
			val remainingComponents = childClipZone match
			{
				case Some(zone) => components.filter { _.bounds.overlapsWith(zone) }
				case None => components
			}
			if (remainingComponents.nonEmpty)
				drawer.translated(position).disposeAfter { d =>
					remainingComponents.foreach { c => c.paintWith(d, childClipZone) }
				}
		}
		// Paints foreground
		paintContent(drawer, Foreground, clipZone)
	}
	
	/**
	  * @param region Targeted region inside this component (should be relative to this component's top left corner)
	  * @return Image containing the specified region
	  */
	def regionToImage(region: Bounds) =
	{
		if (size.isPositive) {
			// Places the drawer so that the top left corner of the region will be drawn to (0,0)
			region.intersectionWith(Bounds(Point.origin, size)) match {
				case Some(actualRegion) =>
					Image.paint(actualRegion.size) { d =>
						paintWith(d.translated(-position - region.position), Some(region))
					}
				case None => Image.empty
			}
		}
		else
			Image.empty
	}
	
	/**
	  * Registers a new listener to be called when this component's hierarchy attachment status changes
	  * @param listener A listener function that will be called each time this component's hierarchy attachment
	  *                 status changes. The function accepts the new attachment status (true if attached, false if not)
	  * @tparam U Arbitrary result type
	  */
	def addHierarchyListener[U](listener: Boolean => U) = parentHierarchy.linkPointer
		.addContinuousListenerAndSimulateEvent(false) { e => listener(e.newValue) }
	
	/**
	  * Creates a pop-up next to this component
	  * @param actorHandler Actor handler that will deliver action events for the pop-up
	  * @param alignment Alignment to use when placing the pop-up (default = Right)
	  * @param margin Margin to place between this component and the pop-up (not used with Center alignment)
	  * @param autoCloseLogic Logic used for closing the pop-up (default = won't automatically close the pop-up)
	  * @param makeContent A function for producing pop-up contents based on a component hierarchy
	  * @tparam C Type of created component
	  * @tparam R Type of additional result
	  * @return A component wrapping result that contains the pop-up, the created component inside the canvas and
	  *         the additional result returned by 'makeContent'
	  */
	def createPopup[C <: ReachComponentLike, R](actorHandler: ActorHandler, alignment: Alignment = Alignment.Right,
											  margin: Double = 0.0, autoCloseLogic: PopupAutoCloseLogic = Never)
											 (makeContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
		parentCanvas.createPopup(actorHandler, boundsInsideTop, alignment, margin, autoCloseLogic)(makeContent)
	
	// Expects the clip zone to be completely inside this component's bounds
	private def coversAllOf(clipZone: Bounds): Boolean =
	{
		if (opaque)
			true
		else
		{
			val newClipZone = clipZone - position
			children.view.filter { _.bounds.contains(newClipZone) }.exists { _.coversAllOf(newClipZone) }
		}
	}
}
