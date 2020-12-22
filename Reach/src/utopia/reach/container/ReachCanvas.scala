package utopia.reach.container

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.VolatileList
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.event.{KeyStateEvent, MouseButtonStateEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{KeyStateListener, MouseMoveListener}
import utopia.genesis.image.Image
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.{Bounds, Point, Vector2D}
import utopia.genesis.util.Drawer
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.{ComponentCreationResult, ComponentWrapResult}
import utopia.reflection.color.ColorShade.Dark
import utopia.reflection.color.ColorShadeVariant
import utopia.reflection.component.drawing.mutable.CustomDrawable
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.swing.template.{JWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.window.Popup
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic
import utopia.reflection.container.swing.window.Popup.PopupAutoCloseLogic.Never
import utopia.reach.cursor.{CursorSet, ReachCursorManager}
import utopia.reach.util.{Priority, ReachFocusManager, RealTimeReachPaintManager}
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackSize

import java.awt.event.KeyEvent
import java.awt.{AWTKeyStroke, Container, Graphics, KeyboardFocusManager}
import java.util
import javax.swing.{JComponent, JPanel}
import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future, Promise}

object ReachCanvas
{
	/**
	  * Creates a new set of canvas with a reach component in them
	  * @param cursors Cursors to use inside this component. None if this component shouldn't affect the cursor drawing.
	  * @param content Function for producing the content once parent hierarchy is available
	  * @tparam C Type of created canvas content
	  * @return A set of canvas with the content inside them and the produced canvas content as well
	  */
	def apply[C <: ReachComponentLike, R](cursors: Option[CursorSet] = None)
										 (content: ComponentHierarchy => ComponentCreationResult[C, R])
										 (implicit exc: ExecutionContext) =
	{
		val contentPromise = Promise[ReachComponentLike]()
		val canvas = new ReachCanvas(contentPromise.future, cursors)
		val newContent = content(canvas.HierarchyConnection)
		contentPromise.success(newContent.component)
		newContent in canvas
	}
}

/**
  * The component that connects a reach component hierarchy to the swing component hierarchy
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
class ReachCanvas private(contentFuture: Future[ReachComponentLike], cursors: Option[CursorSet])
						 (implicit exc: ExecutionContext)
	extends JWrapper with Stackable with AwtContainerRelated with SwingComponentRelated with CustomDrawable
{
	// ATTRIBUTES	---------------------------
	
	override var customDrawers = Vector[CustomDrawer]()
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	private var layoutUpdateQueue = VolatileList[Seq[ReachComponentLike]]()
	private var updateFinishedQueue = VolatileList[() => Unit]()
	// private var buffer = Image.empty
	
	private val panel = new CustomDrawPanel()
	// private val repaintNeed = VolatileOption[RepaintNeed](Full)
	
	/**
	  * Object that manages focus between the components in this canvas element
	  */
	val focusManager = new ReachFocusManager(panel)
	// TODO: Allow custom repainters
	private val painterPromise = contentFuture.map { c => RealTimeReachPaintManager(c) }
	/**
	  * Object that manages cursor display inside this canvas. None if cursor state is not managed in this canvas.
	  */
	val cursorManager = cursors.map { new ReachCursorManager(_) }
	private val cursorPainter = cursorManager.map { new CursorSwapper(_) }
	
	private val _attachmentPointer = new PointerWithEvents(false)
	
	
	// INITIAL CODE	---------------------------
	
	_attachmentPointer.addListener { event =>
		// When attached to the stack hierarchy, makes sure to update immediate content layout and repaint this component
		if (event.newValue)
		{
			// repaintNeed.setOne(Full)
			currentContent.foreach { content =>
				val branches = content.toTree.allBranches
				if (branches.isEmpty)
					layoutUpdateQueue :+= Vector(content)
				else
					layoutUpdateQueue ++= branches.map { content +: _ }
				updateLayout()
			}
		}
		fireStackHierarchyChangeEvent(event.newValue)
	}
	
	// Listens to tabulator key events for manual focus handling
	addKeyStateListener(FocusKeyListener)
	// Listens to mouse events for manual cursor drawing
	cursorPainter.foreach(addMouseMoveListener)
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return A pointer to this canvas' stack hierarchy attachment status
	  */
	def attachmentPointer = _attachmentPointer.view
	
	/**
	  * @return Whether this component uses custom cursor painting features
	  */
	def isManagingCursor = cursorManager.nonEmpty
	
	private def currentContent = contentFuture.current.flatMap { _.toOption }
	
	private def currentPainter = painterPromise.current.flatMap { _.toOption }
	
	
	// IMPLEMENTED	---------------------------
	
	override def isAttachedToMainHierarchy = _attachmentPointer.value
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
		_attachmentPointer.value = newAttachmentStatus
	
	override def component: JComponent with Container = panel
	
	override def updateLayout() =
	{
		// Updates content size
		val contentSizeChanged = currentContent match
		{
			case Some(content) =>
				val requiresSizeUpdate = content.size != size
				if (requiresSizeUpdate)
					content.size = size
				requiresSizeUpdate
			case None => false
		}
		
		// Updates content layout
		val layoutUpdateQueues = layoutUpdateQueue.popAll().toSet.map { q: Seq[ReachComponentLike] => q -> contentSizeChanged }
		val sizeChangeTargets: Set[ReachComponentLike] =
		{
			if (contentSizeChanged)
				currentContent.toSet
			else
				Set()
		}
		if (layoutUpdateQueues.nonEmpty)
			updateLayoutFor(layoutUpdateQueues, sizeChangeTargets).foreach { repaint(_) }
		
		// Performs the queued tasks
		updateFinishedQueue.popAll().foreach { _() }
	}
	
	override def stackSize = currentContent match
	{
		case Some(content) => content.stackSize
		case None => StackSize.any
	}
	
	override def resetCachedSize() = currentContent.foreach { _.resetCachedSize() }
	
	override val stackId = hashCode()
	
	override def drawBounds = Bounds(Point.origin, size)
	
	override def repaint() = currentPainter.foreach { _.repaintAll() }
	
	override def distributeMouseButtonEvent(event: MouseButtonStateEvent) =
	{
		super.distributeMouseButtonEvent(event) match
		{
			case Some(consumed) =>
				val newEvent = event.consumed(consumed)
				currentContent.foreach { _.distributeMouseButtonEvent(newEvent) }
				Some(consumed)
			case None => currentContent.flatMap { _.distributeMouseButtonEvent(event) }
		}
	}
	
	override def distributeMouseMoveEvent(event: MouseMoveEvent) =
	{
		super.distributeMouseMoveEvent(event)
		currentContent.foreach { _.distributeMouseMoveEvent(event) }
	}
	
	// TODO: WET WET
	override def distributeMouseWheelEvent(event: MouseWheelEvent) =
	{
		super.distributeMouseWheelEvent(event) match
		{
			case Some(consumed) =>
				val newEvent = event.consumed(consumed)
				currentContent.foreach { _.distributeMouseWheelEvent(newEvent) }
				Some(consumed)
			case None => currentContent.flatMap { _.distributeMouseWheelEvent(event) }
		}
	}
	
	// OTHER	------------------------------
	
	/**
	  * Revalidates this component, queueing some component layout updates to be done afterwards
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  */
	def revalidate(updateComponents: Seq[ReachComponentLike]): Unit =
	{
		val trueQueue = updateComponents.dropWhile { _ == this }
		if (trueQueue.nonEmpty)
			layoutUpdateQueue :+= trueQueue
		revalidate()
	}
	
	/**
	  * Revalidates this component's layout. Calls the specified function when whole component layout has been updated.
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  * @param f A function called after layout has been updated.
	  */
	def revalidateAndThen(updateComponents: Seq[ReachComponentLike])(f: => Unit) =
	{
		// Queues the action
		updateFinishedQueue :+= (() => f)
		// Queues revalidation
		revalidate(updateComponents)
	}
	
	/**
	  * Repaints a part of this canvas
	  * @param area Area to paint again
	  * @param priority Priority to use for this repaint. The high level priority areas are painted first.
	  */
	def repaint(area: Bounds, priority: Priority = Priority.Normal) =
		currentPainter.foreach { _.repaintRegion(area, priority) }
	
	/**
	  * Shifts a painted region inside these canvases
	  * @param originalArea The area to shift (relative to this canvas' top left corner)
	  * @param translation Translation vector to apply to the area
	  */
	def shiftArea(originalArea: Bounds, translation: Vector2D) =
		currentPainter.foreach { _.shift(originalArea, translation) }
	
	/**
	  * Creates a pop-up over the specified component-area
	  * @param actorHandler Actor handler that will deliver action events for the pop-up
	  * @param over Area over which the pop-up will be displayed
	  * @param alignment Alignment to use when placing the pop-up (default = Right)
	  * @param margin Margin to place between the area and the pop-up (not used with Center alignment)
	  * @param autoCloseLogic Logic used for closing the pop-up (default = won't automatically close the pop-up)
	  * @param makeContent A function for producing pop-up contents based on a component hierarchy
	  * @tparam C Type of created component
	  * @tparam R Type of additional result
	  * @return A component wrapping result that contains the pop-up, the created component inside the canvas and
	  *         the additional result returned by 'makeContent'
	  */
	def createPopup[C <: ReachComponentLike, R](actorHandler: ActorHandler, over: Bounds,
											  alignment: Alignment = Alignment.Right, margin: Double = 0.0,
											  autoCloseLogic: PopupAutoCloseLogic = Never)
											 (makeContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		val newCanvas = ReachCanvas(cursors)(makeContent)
		newCanvas.isTransparent = true
		val popup = Popup(this, newCanvas.parent, actorHandler, autoCloseLogic, alignment) { (_, popupSize) =>
			// Calculates pop-up top left coordinates based on alignment
			Point.calculateWith { axis =>
				alignment.directionAlong(axis) match
				{
					case Some(direction) =>
						direction match
						{
							case Positive => over.maxAlong(axis) + margin
							case Negative => over.minAlong(axis) - popupSize.along(axis) - margin
						}
					case None => over.center.along(axis) - popupSize.along(axis) / 2
				}
			}
		}
		ComponentWrapResult(popup, newCanvas.child, newCanvas.result)
	}
	
	// Second parameter in queues is whether a repaint operation has already been queued for them
	// Resized children are expected to have their repaints already queued
	// Returns areas to repaint afterwards
	private def updateLayoutFor(componentQueues: Set[(Seq[ReachComponentLike], Boolean)],
								sizeChangedChildren: Set[ReachComponentLike]): Vector[Bounds] =
	{
		val nextSizeChangeChildrenBuilder = new VectorBuilder[ReachComponentLike]()
		val nextPositionChangeChildrenBuilder = new VectorBuilder[ReachComponentLike]()
		val repaintZonesBuilder = new VectorBuilder[Bounds]()
		
		// Component -> Whether paint operation has already been queued
		val nextTargets = componentQueues.map { case (queue, wasPainted) => queue.head -> wasPainted } ++
			sizeChangedChildren.map { _ -> true }
		// Updates the layout of the next layer (from top to bottom) components.
		// Checks for size (and possible position) changes and queues updates for the children of components which
		// changed size during the layout update
		// Also, collects any repaint requirements
		nextTargets.foreach { case (component, wasPainted) =>
			// Caches bounds before update
			val oldChildBounds = component.children.map { c => c -> c.bounds }
			// Applies component update
			component.updateLayout()
			// Queues child updates (on size changes) and possible repaints
			// (only in components where no repaint has occurred yet)
			if (wasPainted)
				oldChildBounds.foreach { case (child, oldBounds) =>
					if (child.size != oldBounds.size)
						nextSizeChangeChildrenBuilder += child
				}
			else
				oldChildBounds.foreach { case (child, oldBounds) =>
					val currentBounds = child.bounds
					if (currentBounds != oldBounds)
					{
						repaintZonesBuilder += (Bounds.around(Vector(oldBounds, currentBounds)) +
							child.parentHierarchy.positionToTopModifier)
						if (oldBounds.size != currentBounds.size)
							nextSizeChangeChildrenBuilder += child
						else
							nextPositionChangeChildrenBuilder += child
					}
				}
		}
		
		// Moves to the next layer of components, if there is one
		val nextSizeChangedChildren = nextSizeChangeChildrenBuilder.result().toSet
		val paintedChildren = nextSizeChangedChildren ++ nextPositionChangeChildrenBuilder.result()
		val nextQueues = componentQueues.filter { _._1.size > 1 }.map { case (queue, wasPainted) =>
			if (wasPainted)
				queue.tail -> wasPainted
			else
				// Checks whether a paint operation was queued for this component already
				queue.tail -> paintedChildren.contains(queue(1))
		}
		val repaintZones = repaintZonesBuilder.result()
		if (nextQueues.isEmpty && nextSizeChangedChildren.isEmpty)
			repaintZones
		else
			 repaintZones ++ updateLayoutFor(nextQueues, nextSizeChangedChildren)
	}
	
	
	// NESTED	------------------------------
	
	private object HierarchyConnection extends ComponentHierarchy
	{
		override def parent = Left(ReachCanvas.this)
		
		override def linkPointer = attachmentPointer
		
		override def isThisLevelLinked = isLinked
	}
	
	private class CustomDrawPanel extends JPanel(null)
	{
		// INITIAL CODE	---------------------
		
		setOpaque(true)
		// setBackground(Color.black.toAwt)
		
		// Makes this canvas element focusable and disables the default focus traversal keys
		setFocusable(true)
		setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		
		
		// IMPLEMENTED	----------------------
		
		override def paint(g: Graphics) = paintComponent(g)
		
		override def paintComponent(g: Graphics) = currentPainter.foreach { p => Drawer.use(g)(p.paintWith) }
		
		// Never paints children (because won't have any children)
		override def paintChildren(g: Graphics) = ()
	}
	
	private object FocusKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	------------------------
		
		// Only listens to tabulator presses
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter && KeyStateEvent.keyFilter(KeyEvent.VK_TAB)
		
		
		// IMPLEMENTED	-----------------------
		
		override def onKeyState(event: KeyStateEvent) =
		{
			// Moves the focus forwards or backwards
			val direction = if (event.keyStatus.shift) Negative else Positive
			focusManager.moveFocus(direction)
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = focusManager.hasFocus
	}
	
	private class CursorSwapper(cursorManager: ReachCursorManager) extends MouseMoveListener with Handleable
	{
		// ATTRIBUTES	-----------------------------
		
		private var lastMousePosition = Point.origin
		private var lastCursorImage: Option[Image] = None
		
		private val shadeCalculatorFuture = painterPromise.map[Bounds => ColorShadeVariant] { painter =>
			area => painter.averageShadeOf(area)
		}
		
		
		// IMPLEMENTED	-----------------------------
		
		override def onMouseMove(event: MouseMoveEvent) =
		{
			val newPosition = event.mousePosition - position
			if (lastMousePosition != newPosition)
			{
				lastMousePosition = newPosition
				if (bounds.contains(event.mousePosition))
				{
					val newImage = cursorManager.cursorImageAt(newPosition) { area =>
						shadeCalculatorFuture.currentSuccess  match
						{
							case Some(calculate) => calculate(area)
							case None => Dark
						}
					}
					if (!lastCursorImage.contains(newImage))
					{
						lastCursorImage = Some(newImage)
						cursorManager.cursorForImage(newImage).foreach(component.setCursor)
					}
				}
			}
		}
	}
}
