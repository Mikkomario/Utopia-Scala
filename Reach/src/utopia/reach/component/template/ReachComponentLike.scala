package utopia.reach.component.template

import utopia.firmament.component.stack.Stackable
import utopia.firmament.localization.LocalizedString
import utopia.flow.collection.immutable.caching.LazyTree
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.PreInitializedLazy
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.{Background, Foreground, Normal}
import utopia.genesis.graphics.{DrawLevel, Drawer, Priority}
import utopia.genesis.handling.event.mouse.{MouseDragEvent, MouseDragHandler}
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.area.polygon.c4
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.wrapper.{ComponentCreationResult, WindowCreationResult}
import utopia.reach.context.ReachWindowContext2
import utopia.reach.window.ReachWindow

import scala.concurrent.ExecutionContext

/**
  * A common trait for "Reach" (no-swing) style components
  * @author Mikko Hilpinen
  * @since 3.10.2020, v0.1
  */
trait ReachComponentLike extends Stackable with PartOfComponentHierarchy
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
	  * @return Handler used for delivering mouse drag -events to this component hierarchy
	  */
	def mouseDragHandler: MouseDragHandler
	
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
	def absoluteBounds = c4.bounds.Bounds(absolutePosition, size)
	
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
	  *         below it are this component's children).
	  *         Initialized lazily.
	  */
	def toTree: LazyTree[ReachComponentLike] =
		LazyTree.iterate(PreInitializedLazy(this)) { c => c.children.iterator.map { PreInitializedLazy(_) } }
	
	/**
	  * @return An image of this component with its current size
	  */
	def toImage = {
		// Places the drawer so that after applying component position, drawer will draw to (0,0)
		if (size.sign.isPositive)
			Image.paint(size) { d => paintWith(d.translated(-position)) }
		else
			Image.empty
	}
	
	
	// IMPLEMENTED	---------------------
	
	override def bounds = boundsPointer.value
	override def position = positionPointer.value
	override def size = sizePointer.value
	
	override def children: Seq[ReachComponentLike] = Empty
	
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
	def revalidate() = {
		// Resets the cached stack size of this and upper components
		resetCachedSize()
		parentHierarchy.revalidate(Single(this))
	}
	/**
	  * Indicates that this component's and its hierarchy's layout should be updated. Calls the specified function
	  * once layout update has completed (if it has completed)
	  * @param f A function called when/if the layout has completed. This function will not get called at all if
	  *          this component is not connected to the main stack hierarchy
	  */
	def revalidateAndThen(f: => Unit) = {
		resetCachedSize()
		parentHierarchy.revalidateAndThen(Single(this))(f)
	}
	/**
	  * Resets the cached stack size of this component and all the children of this component.
	  * Typically this is not required, but might be necessary after connecting this component to the component
	  * hierarchy, in case some revalidate() requests have been ignored.
	  */
	def resetEveryCachedStackSize() = toTree.bottomToTopNodesIterator.foreach { _.nav.resetCachedSize() }
	
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
	@deprecated(".revalidate() now repaints the lowest level automatically", "v1.0")
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
	def paintWith(drawer: Drawer, clipZone: Option[Bounds] = None): Unit = {
		// Calculates new clipping zone and drawer origin
		val childClipZone = clipZone.map { _ - position }
		val components = children
		
		// If one of the child components blocks the specified clip zone,
		// won't draw the underlying contents of this component
		val clipZoneIsCovered = childClipZone.exists { clipZone =>
			components.view.filter { _.bounds.contains(clipZone) }.exists { _.coversAllOf(clipZone) }
		}
		// Paints background and normal levels (if necessary)
		if (!clipZoneIsCovered) {
			paintContent(drawer, Background, clipZone)
			paintContent(drawer, Normal, clipZone)
		}
		
		// Paints child components (only those that overlap with the clipping bounds)
		if (components.nonEmpty) {
			val drawTargetsIterator = childClipZone match {
				case Some(zone) => components.iterator.filter { _.bounds.overlapsWith(zone) }
				case None => components.iterator
			}
			if (drawTargetsIterator.hasNext)
				drawer.translated(position).use { d =>
					drawTargetsIterator.foreach { c => c.paintWith(d, childClipZone) }
				}
		}
		// Paints foreground
		paintContent(drawer, Foreground, clipZone)
	}
	
	/**
	  * @param region Targeted region inside this component (should be relative to this component's top left corner)
	  * @return Image containing the specified region
	  */
	def regionToImage(region: Bounds) = {
		if (size.sign.isPositive) {
			// Places the drawer so that the top left corner of the region will be drawn to (0,0)
			region.overlapWith(Bounds(Point.origin, size)) match {
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
	def addHierarchyListener[U](listener: Boolean => U) =
		parentHierarchy.linkPointer.addContinuousListenerAndSimulateEvent(false) { e => listener(e.newValue) }
	
	/**
	  * Distributes a mouse drag event to this component hierarchy
	  * @param event Event to distribute. Should be relative to this component's parent's position.
	  */
	def distributeMouseDragEvent(event: MouseDragEvent): Unit = {
		// Informs local listeners
		mouseDragHandler.onMouseDrag(event)
		
		// Informs the child components, if applicable.
		// The event is transformed into a coordinate system relative to this component.
		// Events not affecting this component's area are not forwarded.
		if (children.nonEmpty) {
			if (event.concernsArea(bounds)) {
				val translated = relativizeMouseEventForChildren(event)
				children.foreach { _.distributeMouseDragEvent(translated) }
			}
		}
	}
	
	/**
	  * Creates a new window next to this component
	  * @param alignment          Alignment used when positioning the window relative to this component.
	  *                           E.g. If Center is used, will position the over the center of this component.
	  *                           Or if Right is used, will position this window right of this component.
	  *
	  *                           Please note that this alignment may be reversed in case there is not enough space
	  *                           on that side.
	  *
	  *                           Bi-directional alignments, such as TopLeft will place the window next to the component
	  *                           diagonally (so that they won't share any edge together).
	  *
	  *                           Default = Right
	  *
	  * @param margin             Margin placed between this component and the window, when possible
	  *                           (ignored if preferredAlignment=Center).
	  *                           Default = 0
	  * @param title              Title displayed on the window (provided that OS headers are in use).
	  *                           Default = empty = no title.
	  * @param matchEdgeLength Whether the window should share an edge length with the anchor component.
	  *                        E.g. If bottom alignment is used and 'matchEdgeLength' is enabled, the resulting
	  *                        window will attempt to stretch so that to matches the width of the 'component'.
	  *                        The stacksize limits of the window will be respected, however, and may limit the
	  *                        resizing.
	  *                        Default = false = will not resize the window.
	  * @param keepAnchored       Whether the window should be kept close to this component when its size changes
	  *                           or the this component is moved or resized.
	  *                           Set to false if you don't expect the owner component to move.
	  *                           This will save some resources, as a large number of components needs to be tracked.
	  *                           Default = true.
	  * @param display            Whether the window should be displayed immediately (default = false)
	  * @param createContent      A function that accepts a component hierarchy and creates the canvas content.
	  *                           May return an additional result, that will be included in the result of this function.
	  * @param context            Implicit window creation context
	  * @param exc                Implicit execution context
	  * @param log                Implicit logging implementation
	  * @tparam C Type of created canvas content
	  * @tparam R Type of additional function result
	  * @return A new window + created canvas + created canvas content + additional creation result
	  */
	def createWindow[C <: ReachComponentLike, R](alignment: Alignment = Alignment.Right, margin: Double = 0.0,
	                                             title: LocalizedString = LocalizedString.empty,
	                                             matchEdgeLength: Boolean = false, keepAnchored: Boolean = true,
	                                             display: Boolean = false)
	                                            (createContent: ComponentHierarchy => ComponentCreationResult[C, R])
	                                            (implicit context: ReachWindowContext2, exc: ExecutionContext,
	                                             log: Logger): WindowCreationResult[C, R] =
	{
		val window = ReachWindow.contextual.anchoredTo(this, alignment, margin, title,
			matchEdgeLength, keepAnchored)(createContent)
		if (display)
			window.display()
		window
	}
	
	// Expects the clip zone to be completely inside this component's bounds
	private def coversAllOf(clipZone: Bounds): Boolean = {
		if (opaque)
			true
		else {
			val newClipZone = clipZone - position
			children.view.filter { _.bounds.contains(newClipZone) }.exists { _.coversAllOf(newClipZone) }
		}
	}
}
