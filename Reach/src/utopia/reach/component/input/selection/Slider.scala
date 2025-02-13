package utopia.reach.component.input.selection

import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.{Activated, Disabled, Focused, Hover}
import utopia.firmament.model.enumeration.{GuiElementState, SizeCategory}
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.{HasInclusiveEnds, NumericSpan}
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.operator.sign.Sign
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.eventful.{CopyOnDemand, EventfulPointer, IndirectPointer, ResettableFlag}
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.Priority.High
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.animation.{Animator, AnimatorInstruction}
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.KeyDownEvent.KeyDownEventFilter
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{KeyDownEvent, KeyDownListener, KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseDragEvent, MouseDragListener, MouseMoveEvent, MouseMoveListener}
import utopia.paradigm.animation.Animation
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.transform.Adjustment
import utopia.reach.component.factory.contextual.ColorContextualFactory
import utopia.reach.component.factory.{FocusListenableFactory, FromContextComponentFactoryFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.input.selection.Slider.SliderColors
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, HasGuiState, PartOfComponentHierarchy}
import utopia.reach.focus.FocusListener

import scala.concurrent.duration.Duration

/**
  * Common trait for slider factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
trait SliderSettingsLike[+Repr] extends FocusListenableFactory[Repr] with CustomDrawableFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * The primary color of this slider
	  */
	def colorRole: ColorRole
	/**
	  * General height of this slider
	  */
	def height: SizeCategory
	/**
	  * The ratio between the left side bar's height and the right side bar's height. If 1.0,
	  * both sides are the same height.
	  */
	def leftToRightBarHeightRatio: Double
	/**
	  * A pointer that determines whether this slider is interactive or not
	  */
	def enabledFlag: Changing[Boolean]
	/**
	  * The amount of time it takes for an individual transition animation to complete
	  */
	def animationDuration: Duration
	/**
	  * Maximum distance the knob may "jump" without an animated transition
	  */
	def maxJumpWithoutAnimation: Double
	/**
	  * Amount of progress [0,1] added to this slider when the left or the right arrow key is tapped
	  */
	def progressPerArrowPress: Double
	/**
	  * The rate at which progress is added to this slider while an arrow key is held down
	  */
	def progressWhileArrowDown: LinearVelocity
	/**
	  * A custom function which converts a progress value [0,1] to slider colors
	  */
	def colorFunction: Option[Double => SliderColors]
	
	/**
	  * The amount of time it takes for an individual transition animation to complete
	  * @param duration New animation duration to use.
	  * The amount of time it takes for an individual transition animation to complete
	  * @return Copy of this factory with the specified animation duration
	  */
	def withAnimationDuration(duration: Duration): Repr
	/**
	  * The primary color of this slider
	  * @param color New color to use.
	  * The primary color of this slider
	  * @return Copy of this factory with the specified color
	  */
	def withColorRole(color: ColorRole): Repr
	/**
	  * A custom function which converts a progress value [0,1] to slider colors
	  * @param f New color function to use.
	  * A custom function which converts a progress value [0,1] to slider colors
	  * @return Copy of this factory with the specified color function
	  */
	def withColorFunction(f: Option[Double => SliderColors]): Repr
	/**
	  * A pointer that determines whether this slider is interactive or not
	  * @param p New enabled pointer to use.
	  * A pointer that determines whether this slider is interactive or not
	  * @return Copy of this factory with the specified enabled pointer
	  */
	def withEnabledFlag(p: Changing[Boolean]): Repr
	/**
	  * General height of this slider
	  * @param height New height to use.
	  * General height of this slider
	  * @return Copy of this factory with the specified height
	  */
	def withHeight(height: SizeCategory): Repr
	/**
	  * The ratio between the left side bar's height and the right side bar's height. If 1.0,
	  * both sides are the same height.
	  * @param ratio New left to right bar height ratio to use.
	  * The ratio between the left side bar's height and the right side bar's height. If 1.0,
	  * both sides are the same height.
	  * @return Copy of this factory with the specified left to right bar height ratio
	  */
	def withLeftToRightBarHeightRatio(ratio: Double): Repr
	/**
	  * Maximum distance the knob may "jump" without an animated transition
	  * @param maxJump New max jump without animation to use.
	  * Maximum distance the knob may "jump" without an animated transition
	  * @return Copy of this factory with the specified max jump without animation
	  */
	def withMaxJumpWithoutAnimation(maxJump: Double): Repr
	/**
	  * Amount of progress [0,1] added to this slider when the left or the right arrow key is tapped
	  * @param progress New progress per arrow press to use.
	  * Amount of progress [0,1] added to this slider when the left or the right arrow key is tapped
	  * @return Copy of this factory with the specified progress per arrow press
	  */
	def withProgressPerArrowPress(progress: Double): Repr
	/**
	  * The rate at which progress is added to this slider while an arrow key is held down
	  * @param progressVelocity New progress while arrow down to use.
	  * The rate at which progress is added to this slider while an arrow key is held down
	  * @return Copy of this factory with the specified progress while arrow down
	  */
	def withProgressWhileArrowDown(progressVelocity: LinearVelocity): Repr
	
	
	// COMPUTED --------------------
	
	def higher = mapHeight { _.more }
	def lower = mapHeight { _.less }
	
	def withSlowerAnimations(implicit adj: Adjustment) = mapAnimationDuration { _ * adj(1) }
	def withFasterAnimations(implicit adj: Adjustment) = mapAnimationDuration { _ * adj(-1) }
	
	
	// OTHER	--------------------
	
	/**
	  * A custom function which converts a progress value [0,1] to slider colors
	  * @param f New color function to use.
	  *          A custom function which converts a progress value [0,1] to slider colors
	  * @return Copy of this factory with the specified color function
	  */
	def withColorFunction(f: Double => SliderColors): Repr = withColorFunction(Some(f))
	
	def mapAnimationDuration(f: Mutate[Duration]) = withAnimationDuration(f(animationDuration))
	def mapEnabledFlag(f: Mutate[Changing[Boolean]]) = withEnabledFlag(f(enabledFlag))
	def mapHeight(f: Mutate[SizeCategory]) = withHeight(f(height))
	def mapProgressPerArrowPress(f: Mutate[Double]) = withProgressPerArrowPress(f(progressPerArrowPress))
}

object SliderSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing sliders
  * @param focusListeners Focus listeners to assign to created components
  * @param customDrawers Custom drawers to assign to created components
  * @param colorRole The primary color of this slider
  * @param height General height of this slider
  * @param leftToRightBarHeightRatio The ratio between the left side bar's height and the right side bar's height.
  *                                  If 1.0, both sides are the same height.
  * @param enabledFlag A pointer that determines whether this slider is interactive or not
  * @param animationDuration The amount of time it takes for an individual transition animation to complete
  * @param maxJumpWithoutAnimation Maximum distance the knob may "jump" without an animated transition
  * @param progressPerArrowPress Amount of progress [0,1] added to this slider when the left or the right arrow key is tapped
  * @param progressWhileArrowDown The rate at which progress is added to this slider while an arrow key is held down
  * @param colorFunction A custom function which converts a progress value [0,1] to slider colors
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
case class SliderSettings(focusListeners: Seq[FocusListener] = Empty,
                          customDrawers: Seq[CustomDrawer] = Empty, colorRole: ColorRole = ColorRole.Secondary,
                          height: SizeCategory = SizeCategory.Medium, leftToRightBarHeightRatio: Double = 1.5,
                          enabledFlag: Changing[Boolean] = AlwaysTrue, animationDuration: Duration = 0.2.seconds,
                          maxJumpWithoutAnimation: Double = 2.0, progressPerArrowPress: Double = 0.2,
                          progressWhileArrowDown: LinearVelocity = LinearVelocity(1.0, 1.0.seconds),
                          colorFunction: Option[Double => SliderColors] = None)
	extends SliderSettingsLike[SliderSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withAnimationDuration(duration: Duration) = copy(animationDuration = duration)
	override def withColorRole(color: ColorRole) = copy(colorRole = color)
	override def withColorFunction(f: Option[Double => SliderColors]) = copy(colorFunction = f)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
	override def withEnabledFlag(p: Changing[Boolean]) = copy(enabledFlag = p)
	override def withFocusListeners(listeners: Seq[FocusListener]) = copy(focusListeners = listeners)
	override def withHeight(height: SizeCategory) = copy(height = height)
	override def withLeftToRightBarHeightRatio(ratio: Double) = copy(leftToRightBarHeightRatio = ratio)
	override def withMaxJumpWithoutAnimation(maxJump: Double) = copy(maxJumpWithoutAnimation = maxJump)
	override def withProgressPerArrowPress(progress: Double) = copy(progressPerArrowPress = progress)
	override def withProgressWhileArrowDown(progressVelocity: LinearVelocity) =
		copy(progressWhileArrowDown = progressVelocity)
}

