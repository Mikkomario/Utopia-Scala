package utopia.reach.window

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.Window
import utopia.firmament.component.stack.Stackable
import utopia.firmament.localization.LocalizedString
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.async.process.WaitTarget.{Until, UntilNotified, WaitDuration}
import utopia.flow.async.process.{DelayedProcess, PostponingProcess, Process, WaitTarget}
import utopia.flow.collection.immutable.Pair
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.view.mutable.eventful.{PointerWithEvents, SettableOnce}
import utopia.paradigm.shape.shape2d.Point
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentCreationResult
import utopia.reach.container.RevalidationStyle.{Delayed, Immediate}
import utopia.reach.container.{ReachCanvas2, RevalidationStyle}
import utopia.reflection.component.drawing.template.CustomDrawer

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for constructing windows that wrap a ReachCanvas element
  * @author Mikko Hilpinen
  * @since 13.4.2023, v1.0
  */
object ReachWindow
{
	// OTHER    ---------------------------
	
	/**
	  * Creates a new reach window instance
	  * @param parent Parent window, if applicable.
	  *               Setting this to None will cause a Frame to be created, otherwise a Dialog will be created.
	  * @param title Title shown on the OS header of this window, if applicable (default = empty)
	  * @param customDrawers Custom drawers to assign. Default = empty.
	  *                      Please note that these will be applied in addition to the contexts' custom drawers.
	  * @param createContent A function for creating canvas contents.
	  *                      Accepts the canvas component hierarchy.
	  *                      May return a custom creation result, in addition to the component to wrap.
	  * @param context Implicit window creation context
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @tparam C Type of wrapped component
	  * @tparam R Type of additional component creation function result
	  * @return The created window + (created canvas + created component + additional function result)
	  */
	def apply[C <: ReachComponentLike, R](parent: Option[java.awt.Window] = None,
	                                      title: LocalizedString = LocalizedString.empty,
	                                      customDrawers: Vector[CustomDrawer] = Vector())
	                                     (createContent: ComponentHierarchy => ComponentCreationResult[C, R])
	                                     (implicit context: ReachWindowContext, exc: ExecutionContext, log: Logger) =
	{
		// Prepares pointers for the window and canvas
		val windowPointer = SettableOnce[Window]()
		val canvasPointer = SettableOnce[Stackable]()
		// The attachment status tracking starts once the window has been created
		val attachmentPointer = windowPointer.flatMap {
			case Some(window) => window.fullyVisibleFlag
			case None => AlwaysFalse
		}
		val absoluteWindowPositionPointer = windowPointer.flatMap {
			case Some(window) => window.positionPointer.map { _ + window.insets.toPoint }
			case None => Fixed(Point.origin)
		}
		
		// Creates the revalidation implementation lazily.
		// Assumes that the window and the canvas have been initialized when revalidate() is first called (throws if not)
		lazy val lazyWindow = windowPointer.get
		lazy val lazyCanvas = canvasPointer.get
		lazy val revalidation = revalidate(lazyWindow, lazyCanvas, context.revalidationStyle)
		
		// Creates the canvas
		val canvas = ReachCanvas2(attachmentPointer, absoluteWindowPositionPointer, context.cursors,
			context.customDrawers ++ customDrawers,
			disableFocus = !context.focusEnabled) { _ => revalidation() }(createContent)
		canvasPointer.set(canvas)
		
		// Creates the window
		val window = Window.contextual(canvas.parent.component, canvas.parent, parent, title,
			context.getAnchor(canvas, _))
		windowPointer.set(window)
		
		// Returns the canvas and the window
		window -> canvas
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
						_ => revalidate(window, canvas) }
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
		AwtEventThread.async {
			// Optimizes window bounds based on up-to-date sizes
			window.optimizeBounds()
			// Updates the component layout form top to bottom
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
