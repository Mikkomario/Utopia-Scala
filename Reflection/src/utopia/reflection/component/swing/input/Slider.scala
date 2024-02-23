package utopia.reflection.component.swing.input

import utopia.firmament.component.input.InputWithPointer
import utopia.firmament.context.{AnimationContext, BaseContext, ComponentCreationDefaults}
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.{Activated, Disabled, Focused, Hover}
import utopia.firmament.model.stack.StackLength
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.view.mutable.eventful.{EventfulPointer, ResettableFlag}
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.handling.event.consume.ConsumeChoice.Consume
import utopia.genesis.handling.event.keyboard.Key.{LeftArrow, RightArrow}
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{CommonMouseEvents, MouseButtonStateEvent, MouseButtonStateListener, MouseEvent, MouseMoveEvent, MouseMoveListener}
import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.AnimationLike.AnyAnimation
import utopia.paradigm.color.Color
import utopia.paradigm.path.{ProjectilePath, SegmentedPath}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.component.template.Focusable
import utopia.reflection.component.template.layout.stack.ReflectionStackLeaf

import java.awt.event.{FocusEvent, FocusListener, KeyEvent}
import scala.concurrent.duration.{Duration, FiniteDuration}

object Slider
{
	/**
	  * Creates a new slider using contextual information
	  * @param range Selectable values as an animation
	  * @param targetWidth Stack length used as a width for this slider
	  * @param leftColor Color used for the left side slider bar
	  * @param rightColor Color used for the right side slider bar
	  * @param knobColor Color used for the knot, animated based on progress
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param stickyPoints Progress points where the slider "sticks" to (all between [0, 1]) (default = empty)
	  * @param arrowMovement How much slider is progressed with each arrow key press ]0, 1] (default = 0.1)
	  * @param leftHeightModifier A modifier applied to left bar height (default = 1.0)
	  * @param rightHeightModifier A modifier applied to right bar height (default = 1.0)
	  * @param initialValue Value assigned to this slider initially [0, 1] (default = 0.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  */
	def contextual[A](range: AnyAnimation[A], targetWidth: StackLength, leftColor: Color, rightColor: Color,
	                  knobColor: AnyAnimation[Color], colorVariationIntensity: Double = 1.0,
	                  stickyPoints: Seq[Double] = Vector(), arrowMovement: Double = 0.1,
	                  leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0, initialValue: Double = 0.0)
	                 (implicit context: BaseContext, animationContext: AnimationContext) =
	{
		val slider = new Slider(range, context.margins.large, targetWidth, leftColor, rightColor, knobColor,
			colorVariationIntensity, stickyPoints, arrowMovement, leftHeightModifier, rightHeightModifier,
			initialValue)
		slider.enableAnimations()
		slider
	}
	
	/**
	  * Creates a new slider using contextual information. This slider only uses a single color.
	  * The knob and the left side are highlighted.
	  * @param range Selectable values as an animation
	  * @param targetWidth Stack length used as a width for this slider
	  * @param color Color used for the knot and the left side
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param stickyPoints Progress points where the slider "sticks" to (all between [0, 1]) (default = empty)
	  * @param arrowMovement How much slider is progressed with each arrow key press ]0, 1] (default = 0.1)
	  * @param initialValue Value assigned to this slider initially [0, 1] (default = 0.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  */
	def contextualSingleColor[A](range: AnyAnimation[A], targetWidth: StackLength, color: Color,
	                             colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                             arrowMovement: Double = 0.1, initialValue: Double = 0.0)
	                            (implicit context: BaseContext, animationContext: AnimationContext) =
		contextual(range, targetWidth, color, color.timesAlpha(0.38), Animation.fixed(color),
			colorVariationIntensity, stickyPoints, arrowMovement, 1.5, initialValue = initialValue)
	
