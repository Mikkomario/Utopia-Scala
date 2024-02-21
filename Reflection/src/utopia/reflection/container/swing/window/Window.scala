package utopia.reflection.container.swing.window

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.stack.Constrainable
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.firmament.model.enumeration.WindowResizePolicy.User
import utopia.firmament.model.stack.modifier.StackSizeModifier
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.view.mutable.async.{VolatileFlag, VolatileOption}
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.Flag
import utopia.genesis.graphics.FontMetricsWrapper
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.keyboard.Key.Esc
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateHandler, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateListener2, MouseEventGenerator, MouseMoveListener, MouseWheelListener}
import utopia.genesis.handling.template.Handlers
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.genesis.util.Screen
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.button.ButtonLike
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.event.{ResizeListener, StackHierarchyListener}

import java.awt.event.{ComponentAdapter, ComponentEvent, WindowAdapter, WindowEvent}
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

object Window
{
	/**
	  * Creates a new window
	  * @param content            The component placed within this window
	  * @param parent             The window that hosts this window
	  * @param title              Title text displayed on this window (localized, default = empty = no title)
	  * @param resizePolicy       Policy to apply to resizing (default = user may resize this window)
	  * @param screenBorderMargin Margin to place between this window and the screen borders,
	  *                           when there is space (default = 0 px)
	  * @param getAnchor          A function that returns the "anchor" position of this window.
	  *                           Anchor is the point that remains the same whenever the size of this window changes.
	  *                           E.g. if the anchor point remains located at the top left corner of this window, the window
	  *                           will expand right and down when its size increases.
	  *
	  *                           This function accepts the current bounds of this window,
	  *                           where (0,0) is located at the top left corner of the screen.
	  *                           The anchor position must always be returned in this same (absolute) coordinate system.
	  *
	  *                           The default implementation will place the anchor at the center of this window,
	  *                           meaning that this window will expand equally to right and left, top and bottom,
	  *                           provided that there is space available.
	  *
	  * @param borderless         Whether OS window borders should not be used for this window (default = false)
	  * @tparam C Type of content placed within this window
	  * @return A new window
	  */
	def apply[C <: ReflectionStackable with AwtContainerRelated](content: C, parent: Option[java.awt.Window] = None,
	                                                   title: LocalizedString = LocalizedString.empty,
	                                                   resizePolicy: WindowResizePolicy = User,
	                                                   screenBorderMargin: Double = 0.0,
	                                                   getAnchor: Bounds => Point = _.center,
	                                                   borderless: Boolean = false) =
		parent match {
			case Some(parent) =>
				new Dialog[C](parent, content, title, resizePolicy, screenBorderMargin, getAnchor, borderless)
			case None =>
				Frame.windowed[C](content, title, resizePolicy, screenBorderMargin, getAnchor = getAnchor,
					borderless = borderless)
		}
}

/**
  * This is a common wrapper for all window implementations
  * @author Mikko Hilpinen
  * @since 25.3.2019
  **/
