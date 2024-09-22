package utopia.reach.container

import utopia.firmament.awt.AwtComponentExtensions._
import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.stack.Stackable
import utopia.firmament.model.CoordinateTransform
import utopia.firmament.model.stack.StackSize
import utopia.flow.async.context.SingleThreadExecutionContext
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.{EventfulPointer, IndirectPointer, ResettableFlag, SettableOnce}
import utopia.flow.view.mutable.{Resettable, Switch}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.{Drawer, FontMetricsWrapper}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key.{Shift, Tab}
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.Handlers
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorShade.Dark
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.cursor.{CursorSet, ReachCursorManager}
import utopia.reach.dnd.DragAndDropManager
import utopia.reach.drawing.RealTimeReachPaintManager
import utopia.reach.focus.ReachFocusManager

import java.awt.{AWTKeyStroke, Container, Graphics, Graphics2D, KeyboardFocusManager}
import java.util
import javax.swing.event.{AncestorEvent, AncestorListener}
import javax.swing.{JComponent, JPanel}
import scala.concurrent.ExecutionContext

object ReachCanvas
{
	/**
	  * Creates a new Reach canvas
	  * @param attachmentPointer A pointer that contains true while this canvas is attached to a visible window hierarchy
	  * @param absoluteParentPositionView A view into the parent element's absolute position.
	  *                                   Either Left: a view or Right: a real-time pointer (preferred).
	  *                                   Call-by-name.
	  * @param backgroundPointer A pointer that contains the background use in this canvas
	  * @param cursors A set of custom cursors to use on this canvas (optional)
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
	def apply[C <: ReachComponentLike, R](attachmentPointer: Flag,
	                                      absoluteParentPositionView: => Either[View[Point], Changing[Point]],
	                                      backgroundPointer: Changing[Color], cursors: Option[CursorSet] = None,
	                                      enableAwtDoubleBuffering: Boolean = false, disableFocus: Boolean = false)
	                                     (revalidateImplementation: ReachCanvas => Unit)
	                                     (createContent: ComponentHierarchy => ComponentCreationResult[C, R])
	                                     (implicit exc: ExecutionContext, log: Logger) =
	{
		// Creates the canvas first
		val contentPointer = SettableOnce[ReachComponentLike]()
		// The canvas is created in the AWT event thread
		val canvas = AwtEventThread.blocking {
			new ReachCanvas(contentPointer, attachmentPointer, absoluteParentPositionView, backgroundPointer,
				cursors, enableAwtDoubleBuffering, disableFocus)(revalidateImplementation)
		}
		// Then creates the content, using the canvas' component hierarchy
		val newContent = createContent(canvas.hierarchy)
		// Attaches the content to the canvas and returns both
		contentPointer.set(newContent.component)
		newContent in canvas
	}
	
	/**
	  * Creates a new Reach canvas that is suitable to be used as a Swing component (see ReachCanvas.component).
	  * Please note that the primary use of Reach components is not inside a Swing (or even Reflection) system,
	  * but inside a ReachWindow. This function is intended for those rare situations where you want to have a
	  * fixed ReachCanvas layout / component inside a somewhat fixed Swing or AWT component system.
	  * The resulting canvas will never resize itself automatically. If you want the canvas to change size, you
	  * must resize it manually. Remember to call .updateLayout() after any size change.
	  *
	  * @param actorHandler               Actor handler used for delivering action events to the mouse event generator
	  * @param backgroundPointer          A pointer that contains the background use in this canvas.
	  *
	  *                                   Please note that a transparent background is not supported here,
	  *                                   because this canvas must use some background color in order to reverse
	  *                                   the effects of previous paint iteration.
	  *
	  * @param cursors                    A set of custom cursors to use on this canvas (optional)
	  *
	  * @param revalidateListener         A function that is called during ReachCanvas revalidation.
	  *                                   At this point the canvas stack size has been reset.
	  *                                   After this function call the canvas will update its layout.
	  *
	  *                                   This function accepts this canvas instance.
	  *                                   This function should be blocking, and may resize the canvas.
	  *
	  *                                   The default implementation of this function does nothing,
	  *                                   which is suitable in cases where you don't want the canvas to change its size.
	  *
	  * @param enableAwtDoubleBuffering   Whether AWT double buffering should be allowed.
	  *                                   Setting this to true might make the painting less responsive.
	  *                                   Default = false = disable AWT double buffering.
	  * @param disableFocus               Whether this canvas shall not be allowed to gain or manage focus.
	  *                                   Default = false = focus is enabled.
	  *                                   content component that this canvas will wrap.
	  *                                   May return an additional result, which will be returned by this function also.
	  * @param disableMouse               If this is set to true, mouse events are not processed by this canvas element.
	  *                                   It might be useful to set this to true in situations where you want to
	  *                                   process component's mouse events in another way
	  *                                   (since AWT so intelligently doesn't propagate mouse events to parent
	  *                                   components if there exists a mouse listener in the child component).
	  *                                   Default = false.
	  * @param exc                        Implicit execution context
	  * @param log                        Implicit logging implementation for some error cases
	  * @tparam C Type of the component wrapped by this canvas
	  * @tparam R Type of the additional result from the 'createContent' function
	  * @return The created canvas + the created content component + the additional result returned by 'createContent'
	  */
	def forSwing[C <: ReachComponentLike, R](actorHandler: ActorHandler, backgroundPointer: Changing[Color],
	                                         cursors: Option[CursorSet] = None,
	                                         revalidateListener: ReachCanvas => Unit = _ => (),
	                                         enableAwtDoubleBuffering: Boolean = false, disableFocus: Boolean = false,
	                                         disableMouse: Boolean = false)
	                                        (createContent: ComponentHierarchy => ComponentCreationResult[C, R])
	                                        (implicit exc: ExecutionContext, log: Logger) =
	{
		// Performs the canvas creation in the AWT event tread (blocks)
		AwtEventThread.blocking {
			// Implements the attachment tracking etc.
			val attachmentPointer = ResettableFlag()
			val componentPointer = SettableOnce[java.awt.Component]()
			// TODO: Utilize the ancestor listener in caching & resetting this value
			val absoluteParentPositionView = ResettableLazy {
				componentPointer.value match {
					case Some(component) =>
						component.parentsIterator.takeTo { _.isInstanceOf[java.awt.Window] }
							.foldLeft(Point.origin) { (p, c) =>
								c match {
									case window: java.awt.Window =>
										val insets = Insets.of(window.getInsets)
										p + Point.of(window.getLocation) + insets.toPoint
									case component => p + Point.of(component.getLocation)
								}
							}
					case None => Point.origin
				}
			}
			// Creates the canvas
			val canvas = apply(attachmentPointer, Left(absoluteParentPositionView), backgroundPointer, cursors,
				enableAwtDoubleBuffering, disableFocus) { canvas =>
				// The revalidation is performed synchronously, and doesn't necessarily alter canvas size in any way
				canvas.resetCachedSize()
				revalidateListener(canvas)
				canvas.updateLayout()
			}(createContent)
			componentPointer.set(canvas.component)
			val tracker = new SwingAttachmentTracker(actorHandler, canvas, attachmentPointer,
				absoluteParentPositionView, mouseDisabled = disableMouse)
			canvas.component.addAncestorListener(tracker)
			
			// Initializes canvas size
			canvas.size = canvas.stackSize.optimal
			
			canvas
		}
	}
	
	
	// NESTED   ----------------------------
	