	/**
	  * Creates a new slider using contextual information. This slider mixes between two colors.
	  * @param range Selectable values as an animation
	  * @param targetWidth Stack length used as a width for this slider
	  * @param lessColor Color representing smaller values
	  * @param moreColor Color representing larger values
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param stickyPoints Progress points where the slider "sticks" to (all between [0, 1]) (default = empty)
	  * @param arrowMovement How much slider is progressed with each arrow key press ]0, 1] (default = 0.1)
	  * @param initialValue Value assigned to this slider initially [0, 1] (default = 0.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  */
	def contextualDualColor[A](range: AnyAnimation[A], targetWidth: StackLength, lessColor: Color, moreColor: Color,
	                           colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                           arrowMovement: Double = 0.1, initialValue: Double = 0.0)
	                          (implicit context: BaseContext, animationContext: AnimationContext) =
		contextual(range, targetWidth, moreColor, lessColor, dualColorAnimation(lessColor, moreColor),
			colorVariationIntensity, stickyPoints, arrowMovement, 1.25, 1.25,
			initialValue = initialValue)
	
	/**
	  * Creates a new slider using contextual information. This slider only highlights the knob, not parts of the bar.
	  * @param range Selectable values as an animation
	  * @param targetWidth Stack length used as a width for this slider
	  * @param color Color used for the knot
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param stickyPoints Progress points where the slider "sticks" to (all between [0, 1]) (default = empty)
	  * @param arrowMovement How much slider is progressed with each arrow key press ]0, 1] (default = 0.1)
	  * @param initialValue Value assigned to this slider initially [0, 1] (default = 0.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  */
	def contextualSingleColorKnot[A](range: AnyAnimation[A], targetWidth: StackLength, color: Color,
	                                 colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                                 arrowMovement: Double = 0.1, initialValue: Double = 0.0)
	                                (implicit context: BaseContext, animationContext: AnimationContext) =
	{
		val backgroundColor = color.timesAlpha(0.38)
		contextual(range, targetWidth, backgroundColor, backgroundColor, Animation.fixed(color),
			colorVariationIntensity, stickyPoints, arrowMovement, initialValue = initialValue)
	}
	
	/**
	  * Creates a new slider using contextual information. This slider contains pre-existing options for values.
	  * @param options Selectable options for this slider
	  * @param targetWidth Stack length used as a width for this slider
	  * @param leftColor Color used for the left side slider bar
	  * @param rightColor Color used for the right side slider bar
	  * @param knobColor Color used for the knot, animated based on progress
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param leftHeightModifier A modifier applied to left bar height (default = 1.0)
	  * @param rightHeightModifier A modifier applied to right bar height (default = 1.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  * @throws IllegalArgumentException if length of options is smaller than 2
	  */
	@throws[IllegalArgumentException]
	def contextualSelection[A](options: Seq[A], targetWidth: StackLength, leftColor: Color,
	                           rightColor: Color, knobColor: AnyAnimation[Color], colorVariationIntensity: Double = 1.0,
	                           leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0)
	                          (implicit context: BaseContext, animationContext: AnimationContext) =
	{
		val range = SegmentedPath(options)
		contextual(range, targetWidth, leftColor, rightColor, knobColor, colorVariationIntensity,
			range.centerProgressPoints, leftHeightModifier = leftHeightModifier,
			rightHeightModifier = rightHeightModifier)
	}
	
	/**
	  * Creates a new slider using contextual information. This slider contains pre-existing options for values.
	  * This slider highlights the knob and the left side bar with the same color.
	  * @param options Selectable options for this slider
	  * @param targetWidth Stack length used as a width for this slider
	  * @param color Color to use on the knot and on the left side bar
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  * @throws IllegalArgumentException if length of options is smaller than 2
	  */
	@throws[IllegalArgumentException]
	def contextualSingleColorSelection[A](options: Seq[A], targetWidth: StackLength, color: Color,
	                                      colorVariationIntensity: Double = 1.0)
	                                     (implicit context: BaseContext, animationContext: AnimationContext) =
		contextualSelection(options, targetWidth, color, color.timesAlpha(0.38), Animation.fixed(color),
			colorVariationIntensity, 1.5)
	
