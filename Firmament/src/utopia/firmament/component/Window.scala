package utopia.firmament.component

import utopia.firmament.awt.AwtComponentExtensions._
import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.Window.{locationAdjustment, minIconSize}
import utopia.firmament.component.stack.{CachingStackable, Stackable}
import utopia.firmament.context.ComponentCreationDefaults
import utopia.firmament.context.window.WindowContext
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.firmament.model.enumeration.WindowResizePolicy.Program
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.Delay
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.{EventfulPointer, IndirectPointer, ResettableFlag, SettableFlag}
import utopia.flow.view.template.eventful.Flag
import utopia.flow.view.template.eventful.Flag._
import utopia.genesis.graphics.FontMetricsWrapper
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.keyboard.Key.Esc
import utopia.genesis.handling.event.keyboard.{KeyStateHandler, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.Handlers
import utopia.genesis.image.Image
import utopia.genesis.text.Font
import utopia.genesis.util.Screen
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

import java.awt.Frame
import java.awt.event.{ComponentEvent, ComponentListener, WindowAdapter, WindowEvent}
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
	
	/**
	  * The maximum duration given to the window initialization process to block the calling thread while
	  * waiting for the AWT event thread access or completion.
	  *
	  * This value is used as a default during window creation, but may be overruled by another value
	  * specified during window-creation.
	  *
	  * By default, this value is zero and no warnings will be fired.
	  */
	var maxInitializationWaitDurationDefault: Duration = Duration.Zero
	
	/**
	  * Stores here the location adjustment that must be applied for every call to setLocation and setBounds
	  * in order to correct for Java and/or OS -caused errors.
	  *
	  * This value is set after the first window becomes visible, after which it is used among all created windows.
	  */
	private var locationAdjustment: Option[Vector2D] = None
	
	
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
	  * @param maxInitializationWaitDuration The maximum duration this window is allowed to block the current thread
	  *                                      during the initialization process.
	  *                                      The initialization occurs within the AWT event thread and by default
	  *                                      this thread will wait until it is completed or until this threshold duration
	  *                                      is reached.
	  *                                      Default = the default specified in the Window object,
	  *                                      which by default is 0.1 seconds.
	  *                                      A warning is logged if this threshold is reached.
	  * @param prepareForSizeChange A function called before each window size change.
	  *                             If specified, accepts 2 values:
	  *                                 1. The predicted new size of this window
	  *                                 1. A flag that contains true as long
	  *                                    as the window size update process is still pending
	  *                                    (i.e. while the new size has not been fully applied).
	  *
	  *                            This function is called before the size change actually occurs,
	  *                            giving time to prepare for the upcoming change.
	  *                            Note: the size property of this window might not yet reflect this new value.
	  *
	  *                            None (default) if no preparation is performed.
	  *
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
	  * @param disableAutoBoundsUpdates Whether automatic window bounds updates should be disabled.
	  *                                 This concerns bounds updates that occur at two places:
	  *                                 1) When this window is constructed (once), and
	  *                                 2) Whenever this window becomes visible.
	  *
	  *                                 Set this to false if you intend to perform your own window bounds optimization
	  *                                 upon these two cases.
	  *
	  *                                 Default = false = window automatically updates its bounds
	  * @param exc                 Implicit execution context
	  * @param logger Implicit logging implementation used for handling non-critical (recoverable) errors,
	  *               as well as possible window initialization warnings.
	  * @return A new window
	  */
	def apply(container: java.awt.Container, content: Stackable, eventActorHandler: ActorHandler,
	          parent: Option[java.awt.Window], title: LocalizedString = LocalizedString.empty,
	          resizeLogic: WindowResizePolicy = Program, screenBorderMargins: Insets = Insets.zero,
	          getAnchor: Bounds => Point = _.center, icon: Image = ComponentCreationDefaults.windowIcon,
	          maxInitializationWaitDuration: Duration = Window.maxInitializationWaitDurationDefault,
	          prepareForSizeChange: Option[(Size, Flag) => Unit] = None,
	          borderless: Boolean = false, fullScreen: Boolean = false, disableFocus: Boolean = false,
	          ignoreScreenInsets: Boolean = false, enableTransparency: Boolean = false,
	          disableAutoBoundsUpdates: Boolean = false)
	         (implicit exc: ExecutionContext, logger: Logger) =
	{
		val window = parent match {
			case Some(parent) => Left(new JDialog(parent, title.string))
			case None => Right(new JFrame(title.string))
		}
		new Window(window, container, content, eventActorHandler, resizeLogic, screenBorderMargins, getAnchor, icon,
			maxInitializationWaitDuration, prepareForSizeChange, !borderless, fullScreen, !disableFocus,
			!ignoreScreenInsets, enableTransparency, disableAutoBoundsUpdates)
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
	  * @param prepareForSizeChange A function called before each window size change.
	  *                             If specified, accepts 2 values:
	  *                                 1. The predicted new size of this window
	  *                                 1. A flag that contains true as long
	  *                                    as the window size update process is still pending
	  *                                    (i.e. while the new size has not been fully applied).
	  *
	  *                             This function is called before the size change actually occurs,
	  *                             giving time to prepare for the upcoming change.
	  *                             Note: the size property of this window might not yet reflect this new value.
	  *
	  *                             None (default) if no preparation is performed.
	  *
	  * @param maxInitializationWaitDuration The maximum duration this window is allowed to block the current thread
	  *                                      during the initialization process.
	  *                                      The initialization occurs within the AWT event thread and by default
	  *                                      this thread will wait until it is completed or until this threshold duration
	  *                                      is reached.
	  *                                      Default = the default specified in the Window object,
	  *                                      which by default is 0.1 seconds.
	  *                                      A warning is logged if this threshold is reached.
	  * @param disableAutoBoundsUpdates Whether automatic window bounds updates should be disabled.
	  *                                 This concerns bounds updates that occur at two places:
	  *                                 1) When this window is constructed (once), and
	  *                                 2) Whenever this window becomes visible.
	  *
	  *                                 Set this to false if you intend to perform your own window bounds optimization
	  *                                 upon these two cases.
	  *
	  *                                 Default = false = window automatically updates its bounds
	  * @param logger Implicit logging implementation used for handling non-critical (recoverable) errors,
	  *               as well as possible window initialization warnings.
	  * @return A new window
	  */
	def contextual(container: java.awt.Container, content: Stackable, parent: Option[java.awt.Window] = None,
	               title: LocalizedString = LocalizedString.empty, getAnchor: Bounds => Point = _.center,
	               prepareForSizeChange: Option[(Size, Flag) => Unit] = None,
	               maxInitializationWaitDuration: Duration = Window.maxInitializationWaitDurationDefault,
	               disableAutoBoundsUpdates: Boolean = false)
	              (implicit context: WindowContext, exc: ExecutionContext, logger: Logger) =
		apply(container, content, context.actorHandler, parent, title, context.windowResizeLogic,
			context.screenBorderMargins, getAnchor, context.icon, maxInitializationWaitDuration, prepareForSizeChange,
			!context.windowBordersEnabled, context.fullScreenEnabled, !context.focusEnabled,
			!context.screenInsetsEnabled, context.transparencyEnabled, disableAutoBoundsUpdates)
}