/**
  * Common trait for factories that wrap a slider settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
trait SliderSettingsWrapper[+Repr] extends SliderSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: SliderSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: SliderSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def animationDuration = settings.animationDuration
	override def colorRole = settings.colorRole
	override def colorFunction = settings.colorFunction
	override def customDrawers = settings.customDrawers
	override def enabledFlag = settings.enabledFlag
	override def focusListeners = settings.focusListeners
	override def height = settings.height
	override def leftToRightBarHeightRatio = settings.leftToRightBarHeightRatio
	override def maxJumpWithoutAnimation = settings.maxJumpWithoutAnimation
	override def progressPerArrowPress = settings.progressPerArrowPress
	override def progressWhileArrowDown = settings.progressWhileArrowDown
	
	override def withAnimationDuration(duration: Duration) = mapSettings { _.withAnimationDuration(duration) }
	override def withColorRole(color: ColorRole) = mapSettings { _.withColorRole(color) }
	override def withColorFunction(f: Option[Double => SliderColors]) = mapSettings { _.withColorFunction(f) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = mapSettings { _.withCustomDrawers(drawers) }
	override def withEnabledFlag(p: Changing[Boolean]) = mapSettings { _.withEnabledFlag(p) }
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		mapSettings { _.withFocusListeners(listeners) }
	override def withHeight(height: SizeCategory) = mapSettings { _.withHeight(height) }
	override def withLeftToRightBarHeightRatio(ratio: Double) =
		mapSettings { _.withLeftToRightBarHeightRatio(ratio) }
	override def withMaxJumpWithoutAnimation(maxJump: Double) =
		mapSettings { _.withMaxJumpWithoutAnimation(maxJump) }
	override def withProgressPerArrowPress(progress: Double) =
		mapSettings { _.withProgressPerArrowPress(progress) }
	override def withProgressWhileArrowDown(progressVelocity: LinearVelocity) =
		mapSettings { _.withProgressWhileArrowDown(progressVelocity) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: SliderSettings => SliderSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing sliders using contextual component creation information
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
case class ContextualSliderFactory(hierarchy: ComponentHierarchy, context: StaticColorContext,
                                   settings: SliderSettings = SliderSettings.default,
                                   customColors: Option[SliderColors] = None)
	extends SliderSettingsWrapper[ContextualSliderFactory]
		with ColorContextualFactory[ContextualSliderFactory] with PartOfComponentHierarchy
{
	// COMPUTED ------------------------
	
	private def defaultColors = SliderColors(context.color(settings.colorRole))
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override def withContext(context: StaticColorContext) = copy(context = context)
	override def withSettings(settings: SliderSettings) = copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	def withCustomColor(colors: SliderColors) = copy(customColors = Some(colors))
	def withCustomColor(color: Color): ContextualSliderFactory = withCustomColor(SliderColors(color))
	
	def mapColor(f: Color => Color) =
		withCustomColor(customColors.getOrElse(defaultColors).map(f))
	
	/**
	  * Prepares a new slider factory for specific type of content
	  * @param progressToSelection Function for converting progress [0,1] to a value selection
	  * @param selectionToProgress Function for converting selected value to progress [0,1]
	  * @tparam A Type of selected values
	  * @return A new slider factory using the specified functions
	  */
	def apply[A](progressToSelection: Double => A)(selectionToProgress: A => Double) =
		new Prepared[A](progressToSelection, selectionToProgress)
	
	/**
	  * Prepares a slider that allows selection from the specified value options
	  * @param options The options that may be selected from
	  * @tparam A Type of the available options
	  * @return A slider factory for selecting from the specified options
	  */
	def from[A](options: Seq[A]) = new Prepared[A](
		p => options((p * (options.size - 1)).round.toInt),
		a => options.findIndexOf(a).getOrElse(0) / (options.size - 1),
		options.indices.map { _.toDouble / (options.size - 1) })
	/**
	  * Prepares a slider that allows selection from variable value options
	  * @param optionsPointer Pointer that contains the selectable options
	  * @tparam A Type of the available options
	  * @return A slider factory for selecting from the specified options
	  */
	def fromOptionsPointer[A](optionsPointer: Changing[Seq[A]]) = {
		val progressToValuePointer = optionsPointer
			.map { options => p: Double => options((p * (options.size - 1)).round.toInt) }
		val valueToProgressPointer = optionsPointer
			.map { options => a: A => options.findIndexOf(a).getOrElse(0) / (options.size - 1) }
		new Prepared[A](p => progressToValuePointer.value(p), a => valueToProgressPointer.value(a),
			finalizeFunction = Some(s => optionsPointer.addListenerWhile(s.linkedFlag) { _ => s.repaint() }))
	}
	
	/**
	  * @param range Selectable range of numbers
	  * @return A slider factory for constructing numeric sliders
	  */
	def forDoubles(range: HasInclusiveEnds[Double]) = {
		val len = range.end - range.start
		apply[Double] { _ * len + range.start } { v => (v - range.start) / len }
	}
	
	
	// NESTED   ------------------------
	
	class Prepared[A](progressToSelection: Double => A, selectionToProgress: A => Double,
	                  stickingPoints: Seq[Double] = Empty,
	                  customColorFunction: Option[A => SliderColors] = None,
	                  finalizeFunction: Option[Slider[A] => Unit] = None)
	{
		// OTHER    --------------------
		
		def withItemColorFunction(f: A => SliderColors) =
			new Prepared(progressToSelection, selectionToProgress, stickingPoints, Some(f))
		
		/**
		  * Creates a new slider
		  * @param width Stack width of this slider
		  * @param initialValue Initially selected value
		  * @return A new slider
		  */
		def apply(width: StackLength, initialValue: A) = {
			val knobRadius = context.margins(height)
			val colorFunction = customColorFunction match {
				case Some(itemToColor) => Right(Right(itemToColor))
				case None =>
					settings.colorFunction match {
						case Some(progressToColor) => Right(Left(progressToColor))
						case None => Left(customColors.getOrElse(defaultColors))
					}
			}
			val hoverRadius = knobRadius * 0.75
			new Slider[A](hierarchy, context.actorHandler, initialValue, width, knobRadius * 2,
				colorFunction, stickingPoints, progressPerArrowPress, progressWhileArrowDown, hoverRadius,
				context.color(settings.colorRole), leftToRightBarHeightRatio, animationDuration,
				maxJumpWithoutAnimation, enabledFlag, focusListeners,
				customDrawers)(progressToSelection)(selectionToProgress)
		}
	}
}