	/**
	  * Creates a new slider using contextual information. This slider contains pre-existing options for values.
	  * This slider mixes between two colors.
	  * @param options Selectable options for this slider
	  * @param targetWidth Stack length used as a width for this slider
	  * @param lessColor Color representing smaller values
	  * @param moreColor Color representing larger values
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  * @throws IllegalArgumentException if length of options is smaller than 2
	  */
	@throws[IllegalArgumentException]
	def contextualDualColorSelection[A](options: Seq[A], targetWidth: StackLength, lessColor: Color,
	                           moreColor: Color, colorVariationIntensity: Double = 1.0)
	                          (implicit context: BaseContext, animationContext: AnimationContext) =
		contextualSelection(options, targetWidth, moreColor, lessColor, dualColorAnimation(lessColor, moreColor),
			colorVariationIntensity, 1.25, 1.25)
	
	/**
	  * Creates a new slider using contextual information. This slider contains pre-existing options for values.
	  * This slider only highlights the knob and not either bar side.
	  * @param options Selectable options for this slider
	  * @param targetWidth Stack length used as a width for this slider
	  * @param color Color to use when drawing the knot
	  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
	  * @param context Component creation context (implicit)
	  * @param animationContext Component animation context (implicit)
	  * @tparam A Type of selected value
	  * @return A new slider
	  * @throws IllegalArgumentException if length of options is smaller than 2
	  */
	@throws[IllegalArgumentException]
	def contextualSingleColorKnotSelection[A](options: Seq[A], targetWidth: StackLength, color: Color,
	                                          colorVariationIntensity: Double = 1.0)
	                                         (implicit context: BaseContext, animationContext: AnimationContext) =
	{
		val backgroundColor = color.timesAlpha(0.38)
		contextualSelection(options, targetWidth, backgroundColor, backgroundColor, Animation.fixed(color),
			colorVariationIntensity)
	}
	
	private def dualColorAnimation(lessColor: Color, moreColor: Color) = Animation { p =>
		if (p == 1.0) moreColor else if (p == 0.0) lessColor else moreColor.average(lessColor, p, 1 - p) }
}

/**
  * A slider component for selecting from a range of values
  * @author Mikko Hilpinen
  * @since 16.9.2020, v1.3
  * @param range Selectable values as an animation
  * @param targetKnobDiameter The diameter (2 * r) to use when drawing the knob
  * @param targetWidth Stack length used as a width for this slider
  * @param leftColor Color used for the left side slider bar
  * @param rightColor Color used for the right side slider bar
  * @param knobColor Color used for the knot, animated based on progress
  * @param colorVariationIntensity A modifier applied to color lightness changes (default = 1.0)
  * @param stickyPoints Progress points where the slider "sticks" to (all between [0, 1]) (default = empty)
  * @param arrowMovement How much slider is progressed with each arrow key press ]0, 1] (default = 0.1)
  * @param leftHeightModifier A modifier applied to left bar height (default = 1.0)
  * @param rightHeightModifier A modifier applied to right bar height (default = 1.0)
  * @param initialValue Value assigned to this slider initially [0, 1] (default = 0.0)
  */
