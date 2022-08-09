package utopia.reach.container

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.VolatileList
import utopia.flow.event.ChangingLike
import utopia.genesis.event.{MouseButtonStateEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.handling.mutable.{MouseButtonStateHandler, MouseMoveHandler, MouseWheelHandler}
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size, Vector2D}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.util.{Priority, RealTimeReachPaintManager}
import utopia.reflection.component.template.layout.stack.Stackable2
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.text.Font

import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}

/**
  * The component that connects a reach component hierarchy to the swing component hierarchy
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
// TODO: Continue working on or remove this class
class ReachCanvas2/*(contentFuture: Future[ReachComponentLike], attachmentPointer: ChangingLike[Boolean],
                    disableDoubleBufferingDuringDraw: Boolean = true, syncAfterDraw: Boolean = true)
                   (revalidate: () => Unit)(implicit exc: ExecutionContext)
	extends Stackable2
{
	// ATTRIBUTES	---------------------------
	
	private val layoutUpdateQueue = VolatileList[Seq[ReachComponentLike]]()
	private val updateFinishedQueue = VolatileList[() => Unit]()
	
	// override val focusManager = new ReachFocusManager(panel)
	// TODO: Allow custom repainters
	// TODO: Control repainter position
	private val painterPromise = contentFuture.map { c => RealTimeReachPaintManager(c,
		disableDoubleBuffering = disableDoubleBufferingDuringDraw, syncAfterDraw = syncAfterDraw) }
	
	lazy val mouseButtonHandler = MouseButtonStateHandler()
	lazy val mouseMoveHandler = MouseMoveHandler()
	lazy val mouseWheelHandler = MouseWheelHandler()
	
	
	// COMPUTED -------------------------------
	
	private def currentContent = contentFuture.current.flatMap { _.toOption }
	
	def currentPainter = painterPromise.current.flatMap { _.toOption }
	
	
	// IMPLEMENTED	---------------------------
	
	// TODO: Get font metrics from the main canvas
	override def fontMetrics(font: Font) = ???
	
	// TODO: Do not wrap position / size but control own which is synchronized in updateLayout
	override def position = currentContent.map { _.position }.getOrElse(Point.origin)
	override def position_=(p: Point) = currentContent.foreach { _.position = p }
	
	override def size = currentContent.map { _.size }.getOrElse(Size.zero)
	override def size_=(s: Size) = currentContent.foreach { _.size = s }
	
	override def bounds = currentContent.map { _.bounds }.getOrElse(Bounds.zero)
	override def bounds_=(b: Bounds) = currentContent.foreach { _.bounds = b }
	
	override def updateLayout(): Unit =
	{
		// Updates content size and layout
		updateLayout(layoutUpdateQueue.popAll().toSet, size)
		
		// Performs the queued tasks
		updateFinishedQueue.popAll().foreach { _() }
	}
	
	override def stackSize = currentContent match
	{
		case Some(content) => content.stackSize
		case None => StackSize.any
	}
	
	override def resetCachedSize() = currentContent.foreach { _.resetCachedSize() }
	
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
	
	
	// OTHER    ------------------------------
	
	/**
	  * Revalidates this component, queueing some component layout updates to be done afterwards
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  */
	def revalidate(updateComponents: Seq[ReachComponentLike]): Unit =
	{
		if (updateComponents.nonEmpty)
			layoutUpdateQueue :+= updateComponents
		revalidate() // FIXME: Propagate revalidation to main canvas
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
	  * Performs a layout update for all components in this canvas. Should be called when setting up this canvas /
	  * if updates were previously ignored and can't be tracked anymore
	  * @param targetContentSize Size to assign for the managed component
	  */
	def updateWholeLayout(targetContentSize: Size) = currentContent.foreach { content =>
		val branches = content.toTree.allBranches
		val updateQueues: Set[Seq[ReachComponentLike]] =
		{
			if (branches.isEmpty)
				Set(Vector(content))
			else
				branches.map { content +: _ }.toSet
		}
		updateLayout(updateQueues, targetContentSize)
	}
	
	/**
	  * Updates component layout based on queued updates
	  * @param queues Sequences of components from hierarchy top downwards that require a layout update
	  * @param componentTargetSize Size to assign for the managed component
	  */
	protected def updateLayout(queues: Set[Seq[ReachComponentLike]], componentTargetSize: Size) =
	{
		// Updates content size
		val contentSizeChanged = currentContent match
		{
			case Some(content) =>
				val requiresSizeUpdate = content.size != componentTargetSize
				if (requiresSizeUpdate)
					content.size = componentTargetSize
				requiresSizeUpdate
			case None => false
		}
		
		// Updates content layout
		val layoutUpdateQueues = queues.map { q: Seq[ReachComponentLike] => q -> contentSizeChanged }
		val sizeChangeTargets: Set[ReachComponentLike] =
		{
			if (contentSizeChanged)
				currentContent.toSet
			else
				Set()
		}
		if (layoutUpdateQueues.nonEmpty)
			updateLayoutFor(layoutUpdateQueues, sizeChangeTargets).foreach { repaint(_) }
	}
	
	/**
	  * Requests a repaint for this whole canvas element
	  */
	// TODO: Send repaint events to the main canvas for proper paint ordering
	def repaint() = currentPainter.foreach { _.repaintAll() }
	
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
		// FIXME: This works only after ReachCanvas reference has been updated
		override def parent = ??? // Left(ReachCanvas2.this)
		
		override def linkPointer = attachmentPointer
		
		override def isThisLevelLinked = isLinked
	}
}
*/