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
import utopia.reflection.shape.LengthExtensions._
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.{Duration, FiniteDuration}

object Slider
{
	def contextual[A](range: Animation[A], targetWidth: StackLength, leftColor: Color, rightColor: Color,
	                  knobColor: Animation[Color], colorVariationIntensity: Double = 1.0,
	                  stickyPoints: Seq[Double] = Vector(), defaultArrowMovement: Double = 0.1,
	                  leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0, initialValue: Double = 0.0)
	                 (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
	{
		val slider = new Slider(range, context.margins.medium, targetWidth, leftColor, rightColor, knobColor,
			colorVariationIntensity, stickyPoints, defaultArrowMovement, leftHeightModifier, rightHeightModifier,
			initialValue)
		slider.enableAnimations()
		slider
	}
	
	def contextualSingleColor[A](range: Animation[A], targetWidth: StackLength, color: Color,
	                             colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                             defaultArrowMovement: Double, initialValue: Double = 0.0)
	                            (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
		contextual(range, targetWidth, color, color.timesAlpha(0.38), Animation.fixed(color),
			colorVariationIntensity, stickyPoints, defaultArrowMovement, 1.5, initialValue = initialValue)
	
	def contextualDualColor[A](range: Animation[A], targetWidth: StackLength, lessColor: Color, moreColor: Color,
	                           colorVariationIntensity: Double = 1.0, stickyPoints: Seq[Double] = Vector(),
	                           defaultArrowMovement: Double = 0.1, initialValue: Double = 0.0)
	                          (implicit context: BaseContextLike, animationContext: AnimationContextLike) =
		contextual(range, targetWidth, moreColor, lessColor, Animation { p =>
			if (p == 1.0) moreColor else if (p == 0.0) lessColor else moreColor.average(lessColor, p, 1 - p) },
			colorVariationIntensity, stickyPoints, defaultArrowMovement, 1.25, 1.25,
			initialValue = initialValue)
	
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
}

/**
  * A slider component for selecting from a range of values
  * @author Mikko Hilpinen
  * @since 16.9.2020, v1.3
  */
class Slider[A](range: Animation[A], targetKnobDiameter: Double, targetWidth: StackLength,
                leftColor: Color, rightColor: Color, knobColor: Animation[Color], colorVariationIntensity: Double = 1.0,
                stickyPoints: Seq[Double] = Vector(), defaultArrowMovement: Double = 0.1,
                leftHeightModifier: Double = 1.0, rightHeightModifier: Double = 1.0, initialValue: Double = 0.0)
	extends AwtComponentWrapperWrapper with StackLeaf with Focusable with CustomDrawableWrapper
		with InputWithPointer[A, Changing[A]]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val stackId = hashCode()
	override val stackSize = targetWidth x targetKnobDiameter.any
	
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
	
	private def currentKnobBounds =
	{
		val b = bounds
		Circle(Point(b.x + b.width * doubleValue, b.y + b.height / 2.0), (targetKnobDiameter min bounds.height) / 2.0)
	}
	
	private def colorChangeIterations =
	{
		if (pressed)
			3
		else if (state.isMouseOver)
			2
		else if (state.isInFocus)
			1
		else
			0
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
					Iterator.iterate(base) { _.lightened(0.33 * colorVariationIntensity) }
						.drop(changes - 1).next()
				else
					Iterator.iterate(base) { _.darkened(0.33 * colorVariationIntensity) }
						.drop(changes - 1).next()
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
			animator = Some(newAnimator)
			actorHandler += newAnimator
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
				targetProgress = event.newValue
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
			drawer.onlyFill(currentKnobColor).draw(currentKnobBounds)
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
			Some(ConsumeEvent("Slider grabbed"))
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = notPressed
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
			if (stickyPoints.nonEmpty)
				progressPointer.value = nextStickyPointInDirection(
					if (event.index == KeyEvent.VK_RIGHT) Positive else Negative)
			else
				progressPointer.value = ((progressPointer.value + defaultArrowMovement) max 0.0) min 1.0
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = isInFocus
	}
	
	private object ComponentFocusListener extends FocusListener
	{
		override def focusGained(e: FocusEvent) = state = state.copy(isInFocus = true)
		
		override def focusLost(e: FocusEvent) = state = state.copy(isInFocus = false)
	}
}