class Slider[+A](range: AnyAnimation[A], targetKnobDiameter: Double, targetWidth: StackLength,
                leftColor: Color, rightColor: Color, knobColor: AnyAnimation[Color], colorVariationIntensity: Double = 1.0,
                stickyPoints: Seq[Double] = Vector(), arrowMovement: Double = 0.1,
                leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0, initialValue: Double = 0.0)
	extends AwtComponentWrapperWrapper with ReflectionStackLeaf with Focusable with MutableCustomDrawableWrapper
		with InputWithPointer[A, Changing[A]]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val stackId = hashCode()
	override val stackSize = targetWidth x StackLength(targetKnobDiameter * 0.2, targetKnobDiameter,
		targetKnobDiameter * 3)
	
	private val label = new EmptyLabel()
	
	private val progressPointer = EventfulPointer[Double](initialValue)
	val valuePointer = {
		if (stickyPoints.nonEmpty)
			progressPointer.map { p => range(stickyPointClosestTo(p)) }
		else
			progressPointer.map { range(_) }
	}
	
	private val defaultRepainter = ChangeListener.continuousOnAnyChange { repaint() }
	private var animator: Option[Animator] = None
	
	private val statePointer = EventfulPointer(GuiElementStatus.identity)
	private val pressedPointer: FlagLike = statePointer.map { _ is Activated }
	private val notPressedPointer = !pressedPointer
	private val enabledPointer: FlagLike = statePointer.map { _ isNot Disabled }
	private val focusPointer: FlagLike = statePointer.map { _ is Focused }
	private val focusedAndEnabledPointer = focusPointer && enabledPointer
	
	
	// INITIAL CODE -------------------------
	
	setHandCursor()
	label.component.setFocusable(true)
	label.component.addFocusListener(ComponentFocusListener)
	
	addCustomDrawer(Visualizer)
	// Repaints on progress changes (this listener is removed if/when animations are enabled)
	progressPointer.addListener(defaultRepainter)
	
	// Adds mouse listening. Global listening is active only when this component is part of a stack component hierarchy
	addMouseButtonListener(MousePressListener)
	addMouseMoveListener(MouseOverListener)
	addStackHierarchyChangeListener { isAttached =>
		if (isAttached) {
			CommonMouseEvents += CommonMouseDragListener
			KeyboardEvents += KeyPressListener
		}
		else {
			CommonMouseEvents -= CommonMouseDragListener
			KeyboardEvents -= KeyPressListener
		}
	}
	
	
	// COMPUTED -----------------------------
	
	def doubleValue = animator match {
		case Some(animator) => animator.calculatedProgress
		case None => progressPointer.value
	}
	
	def state = statePointer.value
	private def state_=(newState: GuiElementStatus) = {
		val oldState = statePointer.getAndSet(newState)
		if (oldState!= newState)
			repaint()
	}
	
	def enabled = state isNot Disabled
	def enabled_=(newState: Boolean) = {
		if (newState) {
			state -= Disabled
			setHandCursor()
		}
		else {
			state += Disabled
			setArrowCursor()
		}
	}
	
	def pressed = state is Activated
	private def pressed_=(newStatus: Boolean) = {
		if (newStatus)
			state += Activated
		else
			state -= Activated
	}
	def notPressed = !pressed
	
	private def currentKnobColor = {
		val base = knobColor(doubleValue)
		val changes = state.intensity
		val changed = if (changes > 0) base.highlightedBy(changes) else base
		if (enabled)
			changed
		else
			changed.grayscale
	}
	
	
	// IMPLEMENTED  -------------------------
	
	override def drawable = label
	
	override def updateLayout() = ()
	
	override def resetCachedSize() = ()
	
	override protected def wrapped = label
	
	override def requestFocusInWindow() = component.requestFocusInWindow()
	
	
	// OTHER    -----------------------------
	
	/**
	  * Enables smooth animations in this component
	  * @param actorHandler Actor handler that will deliver the necessary action events
	  * @param curvature A function / animation used for applying curvature to animation progress
	  *                  (default = smooth animation finish)
	  * @param animationDuration Duration of the complete knob transition (default = global default value)
	  */
	def enableAnimations(actorHandler: ActorHandler, curvature: AnyAnimation[Double] = ProjectilePath(),
	                     animationDuration: Duration = ComponentCreationDefaults.transitionDuration) =
	{
		if (animator.isEmpty) {
			// the new animator will take care of the repaint calls so previously installed listener is
			// no longer required
			progressPointer.removeListener(defaultRepainter)
			val newAnimator = new Animator(curvature, animationDuration)
			progressPointer.addListener(newAnimator)
			animator = Some(newAnimator)
			
			// Animations are active only while this slider is attached to the main stack hierarchy
			addStackHierarchyChangeListener(isAttached => {
				if (isAttached)
					actorHandler += newAnimator
				else
					actorHandler -= newAnimator
			}, callIfAttached = true)
		}
	}
	
	/**
	  * Enables smooth animations in this component
	  * @param context Component animation context (implicit)
	  */
	def enableAnimations()(implicit context: AnimationContext): Unit =
		enableAnimations(context.actorHandler, animationDuration = context.animationDuration)
	
	/**
	  * Moves the slider towards the specified direction
	  * @param direction Direction to move towards
	  * @param amount How much this slider is moved [0, 1]. If using sticky points, this value is ignored
	  *               (default = same as with arrow keys)
	  */
	def stepTowards(direction: Sign, amount: Double = arrowMovement) =
	{
		if (stickyPoints.nonEmpty)
			progressPointer.value = nextStickyPointInDirection(direction)
		else
			progressPointer.value = ((progressPointer.value + arrowMovement * direction.modifier) max 0.0) min 1.0
	}
	/**
	  * Moves this slider's value to the right
	  * @param amount Amount to move (default = same as with arrow keys) (ignored if sticky keys are used)
	  */
	def stepRight(amount: Double = arrowMovement) = stepTowards(Positive, amount)
	/**
	  * Moves this slider's value to the right
	  * @param amount Amount to move (default = same as with arrow keys) (ignored if sticky keys are used)
	  */
	def stepLeft(amount: Double = arrowMovement) = stepTowards(Negative, amount)
	/**
	  * Jumps this slider's value to the specified point
	  * @param point A point on this slider [0, 1] to jump to
	  */
	def shiftTo(point: Double) =
	{
		if (stickyPoints.isEmpty)
			progressPointer.value = (point max 0.0) min 1.0
		else
			progressPointer.value = stickyPointClosestTo(point)
	}
	
	// Progress is based on mouse x-coordinate, but limited between [0, 1]
	private def progressForX(x: Double) = ((x / width) max 0.0) min 1.0
	
	private def stickyPointClosestTo(progress: Double) = stickyPoints.minBy { sp => (progress - sp).abs }
	
	private def nextStickyPointInDirection(direction: Sign) =
	{
		val p = progressPointer.value
		direction match
		{
			case Positive => stickyPoints.find { _ > p }.getOrElse(1.0)
			case Negative => stickyPoints.findLast { _ < p }.getOrElse(0.0)
		}
	}
	
	
	// NESTED   -----------------------------
	
	private class Animator(curve: AnyAnimation[Double], animationDuration: Duration)
		extends Actor with ChangeListener[Double]
	{
		// ATTRIBUTES   ---------------------
		
		private val movingFlag = ResettableFlag()
		override val handleCondition: FlagLike = movingFlag.view
		
		private var passedDuration = animationDuration
		private var startProgress = progressPointer.value
		private var targetProgress = startProgress
		
		
		// COMPUTED -------------------------
		
		private def moving = movingFlag.value
		
		def calculatedProgress = {
			if (moving) startProgress +
				curve(passedDuration / animationDuration) * (targetProgress - startProgress)
			else
				targetProgress
		}
		
		
		// IMPLEMENTED  ---------------------
		
		override def act(duration: FiniteDuration) = {
			passedDuration = (passedDuration + duration) min animationDuration
			if (passedDuration >= animationDuration)
				movingFlag.reset()
			repaint()
		}
		
		override def onChangeEvent(event: ChangeEvent[Double]) = {
			if (pressed) {
				targetProgress = event.newValue
				if (!moving)
					repaint()
			}
			else {
				startProgress = calculatedProgress
				targetProgress = event.newValue
				passedDuration = Duration.Zero
				movingFlag.set()
				Continue
			}
		}
	}
	
	private object Visualizer extends CustomDrawer
	{
		override def opaque = false
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			val knobRadius = ((targetKnobDiameter min bounds.height) min bounds.width) / 2.0
			val lineStartX = bounds.leftX + knobRadius
			val lineWidth = bounds.width - knobRadius * 2
			val lineY = bounds.topY + bounds.height / 2.0
			val leftSideWidth = lineWidth * doubleValue
			val thresholdX = lineStartX + leftSideWidth
			// Draws the right side line first
			val rightLineHeight = (targetKnobDiameter / 5.0 * rightHeightModifier) min (bounds.height * 0.8)
			val rightDs = DrawSettings.onlyFill(if (enabled) rightColor else rightColor.grayscale)
			drawer.draw(Bounds(Point(thresholdX, lineY - rightLineHeight / 2.0),
				Size(lineWidth - leftSideWidth, rightLineHeight)).toRoundedRectangle(1.0))(rightDs)
			// Next draws the left line
			val leftLineHeight = (targetKnobDiameter / 5.0 * leftHeightModifier) min (bounds.height * 0.8)
			val leftDs = DrawSettings.onlyFill(if (enabled) leftColor else leftColor.grayscale)
			drawer.draw(
				Bounds(Point(lineStartX, lineY - leftLineHeight / 2.0), Size(leftSideWidth, leftLineHeight)))(leftDs)
			// Finally draws the knob
			val knobDs = DrawSettings.onlyFill(currentKnobColor)
			drawer.draw(Circle(Point(thresholdX, lineY), knobRadius))(knobDs)
		}
	}
	
	private object MousePressListener extends MouseButtonStateListener
	{
		// ATTRIBUTES   -----------------------
		
		override val handleCondition: FlagLike = notPressedPointer && enabledPointer
		
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent.filter.leftPressed && MouseEvent.filter.over(bounds)
		
		
		// IMPLEMENTED  -----------------------
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			progressPointer.value = progressForX(event.position.x - x)
			pressed = true
			if (!isInFocus)
				requestFocusInWindow()
			Consume("Slider grabbed")
		}
	}
	
	private object MouseOverListener extends MouseMoveListener
	{
		override def handleCondition: FlagLike = enabledPointer
		
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseMove(event: MouseMoveEvent) = {
			val b = bounds
			if (event.entered(b))
				state += Hover
			else if (event.exited(b))
				state -= Hover
		}
	}
	
	private object CommonMouseDragListener extends MouseButtonStateListener with MouseMoveListener
	{
		// ATTRIBUTES   -------------------
		
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.leftReleased
		
		
		// IMPLEMENTED  -------------------
		
		override def handleCondition: FlagLike = pressedPointer
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			pressed = false
			// Slides to the closest sticky point, if there is one
			if (stickyPoints.nonEmpty)
				progressPointer.value = stickyPointClosestTo(progressForX(event.position.absolute.x - absolutePosition.x))
			Consume("Slider grab released")
		}
		
		override def onMouseMove(event: MouseMoveEvent) =
			progressPointer.value = progressForX(event.position.absolute.x - absolutePosition.x)
	}
	
	private object KeyPressListener extends KeyStateListener
	{
		// ATTRIBUTES   --------------------
		
		override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter(RightArrow, LeftArrow)
		
		
		// IMPLEMENTED  --------------------
		
		override def handleCondition: FlagLike = focusedAndEnabledPointer
		
		override def onKeyState(event: KeyStateEvent) = {
			val direction = if (event.index == KeyEvent.VK_RIGHT) Positive else Negative
			if (stickyPoints.nonEmpty)
				progressPointer.value = nextStickyPointInDirection(direction)
			else
				progressPointer.value = ((progressPointer.value + arrowMovement * direction.modifier) max 0.0) min 1.0
		}
	}
	
	private object ComponentFocusListener extends FocusListener
	{
		override def focusGained(e: FocusEvent) = state += Focused
		override def focusLost(e: FocusEvent) = state -= Focused
	}
}
