package utopia.reflection.component.swing.input

import java.awt.event.{FocusEvent, FocusListener, KeyEvent}

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener, Changing}
import utopia.genesis.animation.Animation
import utopia.genesis.color.Color
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButtonStateEvent, MouseEvent, MouseMoveEvent}
import utopia.genesis.handling.{Actor, KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.path.{ProjectilePath, SegmentedPath}
import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point, Size}
import utopia.genesis.util.Drawer
import utopia.genesis.view.GlobalMouseEventHandler
import utopia.inception.handling.HandlerType
import utopia.reflection.component.context.{AnimationContextLike, BaseContextLike}
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.swing.button.ButtonState
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.component.template.Focusable
import utopia.reflection.component.template.input.InputWithPointer
import utopia.reflection.component.template.layout.stack.StackLeaf
import utopia.reflection.shape.stack.StackLength
import utopia.reflection.util.ComponentCreationDefaults

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
	def contextual[A](range: Animation[A], targetWidth: StackLength, leftColor: Color, rightColor: Color,
	                  knobColor: Animation[Color], colorVariationIntensity: Double = 1.0,
	                  stickyPoints: Seq[Double] = Vector(), arrowMovement: Double = 0.1,
	                  leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0, initialValue: Double = 0.0)
	                 (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
	def contextualSingleColor[A](range: Animation[A], targetWidth: StackLength, color: Color,
	                             colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                             arrowMovement: Double = 0.1, initialValue: Double = 0.0)
	                            (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
	def contextualDualColor[A](range: Animation[A], targetWidth: StackLength, lessColor: Color, moreColor: Color,
	                           colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                           arrowMovement: Double = 0.1, initialValue: Double = 0.0)
	                          (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
	def contextualSingleColorKnot[A](range: Animation[A], targetWidth: StackLength, color: Color,
	                                 colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                                 arrowMovement: Double = 0.1, initialValue: Double = 0.0)
	                                (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
	                           rightColor: Color, knobColor: Animation[Color], colorVariationIntensity: Double = 1.0,
	                           leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0)
	                          (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
	                                     (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
	                          (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
	                                         (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
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
class Slider[A](range: Animation[A], targetKnobDiameter: Double, targetWidth: StackLength,
                leftColor: Color, rightColor: Color, knobColor: Animation[Color], colorVariationIntensity: Double = 1.0,
                stickyPoints: Seq[Double] = Vector(), arrowMovement: Double = 0.1,
                leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0, initialValue: Double = 0.0)
	extends AwtComponentWrapperWrapper with StackLeaf with Focusable with CustomDrawableWrapper
		with InputWithPointer[A, Changing[A]]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val stackId = hashCode()
	override val stackSize = targetWidth x StackLength(targetKnobDiameter * 0.2, targetKnobDiameter,
		targetKnobDiameter * 3)
	
	private val label = new EmptyLabel()
	
	private val progressPointer = new PointerWithEvents[Double](initialValue)
	val valuePointer =
	{
		if (stickyPoints.nonEmpty)
			progressPointer.map { p => range(stickyPointClosestTo(p)) }
		else
			progressPointer.map { range(_) }
	}
	
	private val defaultRepainter: ChangeListener[Any] = _ => repaint()
	private var animator: Option[Animator] = None
	
	private var _state = ButtonState.default
	
	
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
		if (isAttached)
			GlobalMouseEventHandler += GlobalMouseDragListener
		else
			GlobalMouseEventHandler -= GlobalMouseDragListener
	}
	
	addKeyStateListener(KeyPressListener)
	
	
	// COMPUTED -----------------------------
	
	def doubleValue = animator match
	{
		case Some(animator) => animator.calculatedProgress
		case None => progressPointer.value
	}
	
	def state = _state
	private def state_=(newState: ButtonState) =
	{
		if (_state != newState)
		{
			_state = newState
			repaint()
		}
	}
	
	def enabled = state.isEnabled
	def enabled_=(newState: Boolean) =
	{
		state = state.copy(isEnabled = newState)
		if (newState)
			setHandCursor()
		else
			setArrowCursor()
	}
	
	def pressed = state.isPressed
	private def pressed_=(newStatus: Boolean) = state = state.copy(isPressed = newStatus)
	def notPressed = !pressed
	
	private def colorChangeIterations =
	{
		val base = if (pressed) 2 else if (state.isMouseOver) 1 else 0
		if (state.isInFocus) base + 1 else base
	}
	
	private def currentKnobColor =
	{
		val base = knobColor(doubleValue)
		val changes = colorChangeIterations
		val changed =
		{
			if (changes > 0)
			{
				if (base.luminosity < 0.6)
					Iterator.iterate(base) { _.lightened(1 + 0.4 * colorVariationIntensity) }
						.drop(changes).next()
				else
					Iterator.iterate(base) { _.darkened(1 + 0.4 * colorVariationIntensity) }
						.drop(changes).next()
			}
			else
				base
		}
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
	def enableAnimations(actorHandler: ActorHandler, curvature: Animation[Double] = ProjectilePath(),
	                     animationDuration: Duration = ComponentCreationDefaults.transitionDuration) =
	{
		if (animator.isEmpty)
		{
			// the new animator will take care of the repaint calls so previously installed listener is
			// no longer required
			progressPointer.removeListener(defaultRepainter)
			val newAnimator = new Animator(curvature, animationDuration)
			progressPointer.addListener(newAnimator)
			animator = Some(newAnimator)
			
			// Animations are active only while this slider is attached to the main stack hierarchy
			addStackHierarchyChangeListener(isAttached =>
			{
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
	def enableAnimations()(implicit context: AnimationContextLike): Unit =
		enableAnimations(context.actorHandler, animationDuration = context.animationDuration)
	
	// Progress is based on mouse x-coordinate, but limited between [0, 1]
	private def progressForX(x: Double) = ((x / width) max 0.0) min 1.0
	
	private def stickyPointClosestTo(progress: Double) = stickyPoints.minBy { sp => (progress - sp).abs }
	
	private def nextStickyPointInDirection(direction: Direction1D) =
	{
		val p = progressPointer.value
		direction match
		{
			case Positive => stickyPoints.find { _ > p }.getOrElse(1.0)
			case Negative => stickyPoints.findLast { _ < p }.getOrElse(0.0)
		}
	}
	
	
	// NESTED   -----------------------------
	
	private class Animator(curve: Animation[Double], animationDuration: Duration) extends Actor with ChangeListener[Double]
	{
		// ATTRIBUTES   ---------------------
		
		private var passedDuration = animationDuration
		private var startProgress = progressPointer.value
		private var targetProgress = startProgress
		private var isMoving = false
		
		
		// COMPUTED -------------------------
		
		def calculatedProgress = if (isMoving) startProgress +
			curve(passedDuration / animationDuration) * (targetProgress - startProgress) else targetProgress
		
		
		// IMPLEMENTED  ---------------------
		
		override def act(duration: FiniteDuration) =
		{
			passedDuration = (passedDuration + duration) min animationDuration
			if (passedDuration >= animationDuration)
				isMoving = false
			repaint()
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = isMoving
		
		override def onChangeEvent(event: ChangeEvent[Double]) =
		{
			if (pressed)
			{
				targetProgress = event.newValue
				if (!isMoving)
					repaint()
			}
			else
			{
				startProgress = calculatedProgress
				targetProgress = event.newValue
				passedDuration = Duration.Zero
				isMoving = true
			}
		}
	}
	
	private object Visualizer extends CustomDrawer
	{
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			val lineY = bounds.y + bounds.height / 2.0
			val leftSideWidth = bounds.width * doubleValue
			val thresholdX = bounds.x + leftSideWidth
			// Draws the right side line first
			val rightLineHeight = (targetKnobDiameter / 5.0 * rightHeightModifier) min (bounds.height * 0.8)
			drawer.onlyFill(if (enabled) rightColor else rightColor.grayscale)
				.draw(Bounds(Point(thresholdX, lineY - rightLineHeight / 2.0),
					Size(bounds.width - leftSideWidth, rightLineHeight)).toRoundedRectangle(1.0))
			// Next draws the left line
			val leftLineHeight = (targetKnobDiameter / 5.0 * leftHeightModifier) min (bounds.height * 0.8)
			drawer.onlyFill(if (enabled) leftColor else leftColor.grayscale)
				.draw(Bounds(Point(bounds.x, lineY - leftLineHeight / 2.0), Size(leftSideWidth, leftLineHeight)))
			// Finally draws the knob
			drawer.onlyFill(currentKnobColor).draw(Circle(Point(thresholdX, lineY), (targetKnobDiameter min bounds.height) / 2.0))
		}
	}
	
	private object MousePressListener extends MouseButtonStateListener
	{
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter &&
			MouseEvent.isOverAreaFilter(bounds)
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			progressPointer.value = progressForX(event.mousePosition.x - x)
			pressed = true
			if (!isInFocus)
				requestFocusInWindow()
			Some(ConsumeEvent("Slider grabbed"))
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = notPressed && enabled
	}
	
	private object MouseOverListener extends MouseMoveListener
	{
		override def onMouseMove(event: MouseMoveEvent) =
		{
			val b = bounds
			if (event.enteredArea(b))
				state = state.copy(isMouseOver = true)
			else if (event.exitedArea(b))
				state = state.copy(isMouseOver = false)
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled
	}
	
	private object GlobalMouseDragListener extends MouseButtonStateListener with MouseMoveListener
	{
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.wasReleasedFilter &&
			MouseButtonStateEvent.leftButtonFilter
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			pressed = false
			// Slides to the closest sticky point, if there is one
			if (stickyPoints.nonEmpty)
				progressPointer.value = stickyPointClosestTo(progressForX(
					event.absoluteMousePosition.x - absolutePosition.x))
			Some(ConsumeEvent("Slider grab released"))
		}
		
		override def onMouseMove(event: MouseMoveEvent) = progressPointer.value = progressForX(
			event.absoluteMousePosition.x - absolutePosition.x)
		
		override def allowsHandlingFrom(handlerType: HandlerType) = pressed
	}
	
	private object KeyPressListener extends KeyStateListener
	{
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter &&
			KeyStateEvent.keysFilter(KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT)
		
		override def onKeyState(event: KeyStateEvent) =
		{
			val direction = if (event.index == KeyEvent.VK_RIGHT) Positive else Negative
			if (stickyPoints.nonEmpty)
				progressPointer.value = nextStickyPointInDirection(direction)
			else
				progressPointer.value = ((progressPointer.value + arrowMovement * direction.modifier) max 0.0) min 1.0
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = isInFocus && enabled
	}
	
	private object ComponentFocusListener extends FocusListener
	{
		override def focusGained(e: FocusEvent) = state = state.copy(isInFocus = true)
		
		override def focusLost(e: FocusEvent) = state = state.copy(isInFocus = false)
	}
}
