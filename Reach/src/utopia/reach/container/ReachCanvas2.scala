package utopia.reach.container

import utopia.firmament.awt.AwtComponentExtensions._
import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.stack.Stackable
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.SettableOnce
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.event.{KeyStateEvent, MouseButtonStateEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.graphics.{Drawer, FontMetricsWrapper}
import utopia.genesis.handling.mutable.{MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.handling.{KeyStateListener, MouseMoveListener}
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.color.ColorShade
import utopia.paradigm.color.ColorShade.Dark
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.cursor.{CursorSet, ReachCursorManager}
import utopia.reach.dnd.DragAndDropManager
import utopia.reach.focus.ReachFocusManager
import utopia.reach.util.RealTimeReachPaintManager
import utopia.reflection.component.drawing.template.{CustomDrawable, CustomDrawer}

import java.awt.event.KeyEvent
import java.awt.{AWTKeyStroke, Container, Graphics, Graphics2D, KeyboardFocusManager}
import java.util
import javax.swing.{JComponent, JPanel}
import scala.concurrent.ExecutionContext

object ReachCanvas2
{
	/**
	  * Creates a new Reach canvas
	  * @param attachmentPointer A pointer that contains true while this canvas is attached to a visible window hierarchy
	  * @param absoluteParentPositionView A view into the parent element's absolute position.
	  *                                   Preferably an instance of Changing.
	  * @param cursors A set of custom cursors to use on this canvas (optional)
	  * @param customDrawers Custom drawers to apply to this canvas (default = empty)
	  * @param enableAwtDoubleBuffering Whether AWT double buffering should be allowed.
	  *                                 Setting this to true might make the painting less responsive.
	  *                                 Default = false = disable AWT double buffering.
	  * @param disableFocus             Whether this canvas shall not be allowed to gain or manage focus.
	  *                                 Default = false = focus is enabled.
	  * @param revalidateImplementation Implementation for the revalidate() function.
	  *                                 Accepts this ReachCanvas instance.
	  *                                 Should:
	  *                                     1) Call resetCachedStackSize() for this canvas and
	  *                                         all the parent elements of this canvas.
	  *                                     2) Resize the components from top to bottom
	  *                                     3) Call updateLayout() for all parent elements of this canvas, and this canvas.
	  *                                 This might happen immediately or after a delay.
	  * @param createContent A function that accepts the created canvas' component hierarchy and yields the main
	  *                      content component that this canvas will wrap.
	  *                      May return an additional result, which will be returned by this function also.
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation for some error cases
	  * @tparam C Type of the component wrapped by this canvas
	  * @tparam R Type of the additional result from the 'createContent' function
	  * @return The created canvas + the created content component + the additional result returned by 'createContent'
	  */
	def apply[C <: ReachComponentLike, R](attachmentPointer: FlagLike, absoluteParentPositionView: View[Point],
	                                      cursors: Option[CursorSet] = None,
	                                      customDrawers: Vector[CustomDrawer] = Vector(),
	                                      enableAwtDoubleBuffering: Boolean = false, disableFocus: Boolean = false)
	                                     (revalidateImplementation: ReachCanvas2 => Unit)
	                                     (createContent: ComponentHierarchy => ComponentCreationResult[C, R])
	                                     (implicit exc: ExecutionContext, log: Logger) =
	{
		// Creates the canvas first
		val contentPointer = SettableOnce[ReachComponentLike]()
		val canvas = new ReachCanvas2(contentPointer, attachmentPointer, absoluteParentPositionView, cursors,
			customDrawers, enableAwtDoubleBuffering, disableFocus)(revalidateImplementation)
		// Then creates the content, using the canvas' component hierarchy
		val newContent = createContent(canvas.HierarchyConnection)
		// Attaches the content to the canvas and returns both
		contentPointer.set(newContent.component)
		newContent in canvas
	}
}

/**
  * The component that connects a reach component hierarchy to the swing component hierarchy
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  *
  * @constructor Creates a new ReachCanvas. Expects the main canvas element to be created soon after.
  * @param contentPointer A pointer that will contain the component wrapped by this canvas.
  *                       Contains None until initialized.
  * @param attachmentPointer A pointer that contains true whenever this canvas is attached to the main component
  *                          hierarchy (i.e. connected to a visible screen)
  * @param absoluteParentPositionView A view into the parent element's absolute position.
  *                                   Preferably an instance of Changing.
  * @param cursors Custom cursors to display on this canvas. Default = None = no custom cursor management enabled
  * @param customDrawers Custom drawers that paint on this canvas (default = empty)
  * @param enableAwtDoubleBuffering Whether AWT double buffering should be allowed.
  *                                 Setting this to true might make the painting less responsive.
  *                                 Default = false = disable AWT double buffering.
  * @param disableFocus Whether this canvas shall not be allowed to gain or manage focus.
  *                     Default = false = focus is enabled.
  * @param revalidateImplementation Implementation for the revalidate() function.
  *                                 Should:
  *                                     1) Call resetCachedStackSize() for this canvas and
  *                                     all the parent elements of this canvas.
  *                                     2) Resize the components from top to bottom
  *                                     3) Call updateLayout() for all parent elements of this canvas, and this canvas.
  *                                 This might happen immediately or after a delay.
  * @param exc Implicit execution context
  * @param log Implicit logging implementation for some error cases
  */
class ReachCanvas2 protected(contentPointer: Changing[Option[ReachComponentLike]], val attachmentPointer: FlagLike,
                             absoluteParentPositionView: View[Point],
                             cursors: Option[CursorSet] = None, override val customDrawers: Vector[CustomDrawer] = Vector(),
                             enableAwtDoubleBuffering: Boolean = false, disableFocus: Boolean = false)
                            (revalidateImplementation: ReachCanvas2 => Unit)
                            (implicit exc: ExecutionContext, log: Logger)
	extends ReachCanvasLike with Stackable with CustomDrawable
{
	// ATTRIBUTES	---------------------------
	
	private val layoutUpdateQueue = VolatileList[Seq[ReachComponentLike]]()
	private val updateFinishedQueue = VolatileList[() => Unit]()
	
	override val focusManager = new ReachFocusManager(CustomDrawPanel)
	private val painterPointer = contentPointer.map { _.map { c =>
		RealTimeReachPaintManager(c, disableDoubleBuffering = !enableAwtDoubleBuffering)
	} }
	override val cursorManager = cursors.map { new ReachCursorManager(_) }
	private val cursorPainter = cursorManager.map { new CursorSwapper(_) }
	/**
	 * The drag and drop -manager used by this canvas
	 */
	lazy val dragAndDropManager = DragAndDropManager(this)
	
	/**
	  * A pointer that contains the up-to-date bounds of this canvas
	  */
	lazy val boundsPointer = contentPointer.flatMap {
		case Some(c) => c.boundsPointer
		case None => Fixed(Bounds.zero)
	}
	/**
	  * A pointer that contains the up-to-date position of this canvas' top-left corner
	  */
	lazy val positionPointer = boundsPointer.map { _.position }
	/**
	  * A pointer that contains the up-to-date size of this canvas
	  */
	lazy val sizePointer = boundsPointer.map { _.size }
	/**
	  * A view into the absolute position (i.e. position on screen) of this canvas element
	  */
	// Caches calculations if possible
	lazy val absolutePositionView = absoluteParentPositionView match {
		case c: Changing[Point] => c.lazyMergeWith(positionPointer) { _ + _ }
		case v => View { v.value + position }
	}
	
	override lazy val mouseButtonHandler: MouseButtonStateHandler = MouseButtonStateHandler()
	override lazy val mouseMoveHandler: MouseMoveHandler = MouseMoveHandler()
	override lazy val mouseWheelHandler: MouseWheelHandler = MouseWheelHandler()
	
	
	// INITIAL CODE	---------------------------
	
	// When bounds get updated, updates the underlying component, also
	positionPointer.addContinuousListener { e => AwtEventThread.async { component.setLocation(e.newValue.toAwtPoint) } }
	sizePointer.addContinuousListener { e => AwtEventThread.async { component.setSize(e.newValue.toDimension) } }
	
	attachmentPointer.addListener { event =>
		// When attached to the stack hierarchy, makes sure to update immediate content layout and repaint this component
		if (event.newValue) {
			layoutUpdateQueue.clear()
			updateWholeLayout(size)
			// Listens to tabulator key events for manual focus handling
			if (!disableFocus)
				GlobalKeyboardEventHandler += FocusKeyListener
		}
		else
			GlobalKeyboardEventHandler -= FocusKeyListener
	}
	
	// Listens to mouse events for manual cursor drawing
	cursorPainter.foreach(addMouseMoveListener)
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return The awt component managed by this canvas
	  */
	def component: JComponent with Container = CustomDrawPanel
	/**
	  * @return The component hierarchy that starts with this canvas
	  */
	def hierarchy: ComponentHierarchy = HierarchyConnection
	
	/**
	  * @return The position of this canvas on the screen
	  */
	def absolutePosition = absolutePositionView.value
	/**
	  * @return The window that hosts this canvas instance
	  */
	def parentWindow = component.parentWindow
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def currentContent = contentPointer.value
	override protected def currentPainter = painterPointer.value
	
	override def drawBounds = Bounds(Point.origin, size)
	
	override def stackSize = currentContent match {
		case Some(content) => content.stackSize
		case None => StackSize.any
	}
	
	override def bounds: Bounds = boundsPointer.value
	// Could apply these updates once the content is initialized,
	// but this feature shall not be implemented unless it becomes necessary
	override def bounds_=(b: Bounds): Unit = currentContent.foreach { _.bounds = b }
	override def position_=(p: Point): Unit = currentContent.foreach { _.position = p }
	override def size_=(s: Size): Unit = currentContent.foreach { _.size = s }
	
	override def fontMetricsWith(font: Font): FontMetricsWrapper = component.getFontMetrics(font.toAwt)
	
	/**
	  * Revalidates this component, queueing some component layout updates to be done afterwards
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  */
	override def revalidate(updateComponents: Seq[ReachComponentLike]): Unit = {
		if (updateComponents.nonEmpty)
			layoutUpdateQueue :+= updateComponents
		revalidate()
	}
	/**
	  * Revalidates this component's layout. Calls the specified function when whole component layout has been updated.
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  * @param f                A function called after layout has been updated.
	  */
	override def revalidateAndThen(updateComponents: Seq[ReachComponentLike])(f: => Unit) = {
		// Queues the action
		updateFinishedQueue :+= (() => f)
		// Queues revalidation
		revalidate(updateComponents)
	}
	
	override def resetCachedSize() = currentContent.foreach { _.resetCachedSize() }
	
	override def updateLayout(): Unit = {
		// Updates content size and layout
		updateLayout(layoutUpdateQueue.popAll().toSet, size)
		// Performs the queued tasks
		updateFinishedQueue.popAll().foreach { _() }
	}
	
	// Repaints everything within this canvas
	override def repaint() = {
		currentPainter.foreach { _.resetBuffer() }
		super[ReachCanvasLike].repaint()
	}
	
	// Distributes the events via the canvas content element
	override def distributeMouseButtonEvent(event: MouseButtonStateEvent) = {
		super.distributeMouseButtonEvent(event) match {
			case Some(consumed) =>
				val newEvent = event.consumed(consumed)
				currentContent.foreach { _.distributeMouseButtonEvent(newEvent) }
				Some(consumed)
			case None => currentContent.flatMap { _.distributeMouseButtonEvent(event) }
		}
	}
	override def distributeMouseMoveEvent(event: MouseMoveEvent) = {
		super.distributeMouseMoveEvent(event)
		currentContent.foreach { _.distributeMouseMoveEvent(event) }
	}
	override def distributeMouseWheelEvent(event: MouseWheelEvent) = {
		super.distributeMouseWheelEvent(event) match {
			case Some(consumed) =>
				val newEvent = event.consumed(consumed)
				currentContent.foreach { _.distributeMouseWheelEvent(newEvent) }
				Some(consumed)
			case None => currentContent.flatMap { _.distributeMouseWheelEvent(event) }
		}
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * Requests this canvas' content hierarchy to be revalidated.
	  * Should cause resetCachedStackSize() and updateLayout() to be called, but not necessarily immediately.
	  */
	def revalidate() = revalidateImplementation(this)
	
	/**
	  * Calculates the default window anchor position.
	  * The anchor is placed at the center of the focused component by default.
	  * If no component is in focus, a specific point within the window will be used instead.
	  *
	  * This function is intended to be passed as a window constructor's 'getAnchor' parameter.
	  *
	  * @param windowBounds Bounds of the resizing window (call-by-name)
	  * @param defaultAlignment Alignment to use when no component is in focus.
	  *                         E.g. When Left alignment is used, the window will expand to the right,
	  *                         when BottomRight aligment is used, the window will expand up and left.
	  *                         Default = Center = Window will attempt to expand equally on both / all sides.
	  *
	  * @return The anchor point to use at this time
	  */
	def anchorPosition(windowBounds: => Bounds, defaultAlignment: Alignment = Center) =
		focusManager.absoluteFocusOwnerBounds match {
			case Some(bounds) => bounds.center
			case None => defaultAlignment.origin(windowBounds)
		}
	
	/*
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
	/* TODO: Rewrite this function once ReachWindow class and a new PopUp class are available
	def createPopup[C <: ReachComponentLike, R](actorHandler: ActorHandler, over: Bounds,
											  alignment: Alignment = Alignment.Right, margin: Double = 0.0,
											  autoCloseLogic: PopupAutoCloseLogic = Never)
											 (makeContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		val newCanvas = ReachCanvas(cursors)(makeContent)
		// FIXME: Normal paint operations don't work while isTransparent = true, but partially transparent windows
		//  don't work when it is false. Avoid this by creating a new pop-up system
		// newCanvas.isTransparent = true
		val popup = Popup(this, newCanvas.parent, actorHandler, autoCloseLogic, alignment) { (_, popupSize) =>
			// Calculates pop-up top left coordinates based on alignment
			Point.fromFunction2D { axis =>
				alignment(axis) match {
					case Close => over.minAlong(axis) - popupSize(axis) - margin
					case Middle => over.center(axis) - popupSize(axis) / 2
					case Far => over.maxAlong(axis) + margin
				}
			}
		}
		ComponentWrapResult(popup, newCanvas.child, newCanvas.result)
	}*/
	
	
	// NESTED	------------------------------
	
	private object HierarchyConnection extends ComponentHierarchy
	{
		override def parent = Left(ReachCanvas2.this)
		override def linkPointer = attachmentPointer
		override def isThisLevelLinked = isLinked
	}
	
	private object CustomDrawPanel extends JPanel(null)
	{
		// INITIAL CODE	---------------------
		
		setOpaque(true)
		// setBackground(Color.black.toAwt)
		
		// Makes this canvas element focusable and disables the default focus traversal keys
		if (!disableFocus) {
			setFocusable(true)
			setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
			setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, new util.HashSet[AWTKeyStroke]())
		}
		
		
		// IMPLEMENTED	----------------------
		
		override def paint(g: Graphics) = paintComponent(g)
		override def paintComponent(g: Graphics) =
			currentPainter.foreach { p => Drawer(g.asInstanceOf[Graphics2D]).use(p.paintWith) }
		// Never paints children (because won't have any children)
		override def paintChildren(g: Graphics) = ()
	}
	
	private object FocusKeyListener extends KeyStateListener
	{
		// ATTRIBUTES	------------------------
		
		// Only listens to tabulator presses
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter && KeyStateEvent.keyFilter(KeyEvent.VK_TAB)
		
		
		// IMPLEMENTED	-----------------------
		
		override def onKeyState(event: KeyStateEvent) = {
			// Moves the focus forwards or backwards
			val direction = if (event.keyStatus.shift) Negative else Positive
			focusManager.moveFocus(direction)
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = focusManager.hasFocus
	}
	
	private class CursorSwapper(cursorManager: ReachCursorManager) extends MouseMoveListener with Handleable
	{
		// ATTRIBUTES	-----------------------------
		
		private val minCursorDistance = 10
		
		private var lastMousePosition = Point.origin
		private var lastCursorImage: Option[Image] = None
		
		private lazy val shadeCalculatorPointer = painterPointer.map[Bounds => ColorShade] {
			case Some(painter) => area => painter.averageShadeOf(area)
			case None => _ => Dark
		}
		
		
		// IMPLEMENTED	-----------------------------
		
		// TODO: Consider making cursor adjustments asynchronously (oftentimes shade calculation can take a while)
		override def onMouseMove(event: MouseMoveEvent) = {
			val newPosition = event.mousePosition - position
			if ((lastMousePosition - newPosition).length >= minCursorDistance) {
				lastMousePosition = newPosition
				if (bounds.contains(event.mousePosition)) {
					val newImage = cursorManager.cursorImageAt(newPosition) { shadeCalculatorPointer.value(_) }
					if (!lastCursorImage.contains(newImage)) {
						lastCursorImage = Some(newImage)
						cursorManager.cursorForImage(newImage).foreach(component.setCursor)
					}
				}
			}
		}
	}
}