/**
  * Wraps an awt window, providing an interface for it.
  * Please note that this class doesn't handle component revalidation.
  * I.e. when the size of the content needs to be adjusted, this window will not recognize it by default.
  * Please call [[resetCachedSize]], [[optimizeBounds]] and [[updateLayout]] when you wish to "revalidate"
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
  * @param prepareForSizeChange A function called before each window size change.
  *                             If specified, accepts 2 values:
  *                                 1. The predicted new size of this window
  *                                 1. A flag that contains true as long
  *                                    as the window size update process is still pending
  *                                    (i.e. while the new size has not been fully applied).
  *
  *                             This function is called before the size change actually occurs,
  *                             giving time to prepare for the upcoming change.
  *                             Note: the size property of this window might not yet reflect this new value.
  *
  *                             None (default) if no preparation is performed.
  *
  * @param maxInitializationWaitDuration The maximum duration this window is allowed to block the current thread
  *                                      during the initialization process.
  *                                      The initialization occurs within the AWT event thread and by default
  *                                      this thread will wait until it is completed or until this threshold duration
  *                                      is reached.
  *                                      Default = the default specified in the Window object,
  *                                                which by default is 0.1 seconds.
  *                                      A warning is logged if this threshold is reached.
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
  * @param disableAutoBoundsUpdates Whether automatic window bounds updates should be disabled.
  *                                 This concerns bounds updates that occur at two places:
  *                                     1) When this window is constructed (once), and
  *                                     2) Whenever this window becomes visible.
  *
  *                                 Set this to false if you intend to perform your own window bounds optimization
  *                                 upon these two cases.
  *
  *                                 Default = false = window automatically updates its bounds
  * @param exc Implicit execution context
  * @param logger Implicit logging implementation used for handling non-critical (recoverable) errors,
  *               as well as possible window initialization warnings.
  */