	private class SwingAttachmentTracker(actorHandler: ActorHandler, canvas: ReachCanvas, attachedFlag: Switch,
	                                     absolutePositionView: Resettable, mouseDisabled: Boolean)
	                                    (implicit exc: ExecutionContext, log: Logger)
		extends AncestorListener
	{
		// ATTRIBUTES   --------------------
		
		// In some circumstances, mouse event generation is disabled altogether
		private val parentPointer = {
			if (mouseDisabled)
				None
			else {
				val p = EventfulPointer.empty[java.awt.Component]
				new SwingMouseEventConverter(actorHandler, canvas, p)
				Some(p)
			}
		}
		
		
		// IMPLEMENTED  --------------------
		
		override def ancestorAdded(event: AncestorEvent) = {
			attachedFlag.set()
			parentPointer.foreach { _.value = Some(canvas.component.getParent) }
		}
		override def ancestorRemoved(event: AncestorEvent) = {
			parentPointer.foreach { _.value = None }
			attachedFlag.reset()
		}
		
		override def ancestorMoved(event: AncestorEvent) = absolutePositionView.reset()
	}
	
	private class SwingMouseEventConverter(actorHandler: ActorHandler, canvas: ReachCanvas,
	                                       parentPointer: Changing[Option[java.awt.Component]])
	                                      (implicit exc: ExecutionContext, log: Logger)
	{
		// ATTRIBUTES   -----------------------
		
		private val generatorPointer = parentPointer.strongMap { _.map { new MouseEventGenerator(_) } }
		
		
		// INITIAL CODE --------------------
		
		// Distributes the mouse events from the parent component into the canvas element
		generatorPointer.addContinuousListener { event =>
			event.oldValue.foreach { _.stop() }
			event.newValue.foreach { generator =>
				generator.buttonHandler += MouseButtonStateListener.unconditional { event =>
					canvas.distributeMouseButtonEvent(event)
				}
				generator.moveHandler += MouseMoveListener.unconditional(canvas.distributeMouseMoveEvent)
				generator.wheelHandler += MouseWheelListener.unconditional(canvas.distributeMouseWheelEvent)
				actorHandler += generator
			}
		}
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
  *                                   Either Left: a view or Right: a real-time pointer (preferred).
  *                                   Call-by-name.
  * @param backgroundPointer A pointer that contains the background use in this canvas
  * @param cursors Custom cursors to display on this canvas. Default = None = no custom cursor management enabled
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
  */
class ReachCanvas protected(contentPointer: Changing[Option[ReachComponentLike]], val attachmentPointer: Flag,
                            absoluteParentPositionView: => Either[View[Point], Changing[Point]],
                            backgroundPointer: Changing[Color], cursors: Option[CursorSet] = None,
                            enableAwtDoubleBuffering: Boolean = false, disableFocus: Boolean = false)
                           (revalidateImplementation: ReachCanvas => Unit)
                           (implicit exc: ExecutionContext)
	extends ReachCanvasLike with Stackable
{
	// ATTRIBUTES	---------------------------
	
	private val layoutUpdateQueue = Volatile.seq[Seq[ReachComponentLike]]()
	private val updateFinishedQueue = Volatile.seq[() => Unit]()
	
	override val focusManager = new ReachFocusManager(CustomDrawPanel)
	private val painterPointer = contentPointer.map { _.map { c =>
		RealTimeReachPaintManager(c, Some(backgroundPointer.value).filter { _.alpha > 0 },
			disableDoubleBuffering = !enableAwtDoubleBuffering)
	} }
	override val cursorManager = cursors.map { new ReachCursorManager(_) }
	private val cursorSwapper = cursorManager.map { new CursorSwapper(_) }
	/**
	 * The drag and drop -manager used by this canvas
	 */
	lazy val dragAndDropManager = DragAndDropManager(this)
	
	/**
	  * A pointer that contains the up-to-date bounds of this canvas
	  */
	val boundsPointer = EventfulPointer(Bounds.zero)
	// Uses an immutable version of the position and size pointer locally, exposes mutable versions publicly
	private val _positionPointer = boundsPointer.map { _.position }
	/**
	  * A pointer that contains the up-to-date position of this canvas' top-left corner
	  */
	lazy val positionPointer = IndirectPointer(_positionPointer) { position = _ }
	private val _sizePointer = boundsPointer.map { _.size }
	/**
	  * A pointer that contains the up-to-date size of this canvas
	  */
	lazy val sizePointer = IndirectPointer(_sizePointer) { size = _ }
	/**
	  * A view into the absolute position (i.e. position on screen) of this canvas element
	  */
	// Caches calculations if possible
	lazy val absolutePositionView = absoluteParentPositionView match {
		case Right(c) => Right(c.mergeWith(_positionPointer) { _ + _ })
		case Left(v) => Left(View { v.value + position })
	}
	
	override val mouseButtonHandler = MouseButtonStateHandler()
	override val mouseMoveHandler = MouseMoveHandler()
	override val mouseWheelHandler = MouseWheelHandler()
	private val mouseDragHandler = MouseDragHandler()
	
	override val handlers: Handlers =
		Handlers(mouseButtonHandler, mouseMoveHandler, mouseWheelHandler, mouseDragHandler)
	
	
	// INITIAL CODE	---------------------------
	
	// Initializes component state
	AwtEventThread.async {
		component.setFocusable(!disableFocus)
		val initialBackground = backgroundPointer.value
		component.setBackground(initialBackground.toAwt)
		component.setOpaque(initialBackground.alpha >= 1.0)
	}
	
	// When bounds get updated, updates the underlying component, also
	_positionPointer.addContinuousListener { e => AwtEventThread.async { component.setLocation(e.newValue.toAwtPoint) } }
	_sizePointer.addContinuousListener { e => AwtEventThread.async { component.setSize(e.newValue.toDimension) } }
	
	// Updates component background when appropriate
	backgroundPointer.addContinuousListener { e =>
		component.setBackground(e.newValue.toAwt)
		component.setOpaque(e.newValue.alpha >= 1.0)
		repaint()
	}
	
	attachmentPointer.addContinuousListener { event =>
		// When attached to the stack hierarchy,
		// makes sure to update immediate content layout and repaint this component
		if (event.newValue) {
			currentPainter.foreach { _.resetBuffer() }
			// TODO: Do we need to reset every stack size or just the top level?
			currentContent.foreach { _.resetEveryCachedStackSize() }
			layoutUpdateQueue.clear()
			updateWholeLayout(size)
			super[ReachCanvasLike].repaint()
			// Listens to tabulator key events for manual focus handling
			if (!disableFocus)
				KeyboardEvents += FocusKeyListener
		}
		else
			KeyboardEvents -= FocusKeyListener
	}
	
	// Listens to mouse events for cursor-swapping
	cursorSwapper.foreach(addMouseMoveListener)
	
	// Sets up mouse drag event -generation
	handlers += new DragTracker(
		MouseDragListener { event => currentContent.foreach { _.distributeMouseDragEvent(event) } })
	
	
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
	def absolutePosition = absolutePositionView.either.value
	/**
	  * @return The window that hosts this canvas instance
	  */
	def parentWindow = component.parentWindow
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def currentContent = contentPointer.value
	override protected def currentPainter: Option[RealTimeReachPaintManager] = painterPointer.value
	
	override def stackSize = currentContent match {
		case Some(content) => content.stackSize
		case None => StackSize.any
	}
	
	override def bounds: Bounds = boundsPointer.value
	// Could apply these updates once the content is initialized,
	// but this feature shall not be implemented unless it becomes necessary
	// Please note that bounds and size updates don't come into effect before updateLayout() is called
	override def bounds_=(b: Bounds): Unit = boundsPointer.value = b
	override def position_=(p: Point): Unit = boundsPointer.update { _.withPosition(p) }
	override def size_=(s: Size): Unit = boundsPointer.update { _.withSize(s) }
	
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
	
	// Distributes the events via the canvas content element, but transforms the coordinates relative to this canvas
	override def distributeMouseButtonEvent(event: MouseButtonStateEvent) = {
		super.distributeMouseButtonEvent(event) match {
			case consume: Consume =>
				val newEvent = event.consumed(consume.consumeEvent)
				currentContent.foreach { _.distributeMouseButtonEvent(newEvent.relativeTo(position)) }
				consume
			case _ =>
				currentContent match {
					case Some(c) => c.distributeMouseButtonEvent(event.relativeTo(position))
					case None => Preserve
				}
		}
	}
	override def distributeMouseMoveEvent(event: MouseMoveEvent) = {
		super.distributeMouseMoveEvent(event)
		currentContent.foreach { _.distributeMouseMoveEvent(event.relativeTo(position)) }
	}
	override def distributeMouseWheelEvent(event: MouseWheelEvent) = {
		super.distributeMouseWheelEvent(event) match {
			case consume: Consume =>
				val newEvent = event.consumed(consume.consumeEvent)
				currentContent.foreach { _.distributeMouseWheelEvent(newEvent.relativeTo(position)) }
				consume
			case _ =>
				currentContent match {
					case Some(c) => c.distributeMouseWheelEvent(event.relativeTo(position))
					case None => Preserve
				}
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
	
	
	// NESTED	------------------------------
	
	private object HierarchyConnection extends ComponentHierarchy
	{
		override def parent = Left(ReachCanvas.this)
		override def linkPointer = attachmentPointer
		override def isThisLevelLinked = isLinked
		override def top = ReachCanvas.this
		override def coordinateTransform: Option[CoordinateTransform] = None
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
		override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter(Tab)
		
		
		// IMPLEMENTED	-----------------------
		
		override def handleCondition: Flag = AlwaysTrue
		
		override def onKeyState(event: KeyStateEvent) = {
			if (focusManager.hasFocus) {
				// Moves the focus forwards or backwards
				val direction = if (event.keyboardState(Shift)) Negative else Positive
				focusManager.moveFocus(direction)
			}
		}
	}
	
	private class CursorSwapper(cursorManager: ReachCursorManager) extends MouseMoveListener
	{
		// ATTRIBUTES	-----------------------------
		
		// Uses a custom execution context for optimization
		private val swapExc = new SingleThreadExecutionContext("Cursor swapper")
		
		private val minCursorDistance = 10
		private val mousePositionPointer = EventfulPointer(Point.origin)
		
		private lazy val defaultBackgroundShadePointer = backgroundPointer
			.lazyMap { c => if (c.alpha > 0.0) c.shade else Dark }
		
		
		// INITIAL CODE -----------------------------
		
		// Calculates and changes the cursor image asynchronously
		attachmentPointer.onceSet {
			val defaultCursor = cursorManager.cursors.default.light
			mousePositionPointer
				// Calculates new cursor image to use when mouse position changes
				.mapAsync(defaultCursor, skipInitialMap = true) { position =>
					cursorManager.cursorImageAt(position) { _ => defaultBackgroundShadePointer.value }
				}(swapExc)
				// Whenever the image gets updated, changes the component cursor
				.addListener { e =>
					cursorManager.cursorForImage(e.newValue).foreach { cursor =>
						AwtEventThread.blocking { component.setCursor(cursor) }
					}
				}
		}
		
		
		// IMPLEMENTED	-----------------------------
		
		override def handleCondition: Flag = AlwaysTrue
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseMove(event: MouseMoveEvent) = {
			val newPosition = event.position - position
			mousePositionPointer.update { lastPosition =>
				if (newPosition.distanceFrom(lastPosition) >= minCursorDistance)
					newPosition
				else
					lastPosition
			}
		}
	}
}
