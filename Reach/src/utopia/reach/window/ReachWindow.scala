package utopia.reach.window

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.Window
import utopia.firmament.component.stack.Stackable
import utopia.firmament.context.WindowContext
import utopia.firmament.localization.LocalizedString
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
import utopia.flow.view.mutable.eventful.{PointerWithEvents, SettableOnce}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.Screen
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.{ComponentCreationResult, WindowCreationResult}
import utopia.reach.container.RevalidationStyle.{Delayed, Immediate}
import utopia.reach.container.{ReachCanvas2, RevalidationStyle}
import utopia.reflection.component.drawing.template.CustomDrawer

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
	                        (implicit c: ReachWindowContext, exc: ExecutionContext, log: Logger): ContextualReachWindowFactory =
		f.contextual
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @param context Implicit window creation context
	  * @param exc Implicit execution context
	  * @param log Implicit logging execution
	  * @return A new Reach window factory that uses the specified context
	  */
	def contextual(implicit context: ReachWindowContext, exc: ExecutionContext, log: Logger) =
		withContext(context)
	
	
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
	  * @param actorHandler Actor handler to use
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @return A new reach window factory that uses the default context
	  */
	def withActorHandler(actorHandler: ActorHandler)(implicit exc: ExecutionContext, log: Logger) =
		withContext(ReachWindowContext(WindowContext(actorHandler)))
	
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
		println("Revalidating window")
		// Resets cached stack sizes in order to make sure the sizes are set correctly
		canvas.resetCachedSize()
		window.resetCachedSize()
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
	
	case class ContextualReachWindowFactory(context: ReachWindowContext)(implicit exc: ExecutionContext, log: Logger)
		extends ReachWindowContextWrapper[ContextualReachWindowFactory]
	{
		// ATTRIBUTES   ---------------
		
		private implicit val c: ReachWindowContext = context
		
		
		// IMPLEMENTED  ---------------
		
		override def self: ContextualReachWindowFactory = this
		override def wrapped: ReachWindowContext = context
		
		override def withReachBase(base: ReachWindowContext): ContextualReachWindowFactory = copy(base)
		
		
		// OTHER    ------------------
		
		def withContext(context: ReachWindowContext) = copy(context)
		def mapContext(f: ReachWindowContext => ReachWindowContext) = withContext(f(context))
		
		/**
		  * Creates a new reach window instance
		  * @param parent        Parent window, if applicable.
		  *                      Setting this to None will cause a Frame to be created, otherwise a Dialog will be created.
		  * @param title         Title shown on the OS header of this window, if applicable (default = empty)
		  * @param customDrawers Custom drawers to assign. Default = empty.
		  *                      Please note that these will be applied in addition to the contexts' custom drawers.
		  * @param createContent A function for creating canvas contents.
		  *                      Accepts the canvas component hierarchy.
		  *                      May return a custom creation result, in addition to the component to wrap.
		  * @tparam C Type of wrapped component
		  * @tparam R Type of additional component creation function result
		  * @return The created window + created canvas + created component + additional function result
		  */
		def apply[C <: ReachComponentLike, R](parent: Option[java.awt.Window] = None,
		                                      title: LocalizedString = LocalizedString.empty,
		                                      customDrawers: Vector[CustomDrawer] = Vector())
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
			val canvas = ReachCanvas2(attachmentPointer, Right(absoluteWindowPositionPointer), context.cursors,
				context.customDrawers ++ customDrawers,
				disableFocus = !context.focusEnabled) { _ => revalidation() }(createContent)
			canvasPointer.set(canvas)
			
			// Creates the window
			val window = Window.contextual(canvas.parent.component, canvas.parent, parent, title,
				context.getAnchor(canvas, _))
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
		  * @param customDrawers      Custom drawers to assign to this window, in addition to those specified in the context.
		  *                           Default = empty.
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
		                                           customDrawers: Vector[CustomDrawer] = Vector(),
		                                           keepAnchored: Boolean = true)
		                                          (createContent: ComponentHierarchy => ComponentCreationResult[C, R]) =
		{
			// Full-screen is not supported for anchored windows
			val factory = windowed.withAnchorAlignment(preferredAlignment)
			// Creates the window and the canvas
			val windowCreation = factory(component.parentHierarchy.top.parentWindow, title,
				customDrawers)(createContent)
			val window = windowCreation.window
			
			// Determines the optimal position for the window
			lazy val screenArea = {
				val base = Bounds(Point.origin, Screen.actualSize)
				if (context.screenInsetsEnabled)
					base - Screen.actualInsetsAt(component.parentHierarchy.top.component.getGraphicsConfiguration)
				else
					base
			}
			
			def optimizeWindowPosition() = {
				window.position = preferredAlignment.positionRelativeToWithin(window.size, component.absoluteBounds,
					screenArea, margin, swapToFit = true).position
			}
			
			optimizeWindowPosition()
			
			// Updates the window position whenever the owner component's absolute position changes,
			// or when the window's size changes
			// (May be disabled)
			/* TODO: Return
			if (keepAnchored) {
				val repositionListener = ChangeListener.onAnyChange {
					optimizeWindowPosition()
					// Stops reacting to events once the window has closed
					DetachmentChoice.continueUntil(window.hasClosed)
				}
				window.sizePointer.addListener(repositionListener)
				component.boundsPointer.addListener(repositionListener)
				component.parentHierarchy.parentsIterator.foreach { _.positionPointer.addListener(repositionListener) }
				// The canvas absolute position tracking may not be working
				component.parentCanvas.absolutePositionView.toOption.foreach { _.addListener(repositionListener) }
			}*/
			
			windowCreation
		}
	}
	
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