class Window(protected val wrapped: Either[JDialog, JFrame], container: java.awt.Container, content: Stackable,
             eventActorHandler: ActorHandler, resizeLogic: WindowResizePolicy = Program,
             screenBorderMargins: Insets = Insets.zero, getAnchor: Bounds => Point = _.center,
             initialIcon: Image = ComponentCreationDefaults.windowIcon,
             maxInitializationWaitDuration: Duration = Window.maxInitializationWaitDurationDefault,
             prepareForSizeChange: Option[(Size, Flag) => Unit] = None,
             val hasBorders: Boolean = true, isFullScreen: Boolean = false, val isFocusable: Boolean = true,
             respectScreenInsets: Boolean = true, enableTransparency: Boolean = false,
             disableAutoBoundsUpdates: Boolean = false)
            (implicit exc: ExecutionContext, logger: Logger)
	extends CachingStackable
{
	// ATTRIBUTES   ----------------
	
	override val mouseButtonHandler = MouseButtonStateHandler()
	override val mouseMoveHandler = MouseMoveHandler()
	override val mouseWheelHandler = MouseWheelHandler()
	
	override lazy val handlers: Handlers = Handlers(mouseButtonHandler, mouseMoveHandler, mouseWheelHandler)
	
	/**
	  * @return The AWT window wrapped by this window
	  */
	val component = wrapped.either
	
	// Caches screen size
	private lazy val screenSize = Screen.actualSize
	private lazy val screenInsets = Screen.actualInsetsAt(component.getGraphicsConfiguration)
	
	// Allows mutable access to the display icon
	val iconPointer = EventfulPointer(initialIcon)
	
	// Stores window state in private flags
	// These are updated based on awt window events, but also accept preliminary updates from other sources
	private val _openedFlag = SettableFlag()
	private val _closedFlag = SettableFlag()
	private val _visibleFlag = ResettableFlag()
	private val _minimizedFlag = ResettableFlag()
	private val _activeFlag = ResettableFlag()
	private val _focusedFlag = ResettableFlag(component.isFocused)
	
	// Stores position and size in pointers, which are only updated on window events
	private val _positionPointer = EventfulPointer(Point.origin)
	private lazy val roundPositionPointer = _positionPointer.map { _.round }
	// Pre-initializes the window size based on container size.
	// Won't take into account the window insets. Actual size is initialized after pack() in the AWT event thread
	private val _sizePointer = EventfulPointer(Size(container.getSize))
	private lazy val roundSizePointer = _sizePointer.map { _.round }
	
	// Stores calculated anchor, which is used in repositioning after size changes
	// This pointer is cleared after the anchor has been resolved / actuated
	private val pendingAnchor = Volatile.optional[Point]()
	
	// Tracks situations where position and/or size are yet to update because the updates are performed within
	// the AWT event thread
	private val pendingPositionUpdatesPointer = Volatile.eventful.emptySeq[Point]
	private val pendingSizeUpdatesPointer = Volatile.eventful.emptySeq[Size]
	/**
	  * A flag that is set during window position updates
	  */
	val positionUpdatingFlag: Flag = pendingPositionUpdatesPointer.map { _.nonEmpty }
	/**
	  * A flag that is set during window size updates
	  */
	val sizeUpdatingFlag: Flag = pendingSizeUpdatesPointer.map { _.nonEmpty }
	/**
	  * A flag that is set during window bounds updates
	  */
	val boundsUpdatingFlag = positionUpdatingFlag || sizeUpdatingFlag
	
	/**
	  * A flag that contains true whenever this window is fully visible
	  * (i.e. open, visible and not minimized)
	  */
	val fullyVisibleFlag = (_visibleFlag && (!_minimizedFlag)) && (!_closedFlag)
	/**
	  * A flag that contains true when (and only when) this window is fully visible (open, not minimized) and has focus
	  */
	lazy val fullyVisibleAndFocusedFlag = fullyVisibleFlag && _focusedFlag
	
	/**
	  * A future that resolves once this window is displayed for the first time
	  */
	val openedFuture = _openedFlag.future
	
	/**
	  * A flag that contains true while this window is open.
	  * From the creation of this window until the first call of visible = true, this flag contains false.
	  * From first visible = true to the closing of this window, this flag contains true.
	  * After the closing of this window, this flag contains false.
	  */
	lazy val openFlag = _openedFlag && (!_closedFlag)
	
	// Merges the position and size in order to form bounds
	// Provides a separate interface for users
	private lazy val _boundsPointer = _positionPointer.mergeWith(_sizePointer) { Bounds(_, _) }
	
	// Provides a custom set() function for some of the flags
	/**
	  * A flag that is set when this window becomes visible for the first time.
	  * Setting this flag will display this window (unless already set).
	  */
	lazy val openedFlag = SettableFlag.wrap(_openedFlag) {
		if (_openedFlag.isSet)
			false
		else
			visible = true
	}
	/**
	  * A flag that is set once this window closes.
	  * Setting this flag will close this window (unless closed already)
	  */
	lazy val closedFlag = SettableFlag.wrap(_closedFlag) {
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
			KeyboardEvents += handler
		handler
	}
	/**
	  * A handler that distributes keyboard events, but only while this window is the focused window.
	  */
	lazy val focusKeyStateHandler = {
		val parent = keyStateHandlerPointer.value
		val handler = KeyStateHandler.conditional(focusedFlag).empty
		parent += handler
		handler
	}
	
	override lazy val children: Seq[Component] = Single(content)
	
	
	// INITIAL CODE ----------------
	
	// These initial actions are performed in the AWT event thread
	// This thread waits for the completion of these actions for some time, but not indefinitely
	AwtEventThread.future {
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
		component.pack()
		
		// Sets transparent background if content doesn't have a background itself
		// (only works in certain conditions. Doesn't work if this window is decorated)
		if (enableTransparency && isBorderless && container.isBackgroundSet && container.getBackground.getAlpha < 255)
			Try { component.setBackground(Color.black.withAlpha(0.0).toAwt) }
		
		// Initializes position and size
		_positionPointer.value = Point.of(component.getLocation)
		_sizePointer.value = Size(component.getSize)
		
		// Registers to update the state when the wrapped window updates
		component.addComponentListener(WindowComponentStateListener)
		
		// Updates the window icon when appropriate
		iconPointer.addListenerAndSimulateEvent(Image.empty) { e =>
			// Is not interested in icon changes after this window has closed
			if (hasClosed)
				Detach
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
				Continue
			}
		}
		
		// Whenever this window becomes visible, updates content layout
		// (layout updates are skipped while this window is not visible)
		fullyVisibleFlag.addListener { e =>
			if (hasClosed)
				Detach
			else {
				if (e.newValue) {
					AwtEventThread.async {
						content.resetCachedSize()
						resetCachedSize()
						// Automatic bounds-updates may be disabled
						if (disableAutoBoundsUpdates || !optimizeBounds()) {
							updateLayout()
							content.updateLayout()
							component.repaint()
						}
						// Marks this window as opened when becomes visible the first time
						if (_openedFlag.isNotSet)
							boundsUpdatingFlag.onceNotSet { _openedFlag.set() }
					}
				}
				Continue
			}
		}
		
		// Once this window is open, starts event handling
		openedFuture.foreach { _ =>
			// Starts mouse listening (which is active only while visible)
			val mouseEventGenerator = new MouseEventGenerator(container)
			eventActorHandler += mouseEventGenerator
			// Mouse movement events are only enabled while this window is in focus (unless not focusable)
			val movementsEnabledPointer = if (isFocusable) fullyVisibleAndFocusedFlag else fullyVisibleFlag
			mouseEventGenerator.buttonHandler += MouseButtonStateListener.conditional(fullyVisibleFlag) { e =>
				content.distributeMouseButtonEvent(e)
			}
			mouseEventGenerator.moveHandler += MouseMoveListener
				.conditional(movementsEnabledPointer)(content.distributeMouseMoveEvent)
			mouseEventGenerator.wheelHandler += MouseWheelListener
				.conditional(fullyVisibleFlag)(content.distributeMouseWheelEvent)
			CommonMouseEvents.addGenerator(mouseEventGenerator)
			
			// Starts key listening (if used)
			KeyboardEvents ++= keyStateHandlerPointer.current
			
			// Schedules to set up the location adjustment -value, unless set already
			if (locationAdjustment.isEmpty)
				Delay(0.5.seconds) {
					val testPosition = position + X(1)
					position = testPosition
					positionUpdatingFlag.onceNotSet {
						locationAdjustment = Some((testPosition - position).toVector2D)
					}
				}
			
			// Quits event listening once this window closes
			closeFuture.onComplete { _ =>
				CommonMouseEvents.removeGenerator(mouseEventGenerator)
				eventActorHandler -= mouseEventGenerator
				mouseEventGenerator.stop()
				KeyboardEvents --= keyStateHandlerPointer.current
			}
		}
	}.waitFor(maxInitializationWaitDuration).failure
		// Logs a warning if the window-initialization takes longer than the allowed block time
		.foreach { e =>
			if (maxInitializationWaitDuration > Duration.Zero)
				logger(e,
					s"Warning: Window initialization process took more than ${
						maxInitializationWaitDuration.description}. The process will be completed in another thread.")
		}
	
	// May set up preparations for window size changes
	prepareForSizeChange.foreach { prepare =>
		pendingSizeUpdatesPointer.addListenerWhile(fullyVisibleFlag) { change =>
			if (change.oldValue.isEmpty)
				change.newValue.lastOption.foreach { prepare(_, sizeUpdatingFlag) }
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
		val roundedNewPosition = newPosition.round
		// Case: Assigning equal or an insignificantly different value => Only updates the precise position pointer
		if (roundedNewPosition == roundPositionPointer.value)
			_positionPointer.value = newPosition
		// Case: Assigning a different value => Affects window position
		else
			_setPosition(newPosition, roundedNewPosition)
	}
	
	override def size = _sizePointer.value
	override def size_=(newSize: Size) = {
		val roundedNewSize = newSize.round
		if (roundedNewSize == roundSizePointer.value)
			_sizePointer.value = newSize
		else
			_setSize(newSize, roundedNewSize)
	}
	
	override def bounds: Bounds = _boundsPointer.value
	override def bounds_=(b: Bounds): Unit = {
		// Applies the bounds as rounded
		val roundBounds = b.round
		// Case: Size is preserved
		if (roundBounds.size == roundSizePointer.value) {
			_sizePointer.value = b.size
			// Case: Position is also preserved => No visual change
			if (roundBounds.position == roundPositionPointer.value)
				_positionPointer.value = b.position
			// Case: Moving this window
			else
				_setPosition(b.position, roundBounds.position)
		}
		// Case: Size changes while position is preserved
		else if (roundBounds.position == roundPositionPointer.value) {
			_positionPointer.value = b.position
			_setSize(b.size, roundBounds.size)
		}
		// Case: Both size and position change
		else {
			startPositionUpdate(roundBounds.position)
			startSizeUpdate(roundBounds.size)
			pendingAnchor.clear()
			_sizePointer.value = b.size
			_positionPointer.value = b.position
			AwtEventThread.async {
				component.setPreferredSize(roundBounds.size.toDimension)
				component.setBounds((roundBounds + locationAdjustment.getOrElse(Vector2D.zero)).toAwt)
			}
			Delay(0.2.seconds) {
				finishPositionUpdate(roundBounds.position)
				finishSizeUpdate(roundBounds.size)
			}
		}
	}
	
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
	  * @param centerOnParent Whether this window should be positioned so that it lies at the center of
	  *                       its parent component (center of screen if there is no parent window).
	  *                       Default = false.
	  * @return Whether the state of this window was affected
	  */
	def display(gainFocus: Boolean = isFocusable, centerOnParent: Boolean = false): Boolean = {
		// Case: Closed => No change
		if (hasClosed)
			false
		else {
			// Centers on parent once visible, if requested
			if (centerOnParent) {
				if (visible)
					this.centerOnParent()
				else if (hasOpened)
					visiblePointer.onNextChange { _ => boundsUpdatingFlag.onceNotSet { this.centerOnParent() } }
				else
					openedFuture.foreach { _ => this.centerOnParent() }
			}
			
			// Case: Default focus option => Updates visibility
			if (gainFocus == isFocusable)
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
	}
	/**
	  * Displays this window, making it visible
	  * @param gainFocus Whether this window should be allowed to gain focus when it becomes visible
	  *                  (default = whether focus is generally allowed in this window)
	  * @param centerOnParent Whether this window should be positioned so that it lies at the center of
	  *                       its parent component (center of screen if there is no parent window).
	  *                       Default = false.
	  * @param f A function to run after this window has been made visible
	  * @tparam U Arbitrary function result type
	  * @return Whether the visibility state of this window was affected
	  */
	def displayAndThen[U](gainFocus: Boolean = isFocusable, centerOnParent: Boolean = false)(f: => U) = {
		val wasOpen = hasOpened
		// Displays this window
		val result = display(gainFocus, centerOnParent)
		// Triggers the after-effect once visible with up-to-date bounds
		if (wasOpen)
			_visibleFlag.onceSet { boundsUpdatingFlag.onceNotSet(f) }
		else
			openedFuture.foreach { _ => f }
		result
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
		handler += KeyStateListener.released(Esc) { _ => close() }
	}
	/**
	  * Closes this window once the user releases any keyboard key
	  */
	def setToCloseOnAnyKeyRelease() = keyStateHandler += KeyStateListener.released { _ => close() }
	/**
	  * Closes this window when it loses focus the next time
	  */
	def setToCloseOnFocusLost() = focusedFlag.addListener { e =>
		// Case: Gained focus => Ignores
		if (e.newValue)
			Continue
		// Case: Lost focus => Closes this window
		else {
			close()
			Detach
		}
	}
	/**
	  * Closes this window when the user clicks on anywhere outside this window
	  * @param activationDelay The delay after which mouse events should be recognized (default = no delay)
	  */
	def setToCloseWhenClickedOutside(activationDelay: Duration = Duration.Zero) = {
		activationDelay.finite.foreach { delay =>
			val threshold = Now + delay
			addMouseButtonListener(MouseButtonStateListener
				.leftPressed.filtering { _ => Now > threshold }.outside(bounds) { _ => close() })
		}
	}
	/**
	  * Makes it so that this window closes automatically after the specified time period
	  * @param delay Delay before closing this window
	  */
	def setToCloseAfter(delay: Duration) = delay.finite.foreach { Delay(_) { close() } }
	/**
	  * Sets it so that the JVM will exit once this window closes.
	  * @param delay Delay after window closing, before the closing of the JVM
	  */
	def setToExitOnClose(delay: FiniteDuration = Duration.Zero) =
		closeFuture.onComplete { res =>
			res.log
			Delay(delay) { System.exit(0) }
		}
	
	/**
	  * Checks this window's bounds against the current stackSize.
	  * For optimal results, the stackSize of this window and the main contents should be up-to-date
	  * (see [[resetCachedSize]]).
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
		val sizeAtStart = roundSizePointer.value
		// Case: Full screen => Sets optimal size and moves to top-left
		if (isFullScreen) {
			val newBounds = Bounds(if (respectScreenInsets) screenInsets.toPoint else Point.origin, stackSize.optimal)
				.round
			val newPosition = newBounds.position
			val newSize = newBounds.size
			
			// Case: No size or position change
			if (newSize == roundSizePointer.value && newPosition == roundPositionPointer.value)
				false
			// Case: Bounds change
			else {
				// Case: Minimum size feature utilized => Checks whether window minimum size needs an update
				if (resizeLogic.allowsUserResize) {
					val newMinSize = stackSize.min.toDimension
					// Case: No update required => Applies a standard bounds update
					if (newMinSize.equals(component.getMinimumSize))
						bounds = newBounds
					// Case: Minimum size updates => Makes sure this window is in "bounds updating" -mode while doing so
					else {
						// Enters "updating bounds" -mode
						startSizeUpdate(newSize)
						
						// Prevents the user from making this window too small
						AwtEventThread.async { component.setMinimumSize(newMinSize) }
						bounds = newBounds
						
						// Makes sure the "updating bounds" -mode finishes
						AwtEventThread.later { finishSizeUpdate(newSize) }
					}
				}
				// Case: Minimum size feature not applicable => Applies a standard bounds update
				else
					bounds = newBounds
					
				sizeAtStart != newBounds.size
			}
		}
		// Case: Windowed mode => Either dictates the new size or just makes sure it's within limits
		else {
			// Updates the size of this window
			val newSize = {
				// Case: Dictates size => Sets directly to optimal size
				if (dictateSize)
					stackSize.optimal
				// Case: Only optimizes =>
				// Checks whether the current size follows the limits placed by the stack size and modifies if necessary
				else
					size
						.mergeWith(stackSize) { (current, limit) =>
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
			}.round
			
			// Case: No size change
			if (newSize == roundSizePointer.value)
				false
			// Case: Size changes
			else {
				// Case: Possibly needs to modify minimum & maximum sizes
				if (resizeLogic.allowsUserResize) {
					val newMinSize = stackSize.min.toDimension
					val newMaxDimension = stackSize.max match {
						case Some(max) => max.toDimension
						case None => null
					}
					lazy val maxSizeIsEqual = {
						if (newMaxDimension == null)
							component.getMaximumSize == null
						else
							newMaxDimension.equals(component.getMaximumSize)
					}
					// Case: Minimum & maximum sizes won't change => Applies a standard size change
					if (newMinSize.equals(component.getMinimumSize) && maxSizeIsEqual)
						size = newSize
					// Case: Minimum and/or maximum size changes => Applies a custom size change
					else
						_setSize(newSize, newSize, Some(Pair(newMinSize, newMaxDimension)))
				}
				// Case: Min & max size not applicable => Applies a standard size change
				else
					size = newSize
					
				sizeAtStart != newSize
			}
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
	
	private def _setPosition(newPosition: Point, roundedNewPosition: Point) = {
		startPositionUpdate(roundedNewPosition)
		pendingAnchor.clear()
		_positionPointer.value = newPosition
		AwtEventThread.async {
			component.setLocation((roundedNewPosition + locationAdjustment.getOrElse(Vector2D.zero)).toAwtPoint)
		}
		queuePositionUpdateFinish(roundedNewPosition)
	}
	private def _setSize(newSize: Size, roundedNewSize: Size, minAndMaxSize: Option[Pair[java.awt.Dimension]] = None) =
	{
		// Remembers the anchor position for repositioning
		if (isFullyVisible) {
			val anchor = absoluteAnchorPosition
			pendingAnchor.setOne(anchor)
		}
		startSizeUpdate(roundedNewSize)
		
		_sizePointer.value = newSize
		
		val dims = roundedNewSize.toDimension
		AwtEventThread.async {
			minAndMaxSize.foreach { minAndMax =>
				component.setMinimumSize(minAndMax.first)
				component.setMaximumSize(minAndMax.second)
			}
			component.setPreferredSize(dims)
			component.setSize(dims)
		}
		queueSizeUpdateFinish(roundedNewSize)
	}
	
	// WET WET
	private def startPositionUpdate(newPosition: Point) = {
		pendingPositionUpdatesPointer.update { queued =>
			if (queued.lastOption.contains(newPosition)) queued else queued :+ newPosition
		}
	}
	private def finishPositionUpdate(newPosition: Point) = {
		pendingPositionUpdatesPointer.update { updates =>
			updates.findIndexOf(newPosition) match {
				case Some(updateIndex) => updates.drop(updateIndex + 1)
				case None => updates
			}
		}
	}
	
	private def queuePositionUpdateFinish(newPosition: Point) =
		Delay(0.2.seconds) { finishPositionUpdate(newPosition) }
	
	private def startSizeUpdate(newSize: Size) =
		pendingSizeUpdatesPointer.update { queued =>
			if (queued.lastOption.contains(newSize)) queued else queued :+ newSize
		}
	private def finishSizeUpdate(newSize: Size) = pendingSizeUpdatesPointer.update { updates =>
		updates.findIndexOf(newSize) match {
			case Some(updateIndex) => updates.drop(updateIndex + 1)
			case None => updates
		}
	}
	private def queueSizeUpdateFinish(newSize: Size) = Delay(0.2.seconds) { finishSizeUpdate(newSize) }
	
	private def centerOn(component: java.awt.Component) = {
		if (isNotFullScreen) {
			val estimatedNewPosition = {
				if (component == null)
					(Screen.size / 2 - size / 2).toPoint.floor
				else
					(Bounds.fromAwt(component.getBounds).center - size / 2).floor
			}
			startPositionUpdate(estimatedNewPosition)
			pendingAnchor.clear()
			AwtEventThread.async { this.component.setLocationRelativeTo(component) }
			queuePositionUpdateFinish(estimatedNewPosition)
		}
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
	
	private object WindowComponentStateListener extends ComponentListener
	{
		override def componentShown(e: ComponentEvent) = _visibleFlag.set()
		override def componentHidden(e: ComponentEvent) = _visibleFlag.reset()
		
		override def componentMoved(e: ComponentEvent) = {
			// Updates the position pointer
			val newPosition = Point.of(component.getLocation)
			_positionPointer.value = newPosition
			
			// Clears the pending update that (may have) caused this change
			finishPositionUpdate(newPosition)
		}
		override def componentResized(e: ComponentEvent) = {
			// Updates the size pointer
			val newSize = Size(component.getSize)
			_sizePointer.value = newSize
			
			// Updates content layout (only while visible)
			if (isFullyVisible) {
				updateLayout()
				content.updateLayout()
			}
			
			// Clears the pending update that (may have) caused this change
			finishSizeUpdate(newSize)
			
			// Repositions based on anchoring, if queued
			pendingAnchor.pop().foreach { anchor =>
				val newAnchor = absoluteAnchorPosition
				// Moves this window so that the anchors overlap.
				// Makes sure screen borders are respected, also.
				val adjustment = newAnchor - anchor
				if (adjustment.nonZero)
					positionWithinScreen(position - adjustment)
			}
		}
	}
	private object WindowStateListener extends WindowAdapter
	{
		override def windowOpened(e: WindowEvent) = _visibleFlag.set()
		
		override def windowIconified(e: WindowEvent) = _minimizedFlag.set()
		override def windowDeiconified(e: WindowEvent) = _minimizedFlag.reset()
		
		// Assumes that activation always comes with being focused, also.
		// This, because the root windows don't seem to gain focus events at all
		override def windowActivated(e: WindowEvent) = {
			_activeFlag.set()
			_focusedFlag.set()
		}
		override def windowDeactivated(e: WindowEvent) = {
			_activeFlag.reset()
			_focusedFlag.reset()
		}
		
		override def windowGainedFocus(e: WindowEvent) = _focusedFlag.set()
		override def windowLostFocus(e: WindowEvent) = _focusedFlag.reset()
		
		override def windowClosed(e: WindowEvent) = _closedFlag.set()
		override def windowClosing(e: WindowEvent) = close()
	}
}
