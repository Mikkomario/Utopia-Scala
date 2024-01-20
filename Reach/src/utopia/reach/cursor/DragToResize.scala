package utopia.reach.cursor

import utopia.firmament.model.stack.StackSize
import utopia.flow.async.process.PostponingProcess
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.{NumericSpan, Span}
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.sign.Sign
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.VolatileOption
import utopia.genesis.event.{Consumable, ConsumeEvent, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{MouseButtonStateListener, MouseMoveHandlerType, MouseMoveListener}
import utopia.genesis.util.Screen
import utopia.genesis.view.GlobalMouseEventHandler
import utopia.inception.handling.HandlerType
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.template.ReachComponentLike

import scala.annotation.unused
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

object DragToResize
{
	// IMPLICIT ------------------------------
	
	implicit def objectAsFactory(@unused o: DragToResize.type): DragToResizeFactory = DragToResizeFactory()
	
	
	// NESTED   ------------------------------
	
	case class DragToResizeFactory(dragAxes: Set[Axis2D] = Axis2D.values.toSet,
	                               updateDelay: FiniteDuration = Duration.Zero, expandAtSides: Boolean = false,
	                               fillAtTop: Boolean = false)
	{
		// COMPUTED --------------------------
		
		/**
		  * @return Copy of this factory that only allows horizontal resizing
		  */
		def onlyHorizontally = onlyAlong(X)
		/**
		  * @return Copy of this factory that only allows vertical resizing
		  */
		def onlyVertically = onlyAlong(Y)
		
		/**
		  * @return Copy of this factory that automatically expands the content once it reaches the area edge
		  */
		def expandingAtSides = copy(expandAtSides = true)
		/**
		  * @return Copy of this factory that automatically fills the area once the content reaches the top
		  */
		def fillingAtTop = copy(fillAtTop = true)
		
		
		// OTHER    --------------------------
		
		/**
		  * @param axis Targeted axis
		  * @return Copy of this factory that only allows resizing along the specified axis
		  */
		def onlyAlong(axis: Axis2D) = copy(dragAxes = Set(axis))
		
		/**
		  * @param delay Delay before applying size changes
		  * @return Copy of this factory that delays the size changes by the specified amount
		  */
		def delayedBy(delay: FiniteDuration) = copy(updateDelay = updateDelay + delay)
		
		/**
		  * @param component Component that will be resized
		  * @param dragBorders Sizes of the borders at which resizing is enabled
		  * @param exc Implicit execution context for the resizing
		  * @param log Implicit logging implementation for possible unexpected resizing errors
		  */
		def applyTo(component: ReachComponentLike, dragBorders: Insets)
		           (implicit exc: ExecutionContext, log: Logger): Unit =
			new DragToResize(component, dragBorders, dragAxes, updateDelay, expandAtSides, fillAtTop)
	}
}

/**
  * An interface that manages a component, allowing the user to resize it by dragging it from the edges
  * @author Mikko Hilpinen
  * @since 20/01/2024, v1.2
  */
class DragToResize protected(component: ReachComponentLike, dragBorders: Insets,
                             dragAxes: Set[Axis2D] = Axis2D.values.toSet,
                             updateDelay: FiniteDuration = Duration.Zero, expandAtSides: Boolean = false,
                             fillAtTop: Boolean = false)
                            (implicit exc: ExecutionContext, log: Logger)
{
	// ATTRIBUTES   ------------------------------
	
	// The resizing is implemented differently based on the component's context
	// (whether it is the main window component, a canvas root component or a sub-component)
	private lazy val (getBounds, getArea, setBounds) = component.parentHierarchy.parent match {
		// Case: Canvas root component => Directly manipulates the window, if possible
		case Left(canvas) =>
			canvas.parentWindow match {
				// Case: Window-manipulation is possible
				case Some(window) =>
					val getBounds = () => Bounds(window.getBounds)
					val getArea = () => Screen.size
					val setBounds = { b: Bounds => window.setBounds(b.toAwt) }
					(getBounds, getArea, setBounds)
				// Case: Window-manipulation is not possible => Manipulates the canvas instead
				case None =>
					val getBounds = () => canvas.bounds
					val setBounds = { b: Bounds =>
						canvas.bounds = b
						canvas.updateWholeLayout(canvas.size)
					}
					canvas.component.getParent match {
						// Case: No parent component => No limits applied until the canvas is attached
						case null =>
							val getArea = () => {
								Option(canvas.component.getParent) match {
									case Some(parent) => Size(parent.getSize)
									case None => Screen.size
								}
							}
							(getBounds, getArea, setBounds)
						// Case: Parent component present => The component establishes the growth limits
						case parent =>
							val getArea = () => Size(parent.getSize)
							(getBounds, getArea, setBounds)
					}
			}
		// Case: Reach component within another reach component => The parent component establishes the growth limits
		case Right((_, parent)) =>
			val getBounds = () => component.bounds
			val getArea = () => parent.size
			val setBounds = { b: Bounds =>
				val originalBounds = component.bounds
				component.bounds = b
				component.updateLayout()
				parent.repaintArea(Bounds.around(Pair(originalBounds, b)))
			}
			(getBounds, getArea, setBounds)
	}
	
	
	// INITIAL CODE ------------------------
	
	// Starts tracking the mouse
	component.addMouseButtonListener(MouseListener)
	
	
	// NESTED   ---------------------------
	
	private object MouseListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   -------------------------
		
		// 1 = Absolute mouse position at drag start
		// 2 = Component bounds at drag start
		// 3 = Component stack size (at drag start)
		// 4 = Maximum bounds at drag start
		// 5 = Drag directions
		private var drag: Option[(Point, Bounds, StackSize, Bounds, Map[Axis2D, End])] = None
		
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent.leftPressedFilter && Consumable.notConsumedFilter
		
		
		// IMPLEMENTED  -------------------------
		
		// Case: Mouse pressed
		override def onMouseButtonState(event: MouseButtonStateEvent): Option[ConsumeEvent] = {
			val componentBounds = component.bounds
			// Case: Mouse pressed within the component => Checks whether pressed at the drag-area
			// TODO: Could add external borders here
			if (componentBounds.contains(event.mousePosition)) {
				val innerArea = componentBounds - dragBorders
				// Case: Pressed near the borders => Determines the drag directions
				if (!innerArea.contains(event.mousePosition)) {
					val relativePosition = event.mousePosition - componentBounds.position
					val directions = dragAxes.flatMap { axis =>
						val p = relativePosition(axis)
						val borders = dragBorders(axis)
						if (p <= borders.first)
							Some(axis -> First)
						else if (p >= componentBounds.size(axis) - borders.second)
							Some(axis -> Last)
						else
							None
					}
					// Case: Drag start
					if (directions.nonEmpty) {
						drag = Some((event.absoluteMousePosition, getBounds(), component.stackSize,
							Bounds(Point.origin, getArea()), directions.toMap))
						GlobalMouseEventHandler += ReleaseListener
						GlobalMouseEventHandler += DragListener
						Some(ConsumeEvent("Resize-drag"))
					}
					// Case: These directions were not supported
					else
						None
				}
				// Case: Mouse pressed inside the component
				else
					None
			}
			// Case: Mouse pressed outside the component
			else
				None
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType): Boolean = true
		
		
		// NESTED   ------------------------
		
		private object DragListener extends MouseMoveListener
		{
			// ATTRIBUTES   ----------------
			
			private val queuedBoundsP = VolatileOption[Bounds]()
			private val updateBoundsProcess = PostponingProcess.by(Span.singleValue(updateDelay)) {
				queuedBoundsP.pop().foreach(setBounds)
			}
			
			
			// IMPLEMENTED  ----------------
			
			override def onMouseMove(event: MouseMoveEvent): Unit = {
				// Applies the drag
				drag.foreach { case (absoluteOrigin, originalBounds, stackSize, maxBounds, directions) =>
					val totalDrag = event.absoluteMousePosition - absoluteOrigin
					// Calculates the would-be bounds (without limits applied yet)
					val newBounds = Bounds.fromFunction2D { axis =>
						directions.get(axis) match {
							case Some(direction) =>
								val shift = totalDrag(axis)
								val default = originalBounds(axis).mapSpecificEnd(direction) { _ + shift }.ascending
								val defaultLength = default.length
								
								// Makes sure the stack size is respected, also
								val limits = stackSize(axis)
								limits.max.filter { _ < defaultLength } match {
									// Case: Limited by the maximum length => Converts into drag
									case Some(maximumLength) =>
										default.withEnd(default(direction) + (Sign(direction.opposite) * maximumLength),
											direction.opposite)
									case None =>
										// Case: Limited by the minimum length => Converts into push (with limits)
										if (limits.min > defaultLength) {
											val minimized = default.withEnd(
												default(direction) + (Sign(direction.opposite) * limits.min),
												direction.opposite)
											minimized.shiftedInto(maxBounds(axis))
										}
										// Case: No size limits apply
										else
											default
								}
							case None => originalBounds(axis)
						}
					}
					// Case: Fill at top activated => Maximizes size (if possible)
					if (fillAtTop && newBounds.topY <= maxBounds.topY &&
						stackSize.forAllDimensionsWith(maxBounds.size) { (limit, target) => limit.max.forall { target <= _ } })
						updateBounds(maxBounds)
					else {
						// Checks whether expand at side is activated
						val expandedSide = {
							if (expandAtSides)
								Direction2D.values.find { d =>
									d.sign.toOrdering[Double].gt(newBounds(d), maxBounds(d)) &&
										stackSize(d.axis.perpendicular).max
											.forall { _ >= maxBounds.size(d.axis.perpendicular) }
								}
							else
								None
						}
						expandedSide match {
							// Case: Expands at one side
							case Some(d) =>
								updateBounds(Bounds.fromFunction2D { axis =>
									if (axis == d.axis)
										newBounds(axis).overlapWith(maxBounds(axis))
											.getOrElse(NumericSpan.singleValue(0.0))
									else
										maxBounds(axis)
								})
							// Case: No expanding applied => Limits to the maximum bounds
							case None => newBounds.overlapWith(maxBounds).foreach(updateBounds)
						}
					}
				}
			}
			
			override def allowsHandlingFrom(handlerType: HandlerType): Boolean = handlerType match {
				case MouseMoveHandlerType => drag.isDefined
				case _ => true
			}
			
			
			// OTHER    --------------------------
			
			// Updates the bounds asynchronously in order to avoid duplicate and too frequent processes
			private def updateBounds(newBounds: Bounds) = {
				queuedBoundsP.value = Some(newBounds)
				updateBoundsProcess.runAsync(loopIfRunning = true)
			}
		}
		
		private object ReleaseListener extends MouseButtonStateListener
		{
			override def mouseButtonStateEventFilter =
				MouseButtonStateEvent.leftReleasedFilter
			
			// Stops the drag
			override def onMouseButtonState(event: MouseButtonStateEvent): Option[ConsumeEvent] = {
				drag = None
				GlobalMouseEventHandler -= this
				GlobalMouseEventHandler -= MouseListener.DragListener
				None
			}
			
			override def allowsHandlingFrom(handlerType: HandlerType): Boolean = drag.isDefined
		}
	}
}
