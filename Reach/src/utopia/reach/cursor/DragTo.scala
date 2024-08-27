package utopia.reach.cursor

import utopia.firmament.model.stack.StackSize
import utopia.flow.async.process.PostponingProcess
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.{NumericSpan, Span}
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.consume.Consumable
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateEvent, MouseButtonStateListener, MouseMoveEvent, MouseMoveListener}
import utopia.genesis.util.Screen
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.cursor.DragTo.RepositionLogic.{RepositionComponent, RepositionParent, RepositionWindow}
import utopia.reach.cursor.DragTo.{RepositionLogic, componentBoundsActions, windowBoundsActions}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

object DragTo
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Creates a factory for constructing resizing-dragging
	  */
	def resize = DragToFactory()
	
	/**
	  * @return Creates a factory for constructing respositioning-dragging
	  */
	def reposition = DragToFactory(resizeAxes = Set(), repositionLogic = Some(RepositionComponent))
	/**
	  * @return Creates a factory for constructing window-respositioning-dragging
	  */
	def repositionWindow = DragToFactory(resizeAxes = Set(), repositionLogic = Some(RepositionWindow))
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a factory for constructing respositioning-dragging
	  * @param component The component that will be repositioned
	  * @return A new factory that repositions the specified component
	  */
	def repositionOther(component: ReachComponentLike) =
		DragToFactory(resizeAxes = Set(), repositionLogic = Some(RepositionParent(component)))
	
	private def windowBoundsActions(window: java.awt.Window) = {
		val getBounds = () => Bounds(window.getBounds)
		val getArea = () => Screen.size
		val setBounds = { b: Bounds => window.setBounds(b.toAwt) }
		(getBounds, getArea, setBounds)
	}
	
	private def componentBoundsActions(component: ReachComponentLike) =
		component.parentHierarchy.parent match {
			// Case: Canvas root component => Directly manipulates the window, if possible
			case Left(canvas) =>
				canvas.parentWindow match {
					// Case: Window-manipulation is possible
					case Some(window) => windowBoundsActions(window)
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
	
	
	// NESTED   ------------------------------
	
	/**
	  * An enumeration for different ways of handling mouse-based component-repositioning
	  */
	sealed trait RepositionLogic
	
	object RepositionLogic
	{
		/**
		  * Repositions the clicked component within its parent container
		  */
		case object RepositionComponent extends RepositionLogic
		/**
		  * Repositions the window the component is in
		  */
		case object RepositionWindow extends RepositionLogic
		
		/**
		  * Repositions a specific parent component in the component's hierarchy
		  * @param component A component to which the repositioning applies
		  */
		case class RepositionParent(component: ReachComponentLike) extends RepositionLogic
	}
	
	case class DragToFactory(resizeAxes: Set[Axis2D] = Axis2D.values.toSet,
	                         repositionLogic: Option[RepositionLogic] = None,
	                         updateDelay: FiniteDuration = Duration.Zero,
	                         expandAtSides: Boolean = false, fillAtTop: Boolean = false)
	{
		// COMPUTED --------------------------
		
		/**
		  * @return Copy of this factory that allows the component to be repositioned by dragging inside it
		  */
		def repositioning = repositioningUsing(RepositionComponent)
		/**
		  * @return Copy of this factory that allows the component's window to be repositioned by dragging the
		  *         component
		  */
		def repositioningWindow = repositioningUsing(RepositionWindow)
		
		/**
		  * @return Copy of this factory that applies resizing at the edges
		  */
		def resizing = copy(resizeAxes = Axis2D.values.toSet)
		
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
		  * Applies repositioning by dragging inside the targeted component
		  * @param logic Logic for repositioning
		  * @return Copy of this factory that applies the specified repositioning logic
		  */
		def repositioningUsing(logic: RepositionLogic) = copy(repositionLogic = Some(logic))
		/**
		  * @param component Component to reposition when the targeted component is dragged
		  * @return Copy of this factory that applies repositioning to the specified component
		  */
		def repositioningOther(component: ReachComponentLike) =
			repositioningUsing(RepositionParent(component))
		
		/**
		  * @param axis Targeted axis
		  * @return Copy of this factory that only allows resizing along the specified axis
		  */
		def onlyAlong(axis: Axis2D) = copy(resizeAxes = Set(axis))
		
		/**
		  * @param delay Delay before applying size changes
		  * @return Copy of this factory that delays the size changes by the specified amount
		  */
		def delayedBy(delay: FiniteDuration) = copy(updateDelay = updateDelay + delay)
		
		/**
		  * @param component Component that will be resized
		  * @param activeBorders Sizes of the borders at which resizing is enabled
		  * @param exc Implicit execution context for the resizing
		  * @param log Implicit logging implementation for possible unexpected resizing errors
		  */
		def applyTo(component: ReachComponentLike, activeBorders: Insets)
		           (implicit exc: ExecutionContext, log: Logger): Unit =
			new DragTo(component, activeBorders, repositionLogic, resizeAxes, updateDelay, expandAtSides, fillAtTop)
	}
}

/**
  * An interface that manages a component, allowing the user to resize it by dragging it from the edges
  * @author Mikko Hilpinen
  * @since 20/01/2024, v1.2
  */
class DragTo protected(component: ReachComponentLike, resizeActiveInsets: Insets,
                       repositionLogic: Option[RepositionLogic] = None,
                       resizeAxes: Set[Axis2D] = Axis2D.values.toSet,
                       updateDelay: FiniteDuration = Duration.Zero, expandAtSides: Boolean = false,
                       fillAtTop: Boolean = false)
                      (implicit exc: ExecutionContext, log: Logger)
{
	// ATTRIBUTES   ------------------------------
	
	// The resizing is implemented differently based on the component's context
	// (whether it is the main window component, a canvas root component or a sub-component)
	private lazy val (getBounds, getArea, setBounds) = {
		repositionLogic match {
			// Case: Repositioning instead of resizing
			case Some(reposition) =>
				reposition match {
					// Case: Positioning the targeted component
					case RepositionComponent => componentBoundsActions(component)
					// Case: Positioning the parent window (may fail)
					case RepositionWindow =>
						component.parentWindow match {
							case Some(window) => windowBoundsActions(window)
							case None => componentBoundsActions(component)
						}
					// Case: Positioning a parent component
					case RepositionParent(parent) => componentBoundsActions(parent)
				}
			// Case: Resizing the component
			case None => componentBoundsActions(component)
		}
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
		// 5 = Drag directions, which are empty when repositioning
		private val dragPointer = EventfulPointer.empty[(Point, Bounds, StackSize, Bounds, Map[Axis2D, End])]()
		private val draggingFlag: Flag = dragPointer.map { _.isDefined }
		
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent.filter.leftPressed && Consumable.unconsumedFilter
			
		
		// COMPUTED -----------------------------
		
		private def drag = dragPointer.value
		private def drag_=(newDrag: Option[(Point, Bounds, StackSize, Bounds, Map[Axis2D, End])]) =
			dragPointer.value = newDrag
		
		
		// IMPLEMENTED  -------------------------
		
		override def handleCondition: Flag = AlwaysTrue
		
		// Case: Mouse pressed
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			val componentBounds = component.bounds
			// Case: Mouse pressed within the component => Checks whether pressed at the drag-area
			// TODO: Could add external borders here
			if (componentBounds.contains(event.position)) {
				val innerArea = componentBounds - resizeActiveInsets
				// Case: Clicked inside => Applies repositioning, if appropriate
				if (innerArea.contains(event.position)) {
					if (repositionLogic.nonEmpty)
						startDrag(event.position.absolute)
					else
						Preserve
				}
				// Case: Pressed near the borders => Determines the drag directions
				else {
					val relativePosition = event.position - componentBounds.position
					val directions = resizeAxes.flatMap { axis =>
						val p = relativePosition(axis)
						val borders = resizeActiveInsets(axis)
						if (p <= borders.first)
							Some(axis -> First)
						else if (p >= componentBounds.size(axis) - borders.second)
							Some(axis -> Last)
						else
							None
					}
					// Case: Drag start
					if (directions.nonEmpty)
						startDrag(event.position.absolute, directions.toMap)
					// Case: These directions were not supported => Applies repositioning, if appropriate
					else if (repositionLogic.nonEmpty && resizeAxes.nonEmpty)
						startDrag(event.position.absolute)
					// Case: Repositioning not appropriate => Ignores the event
					else
						Preserve
				}
			}
			// Case: Mouse pressed outside the component
			else
				Preserve
		}
		
		
		// OTHER    ------------------------
		
		// Directions is empty if drag-positioning
		private def startDrag(absoluteMousePosition: Point, directions: Map[Axis2D, End] = Map()) = {
			drag = Some((absoluteMousePosition, getBounds(), component.stackSize,
				Bounds(Point.origin, getArea()), directions))
			CommonMouseEvents += ReleaseListener
			CommonMouseEvents += DragListener
			Consume("drag-start")
		}
		
		
		// NESTED   ------------------------
		
		private object DragListener extends MouseMoveListener
		{
			// ATTRIBUTES   ----------------
			
			private val queuedBoundsP = VolatileOption[Bounds]()
			private val updateBoundsProcess = PostponingProcess.by(Span.singleValue(updateDelay)) {
				queuedBoundsP.pop().foreach(setBounds)
			}
			
			
			// IMPLEMENTED  ----------------
			
			override def handleCondition: Flag = draggingFlag
			override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
			
			override def onMouseMove(event: MouseMoveEvent): Unit = {
				// Applies the drag
				drag.foreach { case (absoluteOrigin, originalBounds, stackSize, maxBounds, directions) =>
					val totalDrag = event.position.absolute - absoluteOrigin
					// Calculates the would-be bounds (without limits applied yet)
					val newBounds = {
						// Case: Repositioning
						if (directions.isEmpty)
							(originalBounds + totalDrag.toVector2D).shiftedInto(maxBounds)
						// Case: Resizing
						else
							Bounds.fromFunction2D { axis =>
								directions.get(axis) match {
									case Some(direction) =>
										val shift = totalDrag(axis)
										val default = originalBounds(axis)
											.mapSpecificEnd(direction) { _ + shift }.ascending
										val defaultLength = default.length
										
										// Makes sure the stack size is respected, also
										val limits = stackSize(axis)
										limits.max.filter { _ < defaultLength } match {
											// Case: Limited by the maximum length => Converts into drag
											case Some(maximumLength) =>
												default.withEnd(default(direction) +
													(Sign(direction.opposite) * maximumLength),
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
			
			
			// OTHER    --------------------------
			
			// Updates the bounds asynchronously in order to avoid duplicate and too frequent processes
			private def updateBounds(newBounds: Bounds) = {
				queuedBoundsP.value = Some(newBounds)
				updateBoundsProcess.runAsync(loopIfRunning = true)
			}
		}
		
		private object ReleaseListener extends MouseButtonStateListener
		{
			// ATTRIBUTES   ------------------
			
			override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.leftReleased
			
			
			// IMPLEMENTED  ------------------
			
			override def handleCondition: Flag = draggingFlag
			
			// Stops the drag
			override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
				drag = None
				CommonMouseEvents -= this
				CommonMouseEvents -= MouseListener.DragListener
			}
		}
	}
}
