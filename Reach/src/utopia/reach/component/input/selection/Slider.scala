package utopia.reach.component.input.selection

import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState
import utopia.firmament.model.enumeration.GuiElementState.{Activated, Disabled, Focused, Hover}
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.{CopyOnDemand, EventfulPointer, ResettableFlag}
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.animation.{Animator, AnimatorInstruction}
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.consume.ConsumeChoice.Consume
import utopia.genesis.handling.event.keyboard.KeyDownEvent.KeyDownEventFilter
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{KeyDownEvent, KeyDownListener, KeyStateEvent, KeyStateListener}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseDragEvent, MouseDragListener, MouseMoveEvent, MouseMoveListener}
import utopia.paradigm.animation.Animation
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.component.template.{CustomDrawReachComponent, HasGuiState}
import utopia.reach.focus.FocusListener

import scala.concurrent.duration.FiniteDuration

/**
  * A component used for choosing from a linear range of possible values
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.3.1
  */
class Slider[A](override val parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler,
                override val valuePointer: EventfulPointer[A],
                stackWidth: StackLength, optimalKnobDiameter: Double, barSideColors: Pair[Color],
                knobColorFunction: Either[Color, Either[Double => Color, A => Color]],
                stickingPoints: Seq[Double] = Empty, progressWithArrow: Double = 0.2,
                progressVelocityWithArrow: LinearVelocity = LinearVelocity(1.0, 1.seconds),
                hoverRadius: Double = 0.0, hoverColor: Color = Color.white, leftToRightBarHeightRatio: Double = 1.0,
                animationDuration: FiniteDuration = 0.2.seconds, maxJumpWithoutAnimationDistance: Double = 2.0,
                enabledFlag: FlagLike = AlwaysTrue, override val focusListeners: Seq[FocusListener] = Empty,
                additionalDrawers: IterableOnce[CustomDrawer] = Empty)
               (progressToSelection: Double => A)(selectionToProgress: A => Double)
	extends CustomDrawReachComponent with InteractionWithPointer[A] with HasGuiState with FocusableWithState
{
	// ATTRIBUTES   --------------------------
	
	override val focusId: Int = hashCode()
	
	override val calculatedStackSize: StackSize = {
		val knob = StackLength(optimalKnobDiameter * 0.5, optimalKnobDiameter, optimalKnobDiameter * 2)
		val knobPlusHover = knob + StackLength.downscaling(hoverRadius)
		val width = stackWidth max (knob * 3) max knobPlusHover
		
		width x knobPlusHover
	}
	
	private val _focusPointer = ResettableFlag()
	override def focusPointer: FlagLike = _focusPointer.readOnly
	
	private val enabledAndFocusedFlag = _focusPointer && enabledFlag
	
	// Contains true if mouse is hovering over the knob
	private val hoverFlag = ResettableFlag()
	
	// Contains true while this slider is being dragged using the mouse
	private val draggingFlag = ResettableFlag()
	// Contains Some(initialProgress -> direction) while this slider is being adjusted with a keyboard key
	private val keyDownPointer = EventfulPointer.empty[(Double, Sign)]()
	private val keyDownFlag: FlagLike = keyDownPointer.strongMap { _.isDefined }
	
	private val activatedFlag = draggingFlag || keyDownFlag
	
	// This state pointer needs to be updated manually (because there are so many inputs)
	private val statePointer = CopyOnDemand {
		val builder = OptimizedIndexedSeq.newBuilder[GuiElementState]
		if (_focusPointer.value)
			builder += Focused
		if (hoverFlag.value)
			builder += Hover
		if (activatedFlag.value)
			builder += Activated
		if (enabledFlag.isNotSet)
			builder += Disabled
		
		GuiElementStatus(Set.from(builder.result()))
	}
	private val updateStateListener = ChangeListener.onAnyChange { statePointer.update() }
	
	// Contains maximum amount of progress that may be jumped without an animation
	private lazy val maxJumpProgressPointer = sizePointer.map { size => maxJumpWithoutAnimationDistance / size.width }
	
	private val rawProgressPointer = EventfulPointer(selectionToProgress(value))
	// This version of the progress pointer applies "sticking", if appropriate
	private val progressPointer =
		if (stickingPoints.isEmpty) rawProgressPointer else rawProgressPointer.strongMap(stick)
	
	// Used for directing the animator to visualize progress changes
	private val animatorInstructionPointer = progressPointer.incrementalMap(AnimatorInstruction.fixed) { (_, event) =>
		// Case: The change in progress is so small that no animation is needed
		if (draggingFlag.isSet || keyDownFlag.isSet || event.values.merge { _ - _ }.abs < maxJumpProgressPointer.value)
			AnimatorInstruction.fixed(event.newValue)
		// Case: Significant change in progress => Animates the change
		else
			AnimatorInstruction(
				Animation.progress(event.oldValue, event.newValue).projectileCurved.over(animationDuration))
	}
	private val progressAnimator = new Animator(animatorInstructionPointer, activeFlag = parentHierarchy.linkPointer)
	
	// Contains maximum radius for knob & hover effect
	private val maxRadiusPointer = sizePointer.strongMap { _.minDimension / 2.0 }
	private val knobRadiusPointer = maxRadiusPointer.strongMap { (optimalKnobDiameter / 2.0) min _ }
	private val knobAreaPointer = progressPointer
		.lazyMergeWith(boundsPointer, knobRadiusPointer) { (progress, bounds, radius) =>
			val lineWidth = bounds.width - radius * 2
			val leftSideWidth = lineWidth * progress
			val centerX = bounds.leftX + radius + leftSideWidth
			val centerY = bounds.topY + bounds.height / 2.0
			
			Circle(Point(centerX, centerY), radius)
		}
	
	private val knobBaseColorPointer = knobColorFunction match {
		case Left(staticColor) => Fixed(staticColor)
		case Right(colorFunction) =>
			colorFunction match {
				case Left(progressToColor) => progressAnimator.map(progressToColor)
				case Right(valueToColor) => valuePointer.mapWhile(linkedFlag)(valueToColor)
			}
	}
	private val knobColorPointer = knobBaseColorPointer.mergeWith(statePointer) { (baseColor, state) =>
		val highlighted = baseColor.highlightedBy(state.intensity max 0.0)
		if (state.enabled) highlighted else highlighted.grayscale
	}
		
	
	// INITIAL CODE --------------------------
	
	// Updates the state pointer when one of its components gets updated
	_focusPointer.addListener(updateStateListener)
	hoverFlag.addListener(updateStateListener)
	activatedFlag.addListener(updateStateListener)
	enabledFlag.addListenerWhile(linkedFlag)(updateStateListener)
	
	// TODO: Set up stuff when linked (including state update)
	// TODO: Include drag event setup (needs refactoring in Reach component classes)
	
	// TODO: Set up focus listening
	
	
	// COMPUTED ------------------------------
	
	private def usesSticking = stickingPoints.nonEmpty
	
	private def progress = rawProgressPointer.value
	private def progress_=(progress: Double) = rawProgressPointer.value = progress
	
	private def visualProgress = progressAnimator.value
	
	private def stickingIndex = stickingIndexFor(progress)
	private def stickingIndex_=(newIndex: Int) = {
		if (usesSticking)
			progress = stickingPoints((newIndex max 0) min (stickingPoints.size - 1))
	}
	
	private def knobRadius = knobRadiusPointer.value
	private def knobArea = knobAreaPointer.value
	
	
	// IMPLEMENTED  --------------------------
	
	override def state = statePointer.value
	
	override def customDrawers: Seq[CustomDrawer] = ???
	
	override def allowsFocusEnter: Boolean = true
	override def allowsFocusLeave: Boolean = true
	
	override def updateLayout(): Unit = ()
	
	
	// OTHER    ------------------------------
	
	// Converts an X-coordinate (relative to this component's parent's left side) to a progress value
	private def xToProgress(x: Double) = {
		val minX = this.x + knobRadius
		
		if (x <= minX)
			0.0
		else {
			val maxX = this.rightX - knobRadius
			if (x >= maxX)
				1.0
			else {
				val rangeLength = maxX - minX
				(x - minX) / rangeLength
			}
		}
	}
	
	// "Sticks" the specified progress to the closest "sticking point", if applicable
	private def stick(progress: Double) = {
		stickingPoints.findIndexWhere { _ >= progress } match {
			case Some(nextIndex) =>
				val next = stickingPoints(nextIndex)
				if (nextIndex == 0)
					next
				else {
					val previous = stickingPoints(nextIndex - 1)
					Pair(previous, next).minBy { t => (t - progress).abs }
				}
			case None => stickingPoints.lastOption.getOrElse(progress)
		}
	}
	// Maps a progress value to a sticking point index
	// Returns -1 if sticking is not used
	private def stickingIndexFor(progress: Double) = {
		stickingPoints.findIndexWhere { _ >= progress } match {
			case Some(nextIndex) =>
				if (nextIndex == 0)
					nextIndex
				else {
					val previousIndex = nextIndex - 1
					Pair(previousIndex, nextIndex).minBy { i => (stickingPoints(i) - progress).abs }
				}
			case None => stickingPoints.size - 1
		}
	}
	
	
	// NESTED   ------------------------------
	
	// Used for drawing the bar and the knob, plus the hover effect
	private object Drawer extends CustomDrawer
	{
		override def opaque: Boolean = false
		override def drawLevel: DrawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			val maxRadius = maxRadiusPointer.value
			val knobRadius = Slider.this.knobRadius
			val lineStartX = bounds.leftX + knobRadius
			val lineWidth = bounds.width - knobRadius * 2
			val lineY = bounds.topY + bounds.height / 2.0
			val leftSideWidth = lineWidth * visualProgress
			val thresholdX = lineStartX + leftSideWidth
			
			// Uses anti-aliasing when drawing
			val d = drawer.antialiasing
			
			// Draws the right side line first
			val rightLineHeight = (optimalKnobDiameter / 5.0 / leftToRightBarHeightRatio) min (bounds.height * 0.8)
			DrawSettings.onlyFill(if (enabled) barSideColors.second else barSideColors.second.grayscale)
				.use { implicit ds =>
					d.draw(Bounds(Point(thresholdX, lineY - rightLineHeight / 2.0),
						Size(lineWidth - leftSideWidth, rightLineHeight)).toRoundedRectangle(1.0))
				}
			// Next draws the left line
			val leftLineHeight = (optimalKnobDiameter / 5.0 * leftToRightBarHeightRatio) min (bounds.height * 0.8)
			DrawSettings.onlyFill(if (enabled) barSideColors.first else barSideColors.first.grayscale)
				.use { implicit ds =>
					d.draw(Bounds(Point(lineStartX, lineY - leftLineHeight / 2.0), Size(leftSideWidth, leftLineHeight)))
				}
			
			// Next draws the hover effect, if appropriate
			val knobCenter = Point(thresholdX, lineY)
			val hoverAlpha = state.hoverAlpha
			if (hoverAlpha > 0) {
				val hoverRadius = (knobRadius + Slider.this.hoverRadius) min maxRadius
				if (hoverRadius > knobRadius + 1)
					DrawSettings.onlyFill(hoverColor.withAlpha(hoverAlpha))
						.use { implicit ds => d.draw(Circle(knobCenter, hoverRadius)) }
			}
			
			// Finally draws the knob
			DrawSettings.onlyFill(knobColorPointer.value).use { implicit ds => d.draw(Circle(knobCenter, knobRadius)) }
		}
	}
	
	private object MainMouseListener extends MouseButtonStateListener with MouseMoveListener
	{
		// ATTRIBUTES   ------------------------
		
		// Is interested in left mouse button presses inside this component's area
		override val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] =
			MouseButtonStateEvent.filter.leftPressed.over(bounds)
		
		
		// IMPLEMENTED  ------------------------
		
		override def handleCondition: FlagLike = enabledFlag
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = {
			// Case: Knob clicked => Enters dragging mode
			if (knobArea.contains(event.position)) {
				draggingFlag.set()
				Consume("Slider drag started")
			}
			// Case: Area outside the knob clicked => Adjusts progress to match the clicked value
			else
				progress = xToProgress(event.position.x)
		}
		
		// Updates hover state on mouse move
		override def onMouseMove(event: MouseMoveEvent): Unit = hoverFlag.value = knobArea.contains(event.position)
	}
	
	private object DragListener extends MouseDragListener
	{
		override def handleCondition: FlagLike = draggingFlag
		override def mouseDragEventFilter: Filter[MouseDragEvent] = AcceptAll
		
		override def onMouseDrag(event: MouseDragEvent): Unit = {
			// Updates progress
			progress = xToProgress(event.position.x)
			
			// May end drag
			if (event.isDragEnd)
				draggingFlag.reset()
		}
	}
	
	private object PressedTracker extends KeyStateListener
	{
		// ATTRIBUTES   ------------------------
		
		override val handleCondition: FlagLike = enabledAndFocusedFlag || keyDownFlag
		
		
		// IMPLEMENTED  ------------------------
		
		override def keyStateEventFilter: KeyStateEventFilter = AcceptAll
		
		override def onKeyState(event: KeyStateEvent): Unit = event.arrowAlong(X).foreach { direction =>
			// Case: Arrow key pressed => Makes a record of it
			if (event.pressed)
				keyDownPointer.value = Some(progressPointer.value -> direction.sign)
			// Case: Arrow key released => Ends key-down, if applicable
			else
				keyDownPointer
					.mutate { d =>
						val sameDirection = d.filter { _._2 == direction.sign }
						if (sameDirection.isDefined)
							sameDirection -> None
						else
							None -> d
					}
					.foreach { case (initialProgress, sign) =>
						// May advance the progress, unless the key was held down for a while already
						if (usesSticking) {
							val initialStickingIndex = stickingIndexFor(initialProgress)
							if (stickingIndex == initialStickingIndex)
								stickingIndex = initialStickingIndex + sign.modifier
						}
						else {
							val moveTarget = initialProgress + sign * progressWithArrow
							if ((moveTarget - initialProgress).abs > (progress - initialProgress).abs)
								progress = (moveTarget max 0.0) min 1.0
						}
					}
		}
	}
	
	private object ArrowKeyProgressUpdater extends KeyDownListener
	{
		override def handleCondition: FlagLike = keyDownFlag
		override def keyDownEventFilter: KeyDownEventFilter = AcceptAll
		
		override def whileKeyDown(event: KeyDownEvent): Unit = {
			keyDownPointer.value.foreach { case (_, direction) =>
				rawProgressPointer.update { _ + direction * progressVelocityWithArrow.over(event.duration) }
			}
		}
	}
}