// TODO: Component revalidation should be delayed while this window is invisible
abstract class Window[+Content <: ReflectionStackable with AwtComponentRelated]
	extends ReflectionStackable with AwtContainerRelated with Constrainable
{
	// ATTRIBUTES   ----------------
	
	private var _isAttachedToMainHierarchy = false
	private var _constraints = Vector[StackSizeModifier]()
	
	private val cachedStackSize = ResettableLazy { calculatedStackSizeWithConstraints }
	private val generatorActivated = new VolatileFlag()
	
	private val uponCloseAction = VolatileOption[() => Unit]()
	
	private val _closedFlag = Flag()
	/**
	  * A flag that contains true while this window has not yet been closed
	  */
	lazy val notClosedFlag = !_closedFlag
	
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	private val keyStateHandler = KeyStateHandler()
	
	
	// ABSTRACT    -----------------
	
	override def component: java.awt.Window
	
	/**
	  * @return The localized title of this window
	  */
	def title: LocalizedString
	
	/**
	  * The content displayed in this window
	  */
	def content: Content
	
	/**
	  * Whether the OS toolbar should be shown
	  */
	def showsToolBar: Boolean
	
	/**
	  * Whether this window is currently set to full screen mode
	  */
	def fullScreen: Boolean
	
	/**
	  * @return The current resize policy for this window
	  */
	def resizePolicy: WindowResizePolicy
	
	/**
	  * @return The point which is used as the "anchor" when the size of this window changes.
	  *         Whenever changes occur, the anchor point is preserved and this window is moved around it instead.
	  */
	def absoluteAnchorPosition: Point
	
	/**
	  * @return (Minimum) margin placed between this window and the screen borders, when possible.
	  *         This margin (or larger) is kept when this window would otherwise touch the screen border.
	  *         For very large windows, this value may be ignored or the applied margin may be smaller than this value.
	  *         For full screen windows, this value is always ignored.
	  */
	def screenBorderMargin: Double
	
	
	// COMPUTED    ----------------
	
	/**
	  * The insets in around this screen
	  */
	def insets = Insets of component.getInsets
	
	/**
	  * @return Flag that contains true once this window closes
	  */
	def closedFlag = _closedFlag
	/**
	  * @return A future of this window's closing
	  */
	def closeFuture = _closedFlag.future
	
	/**
	  * @return Whether this is the currently focused window
	  */
	def isFocusedWindow = component.isFocused
	
	/**
	  * @return Whether this window has already been closed
	  */
	def isClosed = _closedFlag.isSet
	
	
	// IMPLEMENTED    --------------
	
	override def constraints = _constraints
	
	override def constraints_=(newConstraints: Vector[StackSizeModifier]) =
	{
		_constraints = newConstraints
		revalidate()
	}
	
	override def stackId = hashCode()
	
	override def isAttachedToMainHierarchy = _isAttachedToMainHierarchy
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
	{
		// Informs / affects the content of this window as well
		if (_isAttachedToMainHierarchy != newAttachmentStatus)
		{
			_isAttachedToMainHierarchy = newAttachmentStatus
			fireStackHierarchyChangeEvent(newAttachmentStatus)
			if (newAttachmentStatus)
				content.attachToStackHierarchyUnder(this)
			else
				content.isAttachedToMainHierarchy = false
		}
	}
	
	override def children = Vector(content)
	
	override def stackSize = cachedStackSize.value
	
	override def resetCachedSize() = cachedStackSize.reset()
	
	override def fontMetricsWith(font: Font): FontMetricsWrapper = content.fontMetricsWith(font)
	
	override def visible_=(isVisible: Boolean) = component.setVisible(isVisible)
	
	// Size and position are not cached since the user may resize and move this Window at will, breaking the cached size / position logic
	override def size = Size(component.getWidth, component.getHeight)
	override def size_=(newSize: Size) = component.setSize(newSize.toDimension)
	
	override def position = Point(component.getX, component.getY)
	override def position_=(newPosition: Point) = component.setLocation(newPosition.toAwtPoint)
	
	override def calculatedStackSize =
	{
		val maxSize =
		{
			if (showsToolBar)
				Screen.size - Screen.insetsAt(component.getGraphicsConfiguration).total
			else
				Screen.size
		}
		val normal = (content.stackSize + insets.total).limitedTo(maxSize)
		
		// If on full screen mode, tries to maximize screen size
		if (fullScreen)
			normal.withOptimal(normal.max getOrElse normal.optimal)
		else
			normal
	}
	
	// Each time (content) layout is updated, may resize this window
	override def updateLayout() = updateWindowBounds(resizePolicy.allowsProgramResize)
	
	override def resizeListeners = content.resizeListeners
	override def resizeListeners_=(listeners: Vector[ResizeListener]) = content.resizeListeners = listeners
	
	// Windows have no parents
	override def parent = None
	
	override def visible = component.isVisible
	
	override def background = content.background
	override def background_=(color: Color) = content.background = color
	
	override def isTransparent = content.isTransparent
	
	override def mouseButtonHandler = content.mouseButtonHandler
	override def mouseMoveHandler = content.mouseMoveHandler
	override def mouseWheelHandler = content.mouseWheelHandler
	override def handlers: Handlers = content.handlers
	
	// override def keyStateHandler = content.keyStateHandler
	// override def keyTypedHandler = content.keyTypedHandler
	
	
	// OTHER    --------------------
	
	/**
	  * ADds a new listener to be informed of keyboard events while this window is open
	  * @param listener A listener to inform
	  */
	def addKeyStateListener(listener: KeyStateListener) = keyStateHandler += listener
	
	/**
	  * Displays this window, making it visible
	  */
	def display(gainFocus: Boolean = true) = {
		if (gainFocus)
			visible = true
		else {
			component.setFocusableWindowState(false)
			visible = true
			component.setFocusableWindowState(true)
		}
	}
	
	/**
	  * Setups up basic functionality in window. Should be called after this window has been filled with content and packed.
	  */
	protected def setup() = {
		// Sets transparent background if content doesn't have a background itself
		// (only works in certain conditions. Doesn't work if this window is decorated)
		if (content.isTransparent)
			Try { component.setBackground(Color.black.withAlpha(0.0).toAwt) }
		
		// Sets position and size
		updateWindowBounds(true)
		
		if (!fullScreen)
			position = ((Screen.size - size) / 2).toVector.toPoint
		
		updateContentBounds()
		
		// Registers to update bounds on each size change
		activateResizeHandling()
		
		// Registers self (and content) into stack hierarchy management
		_isAttachedToMainHierarchy = true
		content.attachToStackHierarchyUnder(this)
		
		component.addWindowListener(CloseListener)
	}
	
	/**
	  * Starts mouse event generation for this window
	  * @param actorHandler An ActorHandler that generates the necessary action events
	  */
	def startEventGenerators(actorHandler: ActorHandler)(implicit exc: ExecutionContext) =
	{
		if (generatorActivated.set()) {
			// Starts mouse listening
			val mouseEventGenerator = new MouseEventGenerator(content.component)
			actorHandler += mouseEventGenerator
			mouseEventGenerator.buttonHandler += MouseButtonStateListener2
				.unconditional { e => content.distributeMouseButtonEvent(e) }
			mouseEventGenerator.moveHandler += MouseMoveListener.unconditional { content.distributeMouseMoveEvent(_) }
			mouseEventGenerator.wheelHandler += MouseWheelListener.unconditional { content.distributeMouseWheelEvent(_) }
			CommonMouseEvents.addGenerator(mouseEventGenerator)
			
			// Starts key listening
			KeyboardEvents += keyStateHandler
			
			// Quits event listening once this window finally closes
			uponCloseAction.setOne(() => {
				KeyboardEvents -= keyStateHandler
				CommonMouseEvents.removeGenerator(mouseEventGenerator)
				actorHandler -= mouseEventGenerator
				mouseEventGenerator.stop()
			})
		}
	}
	
	/**
	  * Registers the buttons used in this window so that the default button will be triggered on enter
	  * @param defaultButton The default button for this window
	  * @param moreButtons More buttons for this window
	  */
	def registerButtons(defaultButton: ButtonLike, moreButtons: ButtonLike*) =
		keyStateHandler += DefaultButtonHandler(defaultButton, moreButtons: _*) { isFocusedWindow }
	
	/**
	  * Sets the icon to this window
	  * @param icon New window icon
	  * @param minSize Minimum size allowed for the icon (in pixels). Default = 16x16.
	  */
	def setIcon(icon: Image, minSize: Size = Size(16, 16)) = {
		// Minimum size must be positive
		if (minSize.sign.isNotPositive)
			throw new IllegalArgumentException(s"Icon minimum size must be positive. Now supplied $minSize")
		else {
			// Copies the maximum size icon first
			val original = icon.downscaled
			original.toAwt.foreach { maxImage =>
				val maxSize = Size(maxImage.getWidth, maxImage.getHeight)
				// Case: No smaller icons are allowed
				if (maxSize.fitsWithin(minSize))
					component.setIconImage(maxImage)
				// Case: Multiple icon sizes allowed
				else
				{
					// Shrinks the original image until minimum size is met
					component.setIconImages((maxImage +: Iterator.iterate(original * 0.7) { _ * 0.7 }
						.takeWhile { image => image.width >= minSize.width || image.height >= minSize.height }
						.flatMap { _.toAwt }.toVector).asJava)
				}
			}
		}
	}
	
	/**
	  * Makes it so that this window will close one escape is pressed
	  */
	def setToCloseOnEsc() = keyStateHandler += KeyStateListener.pressed(Esc) { _: KeyStateEvent => close() }
	
	/**
	  * Makes this window become invisible whenever it loses focus
	  */
	def setToHideWhenNotInFocus() = component.addWindowFocusListener(new HideWindowOnFocusLostListener)
	
	/**
	  * Updates the bounds of this window's contents to match those of this window
	  */
	protected def updateContentBounds() = content.size = size - insets.total
	
	/**
	  * Starts following component resizes, updating content size on each resize
	  */
	protected def activateResizeHandling() = {
		component.addComponentListener(new ComponentAdapter {
			// Resizes content each time this window is resized
			// TODO: This will not limit user's ability to resize window beyond minimum and maximum
			override def componentResized(e: ComponentEvent) = {
				updateContentBounds()
				content.updateLayout()
			}
		})
	}
	
	/**
	  * Updates this window's bounds according to changes either outside or inside this window
	  * @param dictateSize Whether this window should dictate the resulting size (Full screen windows always dictate their size)
	  */
	protected def updateWindowBounds(dictateSize: Boolean) =
	{
		if (fullScreen) {
			// Full screen mode always dictates size & position
			if (showsToolBar)
				position = Screen.insetsAt(component.getGraphicsConfiguration).toPoint
			else
				position = Point.origin
			
			size = stackSize.optimal
			updateContentBounds()
		}
		else {
			// In windowed mode, either dictates the new size or just makes sure its within limits
			if (dictateSize) {
				val oldAnchor = absoluteAnchorPosition
				size = stackSize.optimal
				updateContentBounds()
				val newAnchor = absoluteAnchorPosition
				
				// Moves this window so that the anchors overlap. Makes sure screen borders are respected, also.
				positionWithinScreen(position - (newAnchor - oldAnchor))
			}
			else
				checkWindowBounds()
		}
	}
	
	/**
	  * Makes sure the window bounds are within stackSize limits
	  */
	protected def checkWindowBounds() =
	{
		// Checks current bounds against allowed limits
		stackSize.maxWidth.filter { _ < width }.foreach { width = _ }
		stackSize.maxHeight.filter { _ < height }.foreach { height = _ }
		
		if (isUnderSized) {
			size = size bottomRight stackSize.min
			updateContentBounds()
		}
		
		positionWithinScreen(position)
	}
	
	/**
	  * Closes (disposes) this window
	  */
	def close() = component.dispose()
	
	/**
	  * Centers this window on the screen
	  */
	def centerOnScreen() = centerOn(null)
	
	/**
	  * Centers this window on the screen or on the parent component
	  */
	def centerOnParent() = centerOn(component.getParent)
	
	/**
	  * Requests this window to gain focus if it doesn't have it already. Moves this window to the front in the process.
	  */
	def requestFocus() = {
		if (!isFocusedWindow)
			AwtEventThread.async {
				component.toFront()
				component.repaint()
			}
	}
	
	private def centerOn(component: java.awt.Component) = {
		if (fullScreen) {
			if (showsToolBar)
				position = Screen.insetsAt(component.getGraphicsConfiguration).toPoint
			else
				position = Point.origin
		}
		else
			this.component.setLocationRelativeTo(component)
	}
	
	private def positionWithinScreen(proposed: Point) = {
		val insets = Screen.insetsAt(component.getGraphicsConfiguration)
		val screen = Screen.size
		val remainingArea = screen- size - insets.total
		bounds = Bounds.fromFunction2D { axis =>
			val axisInsets = insets(axis)
			lazy val maxLength = screen(axis) - axisInsets.sum
			// Case: Window doesn't fit within the screen => Shrinks
			if (remainingArea(axis) <= 0)
				NumericSpan(axisInsets.first, maxLength)
			else {
				val p = proposed(axis)
				val s = size(axis)
				val margin = screenBorderMargin min (remainingArea(axis) / 2.0)
				
				// Case: Margin should be applied on left / top
				if (p < margin) {
					val start = margin + axisInsets.first
					NumericSpan(start, start + s)
				}
				else {
					val maxEnd = screen(axis) - margin - axisInsets.second
					// Case: Margin should be applied on right / bottom
					if (p + s > maxEnd)
						NumericSpan(maxEnd - s, maxEnd)
					// Case: No margin is needed
					else
						NumericSpan(p, p + s)
				}
			}
		}
	}
	
	
	// NESTED   -------------------------
	
	private object CloseListener extends WindowAdapter
	{
		override def windowClosed(e: WindowEvent) = handleClosing()
		
		override def windowClosing(e: WindowEvent) = handleClosing()
		
		private def handleClosing() = {
			// Performs a closing action, if one is queued
			uponCloseAction.pop().foreach { _() }
			_closedFlag.set()
			
			// Removes this window from the stack hierarchy (may preserve a portion of the content by detaching it first)
			detachFromMainStackHierarchy()
		}
	}
}