/**
  * Used for defining slider creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
case class SliderSetup(settings: SliderSettings = SliderSettings.default)
	extends SliderSettingsWrapper[SliderSetup]
		with FromContextComponentFactoryFactory[StaticColorContext, ContextualSliderFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: StaticColorContext) =
		ContextualSliderFactory(hierarchy, context, settings)
	
	override def withSettings(settings: SliderSettings) = copy(settings = settings)
}

object Slider extends SliderSetup()
{
	// OTHER	--------------------
	
	def apply(settings: SliderSettings) = withSettings(settings)
	
	
	// NESTED	--------------------
	
	object SliderColors
	{
		def apply(primaryColor: Color): SliderColors = apply(primaryColor, primaryColor.timesAlpha(0.5), primaryColor)
	}
	case class SliderColors(leftBar: Color, rightBar: Color, knob: Color)
	{
		def map(f: Color => Color) = SliderColors(f(leftBar), f(rightBar), f(knob))
	}
}

/**
  * A component used for choosing from a linear range of possible values
  * @author Mikko Hilpinen
  * @since 16.08.2024, v1.4
  */
// TODO: Refactor to accept settings. Also, utilize ProgressBarSettings. However, this requires support for variable color context.
class Slider[A](override val hierarchy: ComponentHierarchy, actorHandler: ActorHandler, initialValue: A,
                stackWidth: StackLength, optimalKnobDiameter: Double,
                colorFunction: Either[SliderColors, Either[Double => SliderColors, A => SliderColors]],
                stickingPoints: Seq[Double] = Empty, progressWithArrow: Double = 0.2,
                progressVelocityWithArrow: LinearVelocity = LinearVelocity(1.0, 1.seconds),
                hoverRadius: Double = 0.0, hoverColor: Color = Color.white, leftToRightBarHeightRatio: Double = 1.5,
                animationDuration: Duration = 0.2.seconds, maxJumpWithoutAnimationDistance: Double = 2.0,
                enabledFlag: Flag = AlwaysTrue, additionalFocusListeners: Seq[FocusListener] = Empty,
                additionalDrawers: Seq[CustomDrawer] = Empty)
               (progressToSelection: Double => A)(selectionToProgress: A => Double)
	extends ConcreteCustomDrawReachComponent with InteractionWithPointer[A] with HasGuiState with FocusableWithState
{
	// ATTRIBUTES   --------------------------
	
	override val focusId: Int = hashCode()
	
	override val calculatedStackSize: StackSize = {
		val knob = StackLength(optimalKnobDiameter * 0.5, optimalKnobDiameter, optimalKnobDiameter * 2)
		val knobPlusHover = knob + StackLength.downscaling(hoverRadius * 2)
		val width = stackWidth max (knob * 3) max knobPlusHover
		
		width x knobPlusHover
	}
	
	private val _focusPointer = ResettableFlag()
	override def focusPointer: Flag = _focusPointer.readOnly
	
	private val enabledAndFocusedFlag = _focusPointer && enabledFlag
	
	// Contains true if mouse is hovering over the knob
	private val hoverFlag = ResettableFlag()
	
	// Contains true while this slider is being dragged using the mouse
	private val draggingFlag = ResettableFlag()
	// Contains Some(initialProgress -> direction) while this slider is being adjusted with a keyboard key
	private val keyDownPointer = EventfulPointer.empty[(Double, Sign)]
	private val keyDownFlag: Flag = keyDownPointer.strongMap { _.isDefined }
	
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
	
	private val rawProgressPointer = EventfulPointer(selectionToProgress(initialValue))
	// This version of the progress pointer applies "sticking", if appropriate
	private val progressPointer =
		if (stickingPoints.isEmpty) rawProgressPointer else rawProgressPointer.strongMap(stick)
	
	override val valuePointer = IndirectPointer(progressPointer.map(progressToSelection)) { value =>
		rawProgressPointer.value = selectionToProgress(value)
	}
	
	// Used for directing the animator to visualize progress changes
	// TODO: Refactor to use ProgressAnimator
	private val animatorInstructionPointer = progressPointer.incrementalMap(AnimatorInstruction.fixed) { (_, event) =>
		// Case: The change in progress is so small that no animation is needed
		if (draggingFlag.isSet || keyDownFlag.isSet || event.values.merge { _ - _ }.abs < maxJumpProgressPointer.value)
			AnimatorInstruction.fixed(event.newValue)
		// Case: Significant change in progress => Animates the change
		else
			AnimatorInstruction(
				Animation.progress(event.oldValue, event.newValue).projectileCurved.over(animationDuration))
	}
	private val progressAnimator = new Animator(animatorInstructionPointer, activeFlag = linkedFlag)
	
	// Contains: 1) Slider X range, 2) Slider Y, 3) knob radius, 4) Max radius
	private val measurementsPointer = boundsPointer.strongMap(sliderMeasurementsIn)
	private val knobAreaPointer = progressPointer
		.lazyMergeWith(measurementsPointer) { case (progress, (xRange, y, radius, _)) =>
			val x = xRange.start + progress * xRange.length
			Circle(Point(x, y), radius)
		}
	
	private val baseColorsPointer = colorFunction match {
		case Left(staticColors) => Fixed(staticColors)
		case Right(colorFunction) =>
			colorFunction match {
				case Left(progressToColors) => progressAnimator.map(progressToColors)
				case Right(valueToColors) => valuePointer.mapWhile(linkedFlag)(valueToColors)
			}
	}
	private val knobColorPointer = baseColorsPointer.mergeWith(statePointer) { (baseColors, state) =>
		val intensity = if (hoverRadius <= 0) state.intensity else state.intensity / 4.0
		val highlighted = baseColors.knob.highlightedBy(intensity max 0.0)
		if (state.enabled) highlighted else highlighted.grayscale
	}
	
	override val focusListeners: Seq[FocusListener] =
		FocusListener.managingFocusPointer(_focusPointer) +: additionalFocusListeners
	override val customDrawers: Seq[CustomDrawer] = Drawer +: additionalDrawers
	
	private val repaintListener = ChangeListener.onAnyChange { repaint(High) }
	
	
	// INITIAL CODE --------------------------
	
	// Updates the state pointer when one of its components gets updated
	_focusPointer.addListener(updateStateListener)
	hoverFlag.addListener(updateStateListener)
	activatedFlag.addListener(updateStateListener)
	enabledFlag.addListenerWhile(linkedFlag)(updateStateListener)
	
	// Sets up mouse listening
	handlers ++= Pair(MainMouseListener, DragListener)
	
	addHierarchyListener { attached =>
		// Case: Attached
		if (attached) {
			enableFocusHandling()
			// Sets up keyboard listening
			KeyboardEvents ++= Pair(PressedTracker, ArrowKeyProgressUpdater)
			// Updates state (in case enabled changed during detachment)
			statePointer.update()
			actorHandler += progressAnimator
		}
		// Case: Detached => Removes keyboard listeners
		else {
			actorHandler -= progressAnimator
			KeyboardEvents --= Pair(PressedTracker, ArrowKeyProgressUpdater)
			disableFocusHandling()
		}
	}
	
	// Repaints when status changes
	statePointer.addAnyChangeListener {
		repaintArea(knobArea
			.mapOrigin { _ - position }
			.mapRadius { r => (r + hoverRadius) min measurementsPointer.value._4 }
			.bounds,
			High)
	}
	
	progressAnimator.addListener { event =>
		val (xRange, y, knobRadius, maxRadius) = measurementsPointer.value
		val totalRadius = (knobRadius + hoverRadius) min maxRadius
		val progressXRange = event.values.minMax.map { p => xRange.start + p * xRange.length }
		
		val affectedArea = Bounds(
			x = NumericSpan(progressXRange.first - totalRadius, progressXRange.second + totalRadius),
			y = NumericSpan(y - totalRadius, y + totalRadius))
		
		repaintArea(affectedArea - position, High)
	}
	
	
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
	
	private def knobArea = knobAreaPointer.value
	
	
	// IMPLEMENTED  --------------------------
	
	override def state = statePointer.value
	
	override def allowsFocusEnter: Boolean = true
	override def allowsFocusLeave: Boolean = true
	
	override def updateLayout(): Unit = ()
	
	
	// OTHER    ------------------------------
	
	// Converts an X-coordinate (relative to this component's parent's left side) to a progress value
	private def xToProgress(x: Double) = {
		val xRange = measurementsPointer.value._1
		if (x <= xRange.start)
			0.0
		else if (x >= xRange.end)
			1.0
		else
			(x - xRange.start) / xRange.length
	}
	
	// Returns 4 measurements:
	//      1) Slider X range
	//      2) Slider Y
	//      3) Knob radius
	//      4) Maximum radius of knob + hover effect
	private def sliderMeasurementsIn(bounds: Bounds) = {
		val maxRadius = bounds.size.minDimension / 2.0
		val minHoverRadius = hoverRadius min 2.0
		val knobRadius = (optimalKnobDiameter / 2.0) min (maxRadius - minHoverRadius)
		val minX = bounds.leftX + knobRadius + hoverRadius
		val maxX = bounds.rightX - knobRadius - hoverRadius
		val y = bounds.centerY
		
		(NumericSpan(minX, maxX), y, knobRadius, maxRadius)
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
			val (xRange, lineY, knobRadius, maxRadius) = sliderMeasurementsIn(bounds)
			val lineWidth = xRange.length
			val leftSideWidth = lineWidth * visualProgress
			val thresholdX = xRange.start + leftSideWidth
			
			// Uses anti-aliasing when drawing
			val d = drawer.antialiasing
			
			// Draws the right side line first
			val rightLineHeight = (optimalKnobDiameter / 4.0 / leftToRightBarHeightRatio) min (bounds.height * 0.8)
			val rightBarColor = baseColorsPointer.value.rightBar
			DrawSettings.onlyFill(if (enabled) rightBarColor else rightBarColor.grayscale)
				.use { implicit ds =>
					d.draw(Bounds(Point(thresholdX, lineY - rightLineHeight / 2.0),
						Size(lineWidth - leftSideWidth, rightLineHeight)).toRoundedRectangle(1.0))
				}
			// Next draws the left line
			val leftLineHeight = (optimalKnobDiameter / 4.0 * leftToRightBarHeightRatio) min (bounds.height * 0.8)
			val leftBarColor = baseColorsPointer.value.leftBar
			DrawSettings.onlyFill(if (enabled) leftBarColor else leftBarColor.grayscale)
				.use { implicit ds =>
					d.draw(Bounds(Point(xRange.start, lineY - leftLineHeight / 2.0), Size(leftSideWidth, leftLineHeight))
						.toRoundedRectangle(1.0))
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
			MouseButtonStateEvent.filter.left.over(bounds)
		
		
		// IMPLEMENTED  ------------------------
		
		override def handleCondition: Flag = enabledFlag
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = {
			if (event.pressed) {
				// Case: Knob clicked => Enters dragging mode
				if (knobArea.contains(event.position)) {
					draggingFlag.set()
					Consume("Slider drag started")
				}
				// Case: Area outside the knob clicked => Adjusts progress to match the clicked value
				else
					progress = xToProgress(event.position.x)
			}
			// Case: Mouse button released => Makes sure the drag doesn't continue
			else {
				draggingFlag.reset()
				Preserve
			}
		}
		
		// Updates hover state on mouse move
		override def onMouseMove(event: MouseMoveEvent): Unit = hoverFlag.value = knobArea.contains(event.position)
	}
	
	private object DragListener extends MouseDragListener
	{
		override def handleCondition: Flag = draggingFlag
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
		
		override val handleCondition: Flag = enabledAndFocusedFlag || keyDownFlag
		
		
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
		override def handleCondition: Flag = keyDownFlag
		override def keyDownEventFilter: KeyDownEventFilter = AcceptAll
		
		override def whileKeyDown(event: KeyDownEvent): Unit =
			keyDownPointer.value.foreach { case (_, direction) =>
				rawProgressPointer.update { _ + direction * progressVelocityWithArrow.over(event.duration) }
			}
	}
}
