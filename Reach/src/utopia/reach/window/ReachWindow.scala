package utopia.reach.window

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.Window
import utopia.firmament.component.stack.Stackable
import utopia.firmament.context.{TextContext, WindowContext}
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.LengthExtensions._
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.async.process.WaitTarget.{Until, UntilNotified, WaitDuration}
import utopia.flow.async.process.{DelayedProcess, PostponingProcess, Process, WaitTarget}
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.DetachmentChoice
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.view.mutable.eventful.{PointerWithEvents, ResettableFlag, SettableOnce}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.Screen
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.component.factory.{FromContextComponentFactoryFactory, ReachContentWindowContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.{ComponentCreationResult, WindowCreationResult}
import utopia.reach.container.RevalidationStyle.{Delayed, Immediate}
import utopia.reach.container.{ReachCanvas, RevalidationStyle}
import utopia.reach.context.{ReachContentWindowContext, ReachWindowContext, ReachWindowContextWrapper}

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

/**
  * Used for constructing windows that wrap a ReachCanvas element
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
object ReachWindow
{
	// IMPLICIT ---------------------------
	
	implicit def autoFactory(f: ReachWindow.type)
	                        (implicit c: ReachContentWindowContext, exc: ExecutionContext, log: Logger): ReachContentWindowFactory =
		f.contentContextual
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @param context Implicit window creation context
	  * @param exc Implicit execution context
	  * @param log Implicit logging execution
	  * @return A new Reach window factory that uses the specified context
	  */
	def contextual(implicit context: ReachWindowContext, exc: ExecutionContext, log: Logger) =
		withContext(context)
	/**
	  * @param context Implicit popup window creation context
	  * @param exc     Implicit execution context
	  * @param log     Implicit logging execution
	  * @return A new Reach window factory that uses the specified context
	  */
	def contentContextual(implicit context: ReachContentWindowContext, exc: ExecutionContext, log: Logger): ReachContentWindowFactory =
		contextual.withContentContext(context.textContext)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param context Window creation context
	  * @param exc     Implicit execution context
	  * @param log     Implicit logging execution
	  * @return A new Reach window factory that uses the specified context
	  */
	def withContext(context: ReachWindowContext)(implicit exc: ExecutionContext, log: Logger) =
		ContextualReachWindowFactory(context)
	/**
	 * @param context Window creation context
	 * @param exc     Implicit execution context
	 * @param log     Implicit logging execution
	 * @return A new Reach window factory that uses the specified context
	 */
	def withContext(context: ReachContentWindowContext)(implicit exc: ExecutionContext, log: Logger): ReachContentWindowFactory =
		ContextualReachWindowFactory(context).withContentContext(context.textContext)
	/**
	  * @param actorHandler Actor handler to use
	  * @param background Window background color
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @return A new reach window factory that uses the default context
	  */
	def apply(actorHandler: ActorHandler, background: Color)(implicit exc: ExecutionContext, log: Logger) =
		withContext(ReachWindowContext(WindowContext(actorHandler), background))
}

