package utopia.firmament.component

import utopia.firmament.awt.AwtComponentExtensions._
import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.Window.minIconSize
import utopia.firmament.component.stack.{CachingStackable, Stackable}
import utopia.firmament.context.{ComponentCreationDefaults, WindowContext}
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.flow.async.process.Delay
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.event.model.DetachmentChoice
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.view.mutable.eventful.{Flag, IndirectPointer, PointerWithEvents, ResettableFlag}
import utopia.genesis.event.{MouseButtonStateEvent, MouseEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.graphics.FontMetricsWrapper
import utopia.genesis.handling._
import utopia.genesis.handling.mutable.{ActorHandler, KeyStateHandler, MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.genesis.util.Screen
import utopia.genesis.view.{GlobalKeyboardEventHandler, GlobalMouseEventHandler, MouseEventGenerator}
import utopia.inception.util.Filter
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.{Bounds, Insets, Point, Size}

import java.awt.Frame
import java.awt.event.{ComponentAdapter, ComponentEvent, KeyEvent, WindowAdapter, WindowEvent}
import javax.swing.{JDialog, JFrame}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Try

object Window
{
	// ATTRIBUTES   ------------------------
	
	// The smallest allowed window icon size
	private val minIconSize = Size.square(16)
	
	
	// OTHER    ----------------------------
	
	/**
	  * Creates a new window (a frame or a dialog)
	  * @param container         The (awt) container that will be used as the root content panel for this window.
	  *                          This container is assumed to be associated with the specified 'content' parameter.
	  * @param content           The root component within this window.
	  *                          The size of this component and this window are linked together.
	  * @param eventActorHandler An actor handler that distributes action events for this window's mouse event generator.
	  * @param parent              The window that will host this dialog. None if this window shall not have a parent
	  *                            (will construct a frame). Default = None.
	  * @param title               Title displayed in the OS header of this window (if applicable). Default = empty.
	  * @param resizeLogic         The logic that should be applied to window resizing.
	  *                            Default = Program = Only the program may resize this window (i.e. user can't)
	  * @param screenBorderMargins Additional margins that are placed on the screen edges, which are not covered by this
	  *                            window if at all possible.
	  *                            Default = no margins
	  * @param getAnchor           A function for determining the so-called anchor position within this window's bounds on screen.
	  *                            When this window is resized, the anchor position is not moved if at all possible.
	  *                            Default = center = The center of this window will remain in the same place upon resize, if possible.
	  * @param icon                Icon displayed on this window, initially.
	  *                            Default = common default (see [[ComponentCreationDefaults]])
	  * @param borderless          Whether this window is 'undecorated', i.e. has no OS headers or borders.
	  *                            Set to true if you implement your own header,
	  *                            or if you're creating a temporary pop-up window.
	  *                            Notice that without the OS header, the user can't move this window by default.
	  *                            Default = false = use OS header.
	  * @param fullScreen          Whether this window should be set to fill the whole screen whenever possible.
	  *                            Default = false.
	  * @param disableFocus        Whether this window shall not be allowed to gain focus.
	  *                            Default = false.
	  * @param ignoreScreenInsets  Whether this window should ignore the screen insets (such as the OS toolbar)
	  *                            when positioning itself.
	  *                            Set to true if you want to cover the toolbar (in some full-screen use-cases, for example).
	  *                            Default = false.
	  * @param enableTransparency  Whether this window shall be allowed to become transparent, when possible.
	  *                            This requires that the specified 'container' background is set to a transparent
	  *                            (alpha < 100%) color. Transparency is only enabled on windows without OS borders, and
	  *                            even in those cases transparency doesn't always work.
	  *                            Default = false = transparency is always disabled.
	  * @param exc                 Implicit execution context
	  * @return A new window
	  */
	def apply(container: java.awt.Container, content: Stackable, eventActorHandler: ActorHandler,
	          parent: Option[java.awt.Window], title: LocalizedString = LocalizedString.empty,
	          resizeLogic: WindowResizePolicy = Program, screenBorderMargins: Insets = Insets.zero,
	          getAnchor: Bounds => Point = _.center, icon: Image = ComponentCreationDefaults.windowIcon,
	          borderless: Boolean = false, fullScreen: Boolean = false, disableFocus: Boolean = false,
	          ignoreScreenInsets: Boolean = false, enableTransparency: Boolean = false)
	         (implicit exc: ExecutionContext) =
	{
		val window = parent match {
			case Some(parent) => Left(new JDialog(parent, title.string))
			case None => Right(new JFrame(title.string))
		}
		new Window(window, container, content, eventActorHandler, resizeLogic, screenBorderMargins, getAnchor, icon,
			!borderless, fullScreen, !disableFocus, !ignoreScreenInsets, enableTransparency)
	}
	
	/**
	  * Creates a new window utilizing a component creation context
	  * @param container           The (awt) container that will be used as the root content panel for this window.
	  *                            This container is assumed to be associated with the specified 'content' parameter.
	  * @param content             The root component within this window.
	  *                            The size of this component and this window are linked together.
	  * @param parent              The window that will host this dialog. None if this window shall not have a parent
	  *                            (will construct a frame). Default = None.
	  * @param title               Title displayed in the OS header of this window (if applicable). Default = empty.
	  * @param getAnchor           A function for determining the so-called anchor position within this window's bounds on screen.
	  *                            When this window is resized, the anchor position is not moved if at all possible.
	  *                            Default = center = The center of this window will remain in the same place upon resize, if possible.
	  * @return A new window
	  */
	def contextual(container: java.awt.Container, content: Stackable, parent: Option[java.awt.Window] = None,
	               title: LocalizedString = LocalizedString.empty, getAnchor: Bounds => Point = _.center)
	              (implicit context: WindowContext, exc: ExecutionContext) =
		apply(container, content, context.actorHandler, parent, title, context.windowResizeLogic,
			context.screenBorderMargins, getAnchor, context.icon, !context.windowBordersEnabled,
			context.fullScreenEnabled, !context.focusEnabled, !context.screenInsetsEnabled, context.transparencyEnabled)
}

/**
  * Wraps an awt window, providing an interface for it.
  * Please note that this class doesn't handle component revalidation.
  * I.e. when the size of the content needs to be adjusted, this window will not recognize it by default.
  * Please call [[resetCachedSize()]], [[optimizeBounds()]] and [[updateLayout()]] when you wish to "revalidate"
  * this window's layout.
  *
  * @author Mikko Hilpinen
  * @since 12.4.2023
  *
  * @constructor Wraps a window
  * @param wrapped The wrapped window. Either
  *                     Left: A dialog, or
  *                     Right: A frame
  * @param container The (awt) container that will be used as the root content panel for this window.
  *                  This container is assumed to be associated with the specified 'content' parameter.
  * @param content The root component within this window.
  *                The size of this component and this window are linked together.
  * @param eventActorHandler An actor handler that distributes action events for this window's mouse event generator.
  * @param resizeLogic The logic that should be applied to window resizing.
  *                    Default = Program = Only the program may resize this window (i.e. user can't)
  * @param screenBorderMargins Additional margins that are placed on the screen edges, which are not covered by this
  *                            window if at all possible.
  *                            Default = no margins
  * @param getAnchor A function for determining the so-called anchor position within this window's bounds on screen.
  *                  When this window is resized, the anchor position is not moved if at all possible.
  *                  Default = center = The center of this window will remain in the same place upon resize, if possible.
  * @param initialIcon Icon displayed on this window, initially.
  *                    Default = common default (see [[ComponentCreationDefaults]])
  * @param hasBorders Whether this window is 'decorated', i.e. has the main OS header and borders.
  *                   Set to false if you implement your own header, or if you're creating a temporary pop-up window.
  *                   Notice that without the OS header, the user can't move this window by default.
  *                   Default = true = use OS header.
  * @param isFullScreen Whether this window should be set to fill the whole screen whenever possible.
  *                     Default = false.
  * @param isFocusable Whether this window is allowed to gain focus.
  *                    Default = true.
  * @param respectScreenInsets Whether this window takes the screen insets (such as the OS toolbar) into account
  *                            when positioning itself.
  *                            Set to false if you want to cover the toolbar (in some full-screen use-cases, for example).
  *                            Default = true.
  * @param enableTransparency Whether this window shall be allowed to become transparent, when possible.
  *                           This requires that the specified 'container' background is set to a transparent
  *                           (alpha < 100%) color. Transparency is only enabled on windows without OS borders, and
  *                           even in those cases transparency doesn't always work.
  *                           Default = false = transparency is always disabled.
  */
class Window(protected val wrapped: Either[JDialog, JFrame], container: java.awt.Container, content: Stackable,
             eventActorHandler: ActorHandler, resizeLogic: WindowResizePolicy = Program,
             screenBorderMargins: Insets = Insets.zero, getAnchor: Bounds => Point = _.center,
             initialIcon: Image = ComponentCreationDefaults.windowIcon, val hasBorders: Boolean = true,
             isFullScreen: Boolean = false, val isFocusable: Boolean = true, respectScreenInsets: Boolean = true,
             enableTransparency: Boolean = false)
            (implicit exc: ExecutionContext)
	extends CachingStackable
{
	// ATTRIBUTES   ----------------
	
	override val mouseButtonHandler = MouseButtonStateHandler()
	override val mouseMoveHandler = MouseMoveHandler()
	override val mouseWheelHandler = MouseWheelHandler()
	
	/**
	  * @return The AWT window wrapped by this window
	  */
	val component = wrapped.either
	
	// Caches screen size
	private lazy val screenSize = Screen.actualSize
	private lazy val screenInsets = Screen.actualInsetsAt(component.getGraphicsConfiguration)
	
	// Allows mutable access to the display icon
	val iconPointer = new PointerWithEvents(initialIcon)
	
	// Stores window state in private flags
	// These are only updated based on awt window events
	private val _openedFlag = Flag()
	private val _closedFlag = Flag()
	private val _visibleFlag = ResettableFlag()
	private val _minimizedFlag = ResettableFlag()
	private val _activeFlag = ResettableFlag()
	private val _focusedFlag = ResettableFlag()
	
	// Stores position and size in pointers, which are only updated on window events
	private val _positionPointer = new PointerWithEvents(Point.origin)
	private val _sizePointer = new PointerWithEvents(Size.zero)
	
	// Stores calculated anchor, which is used in repositioning after size changes
	// This pointer is cleared after the anchor has been resolved / actuated
	private val pendingAnchor = VolatileOption[Point]()
	
	/**
	  * A flag that contains true whenever this window is fully visible
	  * (i.e. open, visible and not minimized)
	  */
	val fullyVisibleFlag = _visibleFlag.mergeWith(_minimizedFlag) { _ && !_ }
	
	/**
	  * A future that resolves once this window is displayed for the first time
	  */
	val openedFuture = fullyVisibleFlag.findMapFuture { if (_) Some(()) else None }
	
	/**
	  * A flag that contains true while this window is open.
	  * From the creation of this window until the first call of visible = true, this flag contains false.
	  * From first visible = true to the closing of this window, this flag contains true.
	  * After the closing of this window, this flag contains false.
	  */
	lazy val openFlag = _openedFlag.mergeWith(_closedFlag) { _ && !_ }
	
	// Merges the position and size in order to form bounds
	// Provides a separate interface for users
	private lazy val _boundsPointer = _positionPointer.mergeWith(_sizePointer) { Bounds(_, _) }
	
	// Provides a custom set() function for some of the flags
	/**
	  * A flag that is set when this window becomes visible for the first time.
	  * Setting this flag will display this window (unless already set).
	  */
	lazy val openedFlag = Flag.wrap(_openedFlag) {
		if (_openedFlag.isSet)
			false
		else
			visible = true
	}
	/**
	  * A flag that is set once this window closes.
	  * Setting this flag will close this window (unless closed already)
	  */
	lazy val closedFlag = Flag.wrap(_closedFlag) {
		// Case: Already closed => No operation
		if (_closedFlag.isSet)
			false
		// Case: Closes (disposes) the window
		else {
			close()
			true
		}
	}
	
	// Provides mutability to certain pointers
	/**
	  * A pointer that contains true while this window is visible.
	  * Changing the value of this pointer will make this window visible or invisible.
	  *
	  * Please notice that this pointer may contain true while this window is minimized.
	  * If you want to track whether this window is actually (fully) displayed, use [[fullyVisibleFlag]]
	  */
	lazy val visiblePointer = IndirectPointer(_visibleFlag) { visible = _ }
	/**
	  * A pointer that contains true while this window is minimized / iconified.
	  * Changing the value of this pointer will minimize or normalize this window
	  * (except when this window is invisible).
	  */
	lazy val minimizedPointer = IndirectPointer(_minimizedFlag) { minimized = _ }
	/**
	  * A mutable pointer that matches the current position of this window.
	  */
	lazy val positionPointer = IndirectPointer(_positionPointer) { position = _ }
	/**
	  * A mutable pointer that matches the current size of this window.
	  * Please note that window size contains window insets,
	  * which are not included in the space available to window contents.
	  */
	lazy val sizePointer = IndirectPointer(_sizePointer) { size = _ }
	/**
	  * A mutable pointer that matches the current bounds of this window.
	  * Please note that window bounds contain window insets,
	  * which are not included in the space available to window contents.
	  */
	lazy val boundsPointer = IndirectPointer(_boundsPointer) { bounds = _ }
	
	/**
	  * The insets around this window.
	  * Includes the window OS header, for example (unless borderless)
	  */
	lazy val insets = Insets of component.getInsets
	
	// The key-state handler is initialized only when necessary
	private val keyStateHandlerPointer = Lazy {
		val handler = KeyStateHandler()
		// Only activates the handler once this window is open
		if (isOpen)
			GlobalKeyboardEventHandler += handler
		handler
	}
	/**
	  * A handler that distributes keyboard events, but only while this window is the focused window.
	  */
	lazy val focusKeyStateHandler = {
		val parent = keyStateHandlerPointer.value
		val handler = KeyStateHandler()
		handler.filter = _ => isFocused
		parent += handler
		handler
	}
	
	
	// INITIAL CODE ----------------
	
	AwtEventThread.async {
		// Starts tracking window state
		component.addWindowListener(WindowStateListener)
		
		// Sets up the underlying window
		component.setLayout(null)
		component.setContentPane(container)
		// Some of the functions are only available through the two separate sub-classes
		wrapped match {
			case Left(dialog) =>
				dialog.setUndecorated(isBorderless)
				dialog.setResizable(resizeLogic.allowsUserResize)
			case Right(frame) =>
				frame.setUndecorated(isBorderless)
				frame.setResizable(resizeLogic.allowsUserResize)
		}
		component.setFocusableWindowState(isFocusable)
		component.setFocusable(isFocusable)
		component.pack()
		
		// Sets transparent background if content doesn't have a background itself
		// (only works in certain conditions. Doesn't work if this window is decorated)
		if (enableTransparency && isBorderless && container.isBackgroundSet && container.getBackground.getAlpha < 255)
			Try { component.setBackground(Color.black.withAlpha(0.0).toAwt) }
		
		// Initializes position and size
		_positionPointer.value = Point.of(component.getLocation)
		_sizePointer.value = Size.of(component.getSize)
		if (isNotFullScreen)
			_boundsPointer.onNextChange { _ => centerOnParent() }
		AwtEventThread.async { optimizeBounds(dictateSize = true) }
		
		// Registers to update the state when the wrapped window updates
		component.addComponentListener(WindowComponentStateListener)
		
		// Updates the window icon when appropriate
		iconPointer.addListenerAndSimulateEvent(Image.empty) { e =>
			// Is not interested in icon changes after this window has closed
			if (hasClosed)
				DetachmentChoice.detach
			else {
				// Copies the maximum size icon first
				val original = e.newValue.downscaled
				original.toAwt.foreach { maxImage =>
					val maxSize = Size(maxImage.getWidth, maxImage.getHeight)
					AwtEventThread.async {
						// Case: No smaller icons are allowed
						if (maxSize.fitsWithin(minIconSize))
							component.setIconImage(maxImage)
						// Case: Multiple icon sizes allowed
						else {
							// Shrinks the original image until minimum size is met
							component.setIconImages((maxImage +: Iterator.iterate(original * 0.7) { _ * 0.7 }
								.takeWhile { _.size.existsDimensionWith(minIconSize) { _ >= _ } }
								.flatMap { _.toAwt }.toVector).asJava)
						}
					}
				}
				DetachmentChoice.continue
			}
		}
		
		// Once this window is open, starts event handling
		openedFuture.foreach { _ =>
			// Starts mouse listening (which is active only while visible)
			val mouseEventGenerator = new MouseEventGenerator(container)
			eventActorHandler += mouseEventGenerator
			val whileVisibleFilter: Filter[Any] = _ => isFullyVisible
			mouseEventGenerator.buttonHandler += MouseButtonStateListener(whileVisibleFilter) { e =>
				content.distributeMouseButtonEvent(e)
				None
			}
			mouseEventGenerator.moveHandler += MouseMoveListener(whileVisibleFilter)(content.distributeMouseMoveEvent)
			mouseEventGenerator.wheelHandler += MouseWheelListener(whileVisibleFilter)(content.distributeMouseWheelEvent)
			GlobalMouseEventHandler.registerGenerator(mouseEventGenerator)
			
			// Starts key listening (if used)
			keyStateHandlerPointer.current.foreach { GlobalKeyboardEventHandler += _ }
			
			// Quits event listening once this window closes
			closeFuture.onComplete { _ =>
				eventActorHandler -= mouseEventGenerator
				mouseEventGenerator.kill()
				keyStateHandlerPointer.current.foreach { GlobalKeyboardEventHandler -= _ }
				GlobalMouseEventHandler.unregisterGenerator(mouseEventGenerator)
			}
		}
		
		// Whenever this window becomes visible, updates content layout
		// (layout updates are skipped while this window is not visible)
		fullyVisibleFlag.addListener { e =>
			if (hasClosed)
				DetachmentChoice.detach
			else {
				if (e.newValue) {
					content.resetCachedSize()
					resetCachedSize()
					if (!optimizeBounds()) {
						updateLayout()
						content.updateLayout()
					}
				}
				AwtEventThread.async { component.repaint() }
				DetachmentChoice.continue
			}
		}
	}
	
	
	// COMPUTED    ----------------
	
	/**
	  * @return True if this window doesn't contain the OS borders / header
	  */
	def isBorderless = !hasBorders
	
	/**
	  * @return Contains true if this window doesn't fill the screen by default.
	  */
	def isNotFullScreen = !isFullScreen
	
	/**
	  * @return Whether this window has become visible at least once.
	  *         Please note that this returns true even after this window has closed.
	  *         If you want to test whether this window is currently open, please use [[isOpen]] instead.
	  */
	def hasOpened = _openedFlag.value
	/**
	  * @return Whether this window has not yet become visible for the first time.
	  */
	def hasNotOpened = !hasOpened
	
	/**
	  * @return Whether this window is currently open.
	  *         Please note that this returns false before this window is first made visible,
	  *         and also after this window closes.
	  *         See also: [[visible]] and [[hasNotClosed]]
	  */
	def isOpen = openFlag.value
	/**
	  * @return Whether this window is not currently open.
	  *         Returns true in two cases:
	  *             1) Before this window first becomes visible, and
	  *             2) After this window closes.
	  *         See also: [[visible]] and [[hasClosed]]
	  */
	def isNotOpen = !isOpen
	
	/**
	  * @return Whether this window has already closed
	  */
	def hasClosed = _closedFlag.value
	/**
	  * @return Whether this window has not yet closed
	  */
	def hasNotClosed = !hasClosed
	/**
	  * @return A future that resolves once this window closes
	  */
	def closeFuture = _closedFlag.future
	
	/**
	  * @return Whether this window is currently visible.
	  *         Note that this is sometimes true even when this window is minimized / iconified.
	  */
	def visible = _visibleFlag.value
	/**
	  * Makes this window visible or invisible.
	  * Please note that this function has no effect after this window has closed.
	  * @param visible Whether this window should be visible
	  * @return Whether the state of this window was changed
	  */
	def visible_=(visible: Boolean) = {
		// Won't allow visibility changes after this window has closed
		if (hasClosed || this.visible == visible)
			false
		else {
			val oldState = this.visible
			AwtEventThread.async { component.setVisible(visible) }
			oldState != visible
		}
	}
	
	/**
	  * @return Whether this window is currently minimized / iconified.
	  */
	def minimized = _minimizedFlag.value
	/**
	  * Minimizes or normalizes this window.
	  * Please note that this function has no effect after this window has closed.
	  * @param minimize Whether this window should be minimized
	  * @return Whether this window's state was affected
	  */
	def minimized_=(minimize: Boolean) = {
		// Case: Already has the specified state => No change
		if (hasClosed || minimize == minimized)
			false
		// Case: Alters state
		else {
			// Only frames can be minimized
			val frame: Option[Frame] = wrapped match {
				// Case: Dialog => Targets the root frame
				case Left(dialog) => dialog.parentFrame
				// Case: Frame
				case Right(frame) => Some(frame)
			}
			frame match {
				// Case: Frame found => Minimizes or restores
				case Some(frame) =>
					frame.setState(if (minimize) Frame.ICONIFIED else Frame.NORMAL)
					true
				// Case: No frame found => No change
				case None => false
			}
		}
	}
	
	/**
	  * @return Whether this window is currently fully visible (i.e. visible and not minimized)
	  */
	def isFullyVisible = fullyVisibleFlag.value
	/**
	  * @return Whether this window is at least partially hidden at this time (i.e. either invisible or minimized)
	  */
	def isNotFullyVisible = !isFullyVisible
	
	/**
	  * @return Whether this is the current active window in the OS
	  *         (i.e. belongs to the window group that has the focus)
	  */
	def isActive = _activeFlag.value
	/**
	  * @return Whether this window is currently not active in the OS,
	  *         meaning that there is another application that's active.
	  */
	def isNotActive = !isActive
	/**
	  * @return A flag that contains true whenever this window is the active window in the OS.
	  */
	def activeFlag = _activeFlag.view
	
	/**
	  * @return Whether this window is the currently focused window
	  */
	def isFocused = _focusedFlag.value
	/**
	  * @return Whether this window is not the currently focused window
	  */
	def isNotFocused = !isFocused
	/**
	  * @return A flag that contains true whenever this is the focused window
	  */
	def focusedFlag = _focusedFlag.view
	
	/**
	  * @return The current icon displayed on this window.
	  *         Empty if the default (Java) icon is displayed.
	  */
	def icon = iconPointer.value
	/**
	  * Changes the icon displayed on this window
	  * @param newIcon New icon to display
	  */
	def icon_=(newIcon: Image) = iconPointer.value = newIcon
	
	/**
	  * @return A key-state handler that distributes keyboard events while this window is open.
	  *         If you only want to receive keyboard events while this window is in focus, please use
	  *         [[focusKeyStateHandler]] instead.
	  */
	def keyStateHandler = keyStateHandlerPointer.value
	
	/**
	  * @return The point which is used as the "anchor" when the size of this window changes.
	  *         Whenever changes occur, the anchor point is preserved and this window is moved around it instead.
	  */
	private def absoluteAnchorPosition: Point = getAnchor(bounds)
	
	
	// IMPLEMENTED    --------------
	
	override def position = _positionPointer.value
	override def position_=(newPosition: Point) = {
		if (newPosition != position)
			AwtEventThread.async { component.setLocation(newPosition.toAwtPoint) }
	}
	
	override def size = _sizePointer.value
	override def size_=(newSize: Size) = {
		if (newSize != size) {
			// Remembers the anchor position for repositioning
			pendingAnchor.setOne(absoluteAnchorPosition)
			val dims = newSize.toDimension
			AwtEventThread.async {
				component.setPreferredSize(dims)
				component.setSize(dims)
			}
		}
	}
	
	override def bounds: Bounds = _boundsPointer.value
	override def bounds_=(b: Bounds): Unit = {
		if (b != bounds)
			AwtEventThread.async { component.setBounds(b.toAwt) }
	}
	
	override def children: Vector[Component] = Vector(content)
	
	override def calculatedStackSize = {
		val availableScreenSize = if (respectScreenInsets) screenSize - screenInsets.total else screenSize
		val normal = (content.stackSize + insets.total).limitedTo(availableScreenSize)
		
		// If on full screen mode, tries to maximize screen size
		if (isFullScreen)
			normal.max match {
				case Some(max) => normal.withOptimal(max)
				case None => normal
			}
		else
			normal
	}
	
	override def fontMetricsWith(font: Font): FontMetricsWrapper = component.getFontMetrics(font.toAwt)
	
	override def updateLayout() = content.size = size - insets.total
	
	// When distributing events, accounts for the difference in coordinate systems (based on insets)
	override def distributeMouseButtonEvent(event: MouseButtonStateEvent) =
		super.distributeMouseButtonEvent(event.translated(-insets.toPoint))
	override def distributeMouseMoveEvent(event: MouseMoveEvent) =
		super.distributeMouseMoveEvent(event.translated(-insets.toPoint))
	override def distributeMouseWheelEvent(event: MouseWheelEvent) =
		super.distributeMouseWheelEvent(event.translated(-insets.toPoint))
	
	
	// OTHER    --------------------
	
	/**
	  * Makes this window visible.
	  * Please note that this has no effect after this window has closed.
	  * @param gainFocus Whether this window should be allowed to gain focus when it becomes visible
	  *                  (default = whether focus is generally allowed in this window)
	  * @return Whether the state of this window was affected
	  */
	def display(gainFocus: Boolean = isFocusable): Boolean = {
		// Case: Closed => No change
		if (hasClosed)
			false
		// Case: Default focus option => Updates visibility
		else if (gainFocus == isFocusable)
			visible = true
		// Case: Already visible => No change
		else if (visible)
			false
		// Case: Focus style altered => Makes visible with altered focus style
		else {
			AwtEventThread.async {
				component.setFocusableWindowState(gainFocus)
				visible = true
				component.setFocusableWindowState(isFocusable)
			}
			true
		}
	}
	
	/**
	  * Makes this window visible and brings it back from the iconified / minimized state, if applicable.
	  * Please note that this function has no effect after this window has closed.
	  * @return Whether the state of this window was affected.
	  */
	def makeFullyVisible() = {
		val r1 = visible = true
		val r2 = minimized = false
		r1 || r2
	}
	
	/**
	  * Closes (disposes) this window
	  */
	def close() = AwtEventThread.async { component.dispose() }
	
	/**
	  * Makes it so that this window will close once the escape key is pressed
	  * @param requireFocus Whether the escape key-press should only be recognized if this window is the
	  *                     focused window at that time
	  */
	def setToCloseOnEsc(requireFocus: Boolean = true) = {
		val handler = if (requireFocus) focusKeyStateHandler else keyStateHandler
		handler += KeyStateListener.onKeyReleased(KeyEvent.VK_ESCAPE) { _ => close() }
	}
	/**
	  * Closes this window once the user releases any keyboard key
	  */
	def setToCloseOnAnyKeyRelease() = {
		keyStateHandler += KeyStateListener.onAnyKeyReleased { _ => close() }
	}
	/**
	  * Closes this window when it loses focus the next time
	  */
	def setToCloseOnFocusLost() = focusedFlag.addListener { e =>
		// Case: Gained focus => Ignores
		if (e.newValue)
			DetachmentChoice.continue
		// Case: Lost focus => Closes this window
		else {
			close()
			DetachmentChoice.detach
		}
	}
	/**
	  * Closes this window when the user clicks on anywhere outside this window
	  * @param activationDelay The delay after which mouse events should be recognized (default = no delay)
	  */
	def setToCloseWhenClickedOutside(activationDelay: Duration = Duration.Zero) = {
		activationDelay.finite.foreach { delay =>
			val threshold = Now + delay
			val listener = MouseButtonStateListener(
				MouseButtonStateEvent.leftButtonFilter && Filter { _: Any => Now > threshold } &&
					MouseEvent.isOutsideAreaFilter(bounds)
			) { _ => close(); None }
			addMouseButtonListener(listener)
		}
	}
	/**
	  * Sets it so that the JVM will exit once this window closes.
	  * @param delay Delay after window closing, before the closing of the JVM
	  * @param exc   Implicit execution context
	  * @param log Implicit logging for encountered errors
	  */
	def setToExitOnClose(delay: FiniteDuration = Duration.Zero)(implicit exc: ExecutionContext, log: Logger) =
		closeFuture.onComplete { res =>
			res.logFailure
			Delay(delay) { System.exit(0) }
		}
	
	/**
	  * Checks this window's bounds against the current stackSize.
	  * For optimal results, the stackSize of this window and the main contents should be up-to-date
	  * (see [[resetCachedSize()]]).
	  *
	  * @param dictateSize Whether this window should dictate the resulting size.
	  *
	  *                    When false, this window will attempt not to modify the size unless required for
	  *                    following the applicable stack size limits.
	  *
	  *                    Full screen windows always dictate their size.
	  *
	  *                    Default = dictate if allowed by this window's resize logic.
	  *
	  * @return Whether the size of this window changes as a result of this function.
	  *         The result might not be immediate (it is performed on the AWT event thread)
	  */
	def optimizeBounds(dictateSize: Boolean = resizeLogic.allowsProgramResize) = {
		val sizeAtStart = size
		// Case: Full screen => Sets optimal size and moves to top-left
		if (isFullScreen) {
			val newBounds = Bounds(if (respectScreenInsets) screenInsets.toPoint else Point.origin, stackSize.optimal)
				.round
			// Prevents the user from making this window too small
			if (resizeLogic.allowsUserResize)
				AwtEventThread.async { component.setMinimumSize(stackSize.min.toDimension) }
			bounds = newBounds
			sizeAtStart != newBounds.size
		}
		// Case: Windowed mode => Either dictates the new size or just makes sure its within limits
		else {
			// Updates the size of this window
			val newSize = {
				// Case: Dictates size => Sets directly to optimal size
				if (dictateSize)
					stackSize.optimal
				// Case: Only optimizes =>
				// Checks whether the current size follows the limits placed by the stack size and modifies if necessary
				else
					size.mergeWith(stackSize) { (current, limit) =>
						// Checks min
						if (current < limit.min)
							limit.min
						else
							limit.max match {
								// Checks max
								case Some(max) =>
									if (current > max)
										max
									else
										current
								case None => current
							}
					}
			}
			// Prevents the user from resizing too much
			if (resizeLogic.allowsUserResize) {
				AwtEventThread.async {
					component.setMinimumSize(stackSize.min.toDimension)
					val maxDimension = stackSize.max match {
						case Some(max) => max.toDimension
						case None => null
					}
					component.setMaximumSize(maxDimension)
				}
			}
			size = newSize
			sizeAtStart != newSize
		}
	}
	
	/**
	  * Centers this window on the screen.
	  * Calling this function has no effect on full-screen windows.
	  */
	def centerOnScreen() = centerOn(null)
	/**
	  * Centers this window over the parent window.
	  * If this window has no parent, centers it on screen.
	  * Calling this function has no effect on full-screen windows.
	  */
	def centerOnParent() = centerOn(component.getParent)
	
	/**
	  * Requests this window to gain focus if it doesn't have it already.
	  * Moves this window to the front in the process.
	  */
	def requestFocus() = {
		if (isNotFocused)
			AwtEventThread.async {
				component.toFront()
				component.repaint()
			}
	}
	
	private def centerOn(component: java.awt.Component) = {
		if (isNotFullScreen)
			AwtEventThread.async { this.component.setLocationRelativeTo(component) }
	}
	
	// Ensures that this window is kept within the screen area
	private def positionWithinScreen(proposed: Point) = {
		// Priority 3: Full screen size
		val screenBounds = Bounds(Point.origin, screenSize)
		// Priority 2: Screen size - screen insets (may be skipped)
		val insetsReducedScreenBounds = if (respectScreenInsets) Some(screenBounds - screenInsets) else None
		// Priority 1: Screen size - screen insets - extra margins
		val remainingArea = insetsReducedScreenBounds.getOrElse(screenBounds) - screenBorderMargins
		
		// Ensures that the bounds of this window fit within one of these priorities
		bounds = Bounds.fromFunction2D { axis =>
			val optimalArea = remainingArea(axis)
			val proposedStart = proposed(axis)
			val proposedArea = NumericSpan(proposedStart, proposedStart + size(axis))
			// Case: The proposed layout doesn't fit priority 1 => Moves to priority 2
			if (proposedArea.length > optimalArea.length) {
				insetsReducedScreenBounds.map { _(axis) }.filter { _.length >= proposedArea.length } match {
					// Case: Proposed fits within the secondary area => Moves it into that area
					case Some(secondaryArea) => proposedArea.shiftedInto(secondaryArea)
					// Case: Didn't fit priority 2 (or skipped) => Fits to screen
					case None =>
						val screenArea = screenBounds(axis)
						// Case: Fits to screen => Moves to screen
						if (screenArea.length > proposedArea.length)
							proposedArea.shiftedInto(screenArea)
						// Case: Wouldn't fit within screen => Spans the entire screen
						else
							screenArea
				}
			}
			// Case: Proposed layout fits => Makes sure it is positioned within the target area
			else
				proposedArea.shiftedInto(optimalArea)
		}
	}
	
	
	// NESTED   -------------------------
	
	private object WindowComponentStateListener extends ComponentAdapter
	{
		override def componentShown(e: ComponentEvent) = _visibleFlag.set()
		override def componentHidden(e: ComponentEvent) = _visibleFlag.reset()
		
		override def componentMoved(e: ComponentEvent) = _positionPointer.value = Point.of(component.getLocation)
		override def componentResized(e: ComponentEvent) = {
			val newSize = Size.of(component.getSize)
			_sizePointer.value = newSize
			
			// Updates content layout (only while visible)
			if (isFullyVisible) {
				updateLayout()
				content.updateLayout()
			}
			// Repositions based on anchoring, if queued
			pendingAnchor.pop().foreach { anchor =>
				val newAnchor = absoluteAnchorPosition
				// Moves this window so that the anchors overlap.
				// Makes sure screen borders are respected, also.
				positionWithinScreen(position - (newAnchor - anchor))
			}
		}
	}
	private object WindowStateListener extends WindowAdapter
	{
		override def windowOpened(e: WindowEvent) = _openedFlag.set()
		
		override def windowIconified(e: WindowEvent) = _minimizedFlag.set()
		override def windowDeiconified(e: WindowEvent) = _minimizedFlag.reset()
		
		override def windowActivated(e: WindowEvent) = _activeFlag.set()
		override def windowDeactivated(e: WindowEvent) = _activeFlag.reset()
		
		override def windowGainedFocus(e: WindowEvent) = _focusedFlag.set()
		override def windowLostFocus(e: WindowEvent) = _focusedFlag.reset()
		
		override def windowClosed(e: WindowEvent) = _closedFlag.set()
		override def windowClosing(e: WindowEvent) = close()
	}
}