case class ContextualReachWindowFactory(context: ReachWindowContext)(implicit exc: ExecutionContext, log: Logger)
	extends ReachWindowContextWrapper[ContextualReachWindowFactory, ReachContentWindowFactory]
{
	// ATTRIBUTES   ---------------
	
	private implicit val c: ReachWindowContext = context
	
	
	// IMPLEMENTED  ---------------
	
	override def self: ContextualReachWindowFactory = this
	
	override def reachWindowContext: ReachWindowContext = context
	
	override def withReachWindowContext(base: ReachWindowContext): ContextualReachWindowFactory = copy(base)
	
	override def withContentContext(textContext: TextContext) =
		ReachContentWindowFactory(this, context.withContentContext(textContext))
	
	
	// OTHER    ------------------
	
	def withContext(context: ReachWindowContext) = copy(context)
	
	def mapContext(f: ReachWindowContext => ReachWindowContext) = withContext(f(context))
	
	/**
	  * Creates a new reach window instance
	  * @param parent        Parent window, if applicable.
	  *                      Setting this to None will cause a Frame to be created, otherwise a Dialog will be created.
	  * @param title         Title shown on the OS header of this window, if applicable (default = empty)
	  * @param disableAutoBoundsUpdates Whether automatic window bounds updates should be disabled.
	  *                                 This concerns bounds updates that occur at two places:
	  *                                 1) When this window is constructed (once), and
	  *                                 2) Whenever this window becomes visible.
	  *
	  *                                 Set this to false if you intend to perform your own window bounds optimization
	  *                                 upon these two cases.
	  *
	  *                                 Default = false = window automatically updates its bounds
	  * @param createContent A function for creating canvas contents.
	  *                      Accepts the canvas component hierarchy.
	  *                      May return a custom creation result, in addition to the component to wrap.
	  * @tparam C Type of wrapped component
	  * @tparam R Type of additional component creation function result
	  * @return The created window + created canvas + created component + additional function result
	  */
	def apply[C <: ReachComponentLike, R](parent: Option[java.awt.Window] = None,
	                                      title: LocalizedString = LocalizedString.empty,
	                                      disableAutoBoundsUpdates: Boolean = false)
	                                     (createContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		// Prepares pointers for the window and canvas
		val windowPointer = SettableOnce[Window]()
		val canvasPointer = SettableOnce[Stackable]()
		// The attachment status tracking starts once the window has been created
		val attachmentPointer = windowPointer.flatMap {
			case Some(window) => window.fullyVisibleFlag
			case None => AlwaysFalse
		}
		lazy val absoluteWindowPositionPointer = windowPointer.flatMap {
			case Some(window) => window.positionPointer.map { _ + window.insets.toPoint }
			case None => Fixed(Point.origin)
		}
		
		// Creates the revalidation implementation lazily.
		// Assumes that the window and the canvas have been initialized when revalidate() is first called (throws if not)
		lazy val lazyWindow = windowPointer.get
		lazy val lazyCanvas = canvasPointer.get
		lazy val revalidation = revalidate(lazyWindow, lazyCanvas, context.revalidationStyle)
		
		// Creates the canvas
		val canvas = ReachCanvas(attachmentPointer, Right(absoluteWindowPositionPointer),
			Fixed(context.windowBackground), context.cursors,
			disableFocus = !context.focusEnabled) { _ => revalidation() }(createContent)
		canvasPointer.set(canvas)
		
		// Creates the window
		val window = Window.contextual(canvas.parent.component, canvas.parent, parent, title,
			context.getAnchor(canvas, _), disableAutoBoundsUpdates)
		windowPointer.set(window)
		
		// Returns the canvas and the window
		WindowCreationResult(window, canvas)
	}
	
	/**
	  * Creates a new window, which is anchored so that it stays close to the owner component
	  * @param component          A component that "owns" this window
	  * @param preferredAlignment Alignment used when positioning this window relative to the owner component.
	  *                           E.g. If Center is used, will position this window over the center of that component.
	  *                           Or if Right is used, will position this window right of the owner component.
	  *
	  *                           Please note that this alignment may be reversed in case there is not enough space
	  *                           on that side.
	  *
	  *                           Bi-directional alignments, such as TopLeft will place the window next to the component
	  *                           diagonally (so that they won't share any edge together).
	  * @param margin             Margin placed between the owner component and the window, when possible
	  *                           (ignored if preferredAlignment=Center).
	  *                           Default = 0
	  * @param title              Title displayed on this window (provided that OS headers are in use).
	  *                           Default = empty = no title.
	  * @param matchEdgeLength    Whether the window should share an edge length with the anchor component.
	  *                           E.g. If bottom alignment is used and 'matchEdgeLength' is enabled, the resulting
	  *                           window will attempt to stretch so that to matches the width of the 'component'.
	  *                           The stacksize limits of the window will be respected, however, and may limit the
	  *                           resizing.
	  *                           Default = false = will not resize the window.
	  * @param keepAnchored       Whether this window should be kept close to the owner component when its size changes
	  *                           or the owner component is moved or resized.
	  *                           Set to false if you don't expect the owner component to move.
	  *                           This will save some resources, as a large number of components needs to be tracked.
	  *                           Default = true.
	  * @param createContent      A function that accepts a component hierarchy and creates the canvas content.
	  *                           May return an additional result, that will be included in the result of this function.
	  * @tparam C Type of created canvas content
	  * @tparam R Type of additional function result
	  * @return A new window + created canvas + created canvas content + additional creation result
	  */
	def anchoredTo[C <: ReachComponentLike, R](component: ReachComponentLike, preferredAlignment: Alignment,
	                                           margin: Double = 0.0, title: LocalizedString = LocalizedString.empty,
	                                           matchEdgeLength: Boolean = false, keepAnchored: Boolean = true)
	                                          (createContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
	{
		// Full-screen is not supported for anchored windows
		val factory = windowed.withAnchorAlignment(preferredAlignment)
		// Creates the window and the canvas
		val windowCreation = factory(component.parentHierarchy.top.parentWindow, title,
			disableAutoBoundsUpdates = true)(createContent)
		val window = windowCreation.window
		
		// Determines the optimal position for the window
		lazy val screenArea = {
			val base = Bounds(Point.origin, Screen.actualSize)
			if (context.screenInsetsEnabled)
				base - Screen.actualInsetsAt(component.parentHierarchy.top.component.getGraphicsConfiguration)
			else
				base
		}
		
		// A flag used to avoid looping size-altering & listening - Only needed when window streching is used
		val ignoreNextSizeUpdateFlag = ResettableFlag()
		def optimizeWindowPosition() = {
			if (window.isFullyVisible) {
				// Case: Stretching enabled => May modify window size as well
				if (matchEdgeLength) {
					val newBounds = preferredAlignment.stretchNextToWithin(window.stackSize, component.absoluteBounds,
						screenArea, margin, swapToFit = true)
					// Ignores the next size update when altering window size through this method
					ignoreNextSizeUpdateFlag.value = window.size != newBounds.size
					window.bounds = newBounds
				}
				// Case: Only positioning is enabled
				else
					window.position = preferredAlignment.positionRelativeToWithin(window.size, component.absoluteBounds,
						screenArea, margin, swapToFit = true).position
			}
		}
		
		// Whenever the window becomes visible, updates its location and size
		window.fullyVisibleFlag.addListener { e =>
			// Case: Window became visible
			if (e.newValue) {
				// Case: Optimizing both size and position
				if (matchEdgeLength)
					optimizeWindowPosition()
				// Case: Optimizing size only => Uses window.optimizeBounds() to update size
				else {
					// Case: .optimizeBounds() alters size (delayed) => Updates position once the update takes place
					if (window.optimizeBounds())
						window.sizePointer.onNextChange { _ => optimizeWindowPosition() }
					// Case: No size altered => Optimizes position immediately
					else
						optimizeWindowPosition()
				}
			}
			DetachmentChoice.continueUntil(window.hasClosed)
		}
		
		// Updates the window position whenever the owner component's absolute position changes,
		// or when the window's size changes
		// (May be disabled)
		if (keepAnchored) {
			val repositionListener = ChangeListener.onAnyChange {
				optimizeWindowPosition()
				// Stops reacting to events once the window has closed
				DetachmentChoice.continueUntil(window.hasClosed)
			}
			// Repositions on window size changes
			// Case: Window optimization may alter size => Ignores recursive/looping calls
			if (matchEdgeLength)
				window.sizePointer.addListener { _ =>
					if (!ignoreNextSizeUpdateFlag.reset())
						optimizeWindowPosition()
					DetachmentChoice.continueUntil(window.hasClosed)
				}
			// Case: Window optimization never alters size => Repositions on every size change
			else
				window.sizePointer.addListener(repositionListener)
			component.boundsPointer.addListener(repositionListener)
			component.parentHierarchy.parentsIterator.foreach { _.positionPointer.addListener(repositionListener) }
			// The canvas absolute position tracking may not be working
			component.parentCanvas.absolutePositionView.toOption.foreach { _.addListener(repositionListener) }
		}
		
		windowCreation
	}
	
	// Creates an appropriate revalidation implementation
	private def revalidate(window: => Window, canvas: => Stackable, style: RevalidationStyle)
	                      (implicit exc: ExecutionContext, log: Logger): () => Unit =
	{
		def immediateAsync = {
			lazy val process = Process() { _ => revalidate(window, canvas) }
			() => process.runAsync(loopIfRunning = true)
		}
		
		style match {
			// Case: Revalidate immediately
			case Immediate(blocks) =>
				// Case: Blocking
				if (blocks)
					() => revalidate(window, canvas)
				// Case: Async
				else
					immediateAsync
			case Delayed(delay) =>
				// Case: Immediately async (indirect)
				if (delay.start <= Duration.Zero)
					immediateAsync
				// Case: Fixed delay
				else if (delay.start == delay.end) {
					lazy val process = DelayedProcess(WaitDuration(delay.start), shutdownReaction = Some(Cancel)) {
						_ => revalidate(window, canvas)
					}
					() => process.runAsync()
				}
				// Case: Variable delay
				else {
					lazy val process = PostponingRevalidationProcess(window, canvas, delay.toPair)
					() => process.requestRevalidation()
				}
		}
	}
	
	private def revalidate(window: Window, canvas: Stackable) = {
		// Resets cached stack sizes in order to make sure the sizes are set correctly
		canvas.resetCachedSize()
		window.resetCachedSize()
		// TODO: Size optimization may fails sometimes, as the window has a minimum size (low priority bug)
		val windowSizeChanged = AwtEventThread.blocking {
			// Optimizes window bounds based on up-to-date sizes
			window.optimizeBounds()
		}
		// Updates the component layout form top to bottom (only for visible windows)
		//      The update is skipped if the window size was altered,
		//      because Window size changes automatically trigger layout updates
		if (!windowSizeChanged && window.isFullyVisible) {
			window.updateLayout()
			canvas.updateLayout()
		}
	}
	
	
	// NESTED   -----------------------
	
	private object PostponingRevalidationProcess
	{
		def apply(window: => Window, canvas: => Stackable, revalidationDelay: Pair[FiniteDuration])
		         (implicit exc: ExecutionContext, log: Logger) =
		{
			val waitPointer = new PointerWithEvents[WaitTarget](UntilNotified)
			val orderedDelay = revalidationDelay.sorted
			val process = new PostponingRevalidationProcess(waitPointer, window, canvas, orderedDelay.first, orderedDelay.second)
			// Starts the process immediately
			process.runAsync()
			process
		}
	}
	
	private class PostponingRevalidationProcess(waitTargetPointer: PointerWithEvents[WaitTarget],
	                                            window: => Window, canvas: => Stackable,
	                                            minRevalidationDelay: FiniteDuration, maxRevalidationDelay: FiniteDuration)
	                                           (implicit exc: ExecutionContext, log: Logger)
		extends PostponingProcess(waitTargetPointer, shutdownReaction = Some(Cancel))
	{
		// ATTRIBUTES   -----------------
		
		// Contains the latest allowed update time, which is first unfulfilled update request time + max wait duration
		private val latestUpdateTimePointer = VolatileOption[Instant]()
		
		
		// IMPLEMENTED  -----------------
		
		override protected def isRestartable: Boolean = true
		
		override protected def afterDelay(): Unit = {
			// Resets the update request time now that the update completes
			latestUpdateTimePointer.clear()
			revalidate(window, canvas)
		}
		
		
		// OTHER    -------------------
		
		def requestRevalidation() = {
			val maxUpdateTime = latestUpdateTimePointer.setOneIfEmpty(Now + maxRevalidationDelay)
			waitTargetPointer.value = Until((Now + minRevalidationDelay) min maxUpdateTime)
		}
	}
}

case class ReachContentWindowFactory(private val windowFactory: ContextualReachWindowFactory,
                                     context: ReachContentWindowContext)
	extends ReachContentWindowContextualFactory[ReachContentWindowFactory]
{
	// IMPLEMENTED  ----------------------
	
	override def self: ReachContentWindowFactory = this
	
	override def withContext(context: ReachContentWindowContext): ReachContentWindowFactory =
		copy(windowFactory = windowFactory.withContext(context), context = context)
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a new reach window instance
	  * @param factory       A component creation factory to use in the window content creation
	  * @param parent        Parent window, if applicable.
	  *                      Setting this to None will cause a Frame to be created, otherwise a Dialog will be created.
	  * @param title         Title shown on the OS header of this window, if applicable (default = empty)
	  * @param disableAutoBoundsUpdates Whether automatic window bounds updates should be disabled.
	  *                                 This concerns bounds updates that occur at two places:
	  *                                 1) When this window is constructed (once), and
	  *                                 2) Whenever this window becomes visible.
	  *
	  *                                 Set this to false if you intend to perform your own window bounds optimization
	  *                                 upon these two cases.
	  *
	  *                                 Default = false = window automatically updates its bounds
	  * @param createContent A function for creating canvas contents.
	  *                      Accepts the parent Reach canvas and an initialized component creation factory.
	  *                      May return a custom creation result, in addition to the component to wrap.
	  * @tparam F Type of contextual component creation factory used
	  * @tparam C Type of wrapped component
	  * @tparam R Type of additional component creation function result
	  * @return The created window + created canvas + created component + additional function result
	  */
	def using[F, C <: ReachComponentLike, R](factory: FromContextComponentFactoryFactory[TextContext, F],
	                                         parent: Option[java.awt.Window] = None,
	                                         title: LocalizedString = LocalizedString.empty,
	                                         disableAutoBoundsUpdates: Boolean = false)
	                                        (createContent: (ReachCanvas, F) => ComponentCreationResult[C, R]) =
		windowFactory(parent, title, disableAutoBoundsUpdates = disableAutoBoundsUpdates) { hierarchy =>
			createContent(hierarchy.top, factory.withContext(hierarchy, textContext))
		}
	
	/**
	  * Creates a new window, which is anchored so that it stays close to the owner component
	  * @param factory       A component creation factory to use in the window content creation
	  * @param component          A component that "owns" this window
	  * @param preferredAlignment Alignment used when positioning this window relative to the owner component.
	  *                           E.g. If Center is used, will position this window over the center of that component.
	  *                           Or if Right is used, will position this window right of the owner component.
	  *
	  *                           Please note that this alignment may be reversed in case there is not enough space
	  *                           on that side.
	  *
	  *                           Bi-directional alignments, such as TopLeft will place the window next to the component
	  *                           diagonally (so that they won't share any edge together).
	  * @param margin             Margin placed between the owner component and the window, when possible
	  *                           (ignored if preferredAlignment=Center).
	  *                           Default = 0
	  * @param title              Title displayed on this window (provided that OS headers are in use).
	  *                           Default = empty = no title.
	  * @param matchEdgeLength Whether the window should share an edge length with the anchor component.
	  *                        E.g. If bottom alignment is used and 'matchEdgeLength' is enabled, the resulting
	  *                        window will attempt to stretch so that to matches the width of the 'component'.
	  *                        The stacksize limits of the window will be respected, however, and may limit the
	  *                        resizing.
	  *                        Default = false = will not resize the window.
	  * @param keepAnchored       Whether this window should be kept close to the owner component when its size changes
	  *                           or the owner component is moved or resized.
	  *                           Set to false if you don't expect the owner component to move.
	  *                           This will save some resources, as a large number of components needs to be tracked.
	  *                           Default = true.
	  * @param createContent A function for creating canvas contents.
	  *                      Accepts the parent Reach canvas and an initialized component creation factory.
	  *                      May return a custom creation result, in addition to the component to wrap.
	  * @tparam F Type of contextual component creation factory used
	  * @tparam C Type of created canvas content
	  * @tparam R Type of additional function result
	  * @return A new window + created canvas + created canvas content + additional creation result
	  */
	def anchoredToUsing[F, C <: ReachComponentLike, R](factory: FromContextComponentFactoryFactory[TextContext, F],
	                                                   component: ReachComponentLike, preferredAlignment: Alignment,
	                                                   margin: Double = 0.0,
	                                                   title: LocalizedString = LocalizedString.empty,
	                                                   matchEdgeLength: Boolean = false, keepAnchored: Boolean = true)
	                                                  (createContent: (ReachCanvas, F) => ComponentCreationResult[C, R]) =
		windowFactory.anchoredTo(component, preferredAlignment, margin, title, matchEdgeLength, keepAnchored) { hierarchy =>
			createContent(hierarchy.top, factory.withContext(hierarchy, context))
		}
}