package utopia.reach.component.visualization

import utopia.firmament.context.color.VariableColorContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.Priority.High
import utopia.genesis.graphics.{DrawLevel, DrawSettings, Drawer}
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.transform.Adjustment
import utopia.reach.component.factory.FromContextComponentFactoryFactory
import utopia.reach.component.factory.contextual.ContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ConcreteCustomDrawReachComponent, PartOfComponentHierarchy}

import scala.concurrent.duration.Duration

/**
  * Common trait for progress bar factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
trait ProgressBarSettingsLike[+Repr] extends CustomDrawableFactory[Repr] with BarSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	def barSettings: BarSettings
	
	/**
	  * A custom function which converts a progress value [0,1], plus a background color, to bar
	  * colors (left & right).
	  */
	def colorFunction: Option[(Double, Color) => Pair[Color]]
	
	/**
	  * The amount of time it takes for an individual transition animation to complete
	  */
	def animationDuration: Duration
	/**
	  * Maximum distance the knob may "jump" without an animated transition
	  */
	def maxJumpWithoutAnimation: Double
	
	/**
	  * The amount of time it takes for an individual transition animation to complete
	  * @param duration New animation duration to use.
	  *                 The amount of time it takes for an individual transition animation to complete
	  * @return Copy of this factory with the specified animation duration
	  */
	def withAnimationDuration(duration: Duration): Repr
	/**
	  * @param settings New bar settings to use.
	  * @return Copy of this factory with the specified bar settings
	  */
	def withBarSettings(settings: BarSettings): Repr
	/**
	  * A custom function which converts a progress value [0,1], plus a background color, to bar
	  * colors (left & right).
	  * @param f New color function to use.
	  *          A custom function which converts a progress value [0,1], plus a background color, to
	  *          bar colors (left & right).
	  * @return Copy of this factory with the specified color function
	  */
	def withColorFunction(f: Option[(Double, Color) => Pair[Color]]): Repr
	/**
	  * Maximum distance the knob may "jump" without an animated transition
	  * @param maxJump New max jump without animation to use.
	  *                Maximum distance the knob may "jump" without an animated transition
	  * @return Copy of this factory with the specified max jump without animation
	  */
	def withMaxJumpWithoutAnimation(maxJump: Double): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Copy of this item that doesn't apply animations (instead reflecting all changes immediately)
	  */
	def unanimated = withAnimationDuration(Duration.Zero)
	
	def slower(implicit adj: Adjustment) = mapAnimationDuration { _ * adj(1) }
	def faster(implicit adj: Adjustment) = mapAnimationDuration { _ * adj(-1) }
	
	
	// IMPLEMENTED	--------------------
	
	override def activeToInactiveHeightRatio = barSettings.activeToInactiveHeightRatio
	override def colorRolePointer = barSettings.colorRolePointer
	override def customColorsPointer = barSettings.customColorsPointer
	override def height = barSettings.height
	override def rounds = barSettings.rounds
	
	override def withActiveToInactiveHeightRatio(ratio: Double) =
		withBarSettings(barSettings.withActiveToInactiveHeightRatio(ratio))
	override def withColorRolePointer(p: Changing[ColorRole]) =
		withBarSettings(barSettings.withColorRolePointer(p))
	override def withCustomColorsPointer(p: Option[Changing[Pair[Color]]]) =
		withBarSettings(barSettings.withCustomColorsPointer(p))
	override def withHeight(height: SizeCategory) = withBarSettings(barSettings.withHeight(height))
	override def withRounds(round: Boolean) = withBarSettings(barSettings.withRounds(round))
	
	
	// OTHER	--------------------
	
	def withColorFunction(f: (Double, Color) => Pair[Color]): Repr = withColorFunction(Some(f))
	
	def mapAnimationDuration(f: Mutate[Duration]) = withAnimationDuration(f(animationDuration))
	def mapBarSettings(f: Mutate[BarSettings]) = withBarSettings(f(barSettings))
}

object ProgressBarSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing progress bars
  * @param customDrawers           Custom drawers to assign to created components
  * @param colorFunction           A custom function which converts a progress value [0,1], plus
  *                                a background color, to bar colors (left & right).
  * @param animationDuration       The amount of time it takes for an individual transition
  *                                animation to complete
  * @param maxJumpWithoutAnimation Maximum distance the knob may "jump" without an animated
  *                                transition
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
case class ProgressBarSettings(customDrawers: Seq[CustomDrawer] = Empty,
                               barSettings: BarSettings = BarSettings.default,
                               colorFunction: Option[(Double, Color) => Pair[Color]] = None,
                               animationDuration: Duration = 0.2.seconds,
                               maxJumpWithoutAnimation: Double = 2.0)
	extends ProgressBarSettingsLike[ProgressBarSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withAnimationDuration(duration: Duration) = copy(animationDuration = duration)
	override def withBarSettings(settings: BarSettings) = copy(barSettings = settings)
	override def withColorFunction(f: Option[(Double, Color) => Pair[Color]]) = {
		if (f.isDefined)
			copy(colorFunction = f, barSettings = barSettings.withCustomColorsPointer(None))
		else
			copy(colorFunction = f)
	}
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
	override def withMaxJumpWithoutAnimation(maxJump: Double) = copy(maxJumpWithoutAnimation = maxJump)
	override def withCustomColorsPointer(p: Option[Changing[Pair[Color]]]) = {
		if (p.isDefined)
			copy(colorFunction = None, barSettings = barSettings.withCustomColorsPointer(p))
		else
			super.withCustomColorsPointer(p)
	}
}

/**
  * Common trait for factories that wrap a progress bar settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
trait ProgressBarSettingsWrapper[+Repr] extends ProgressBarSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: ProgressBarSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: ProgressBarSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def animationDuration = settings.animationDuration
	override def barSettings = settings.barSettings
	override def colorFunction = settings.colorFunction
	override def customDrawers = settings.customDrawers
	override def maxJumpWithoutAnimation = settings.maxJumpWithoutAnimation
	override def withAnimationDuration(duration: Duration) = mapSettings { _.withAnimationDuration(duration) }
	
	override def withBarSettings(settings: BarSettings) = mapSettings { _.withBarSettings(settings) }
	override def withColorFunction(f: Option[(Double, Color) => Pair[Color]]) =
		mapSettings { _.withColorFunction(f) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = mapSettings { _.withCustomDrawers(drawers) }
	override def withMaxJumpWithoutAnimation(maxJump: Double) =
		mapSettings { _.withMaxJumpWithoutAnimation(maxJump) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: ProgressBarSettings => ProgressBarSettings) = withSettings(f(settings))
}

/**
  * Factory class used for constructing progress bars using contextual component creation
  * information
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
case class ContextualProgressBarFactory(hierarchy: ComponentHierarchy, context: VariableColorContext,
                                        settings: ProgressBarSettings = ProgressBarSettings.default)
	extends ProgressBarSettingsWrapper[ContextualProgressBarFactory]
		with ContextualFactory[VariableColorContext, ContextualProgressBarFactory]
		with PartOfComponentHierarchy
{
	// IMPLEMENTED	--------------------
	
	override def withContext(context: VariableColorContext) = copy(context = context)
	override def withSettings(settings: ProgressBarSettings) = copy(settings = settings)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param progressPointer A pointer that contains the displayed progress
	  * @param width Width of this bar
	  * @return A new progress bar
	  */
	def apply(progressPointer: Changing[Double], width: StackLength) =
		new ProgressBar(hierarchy, context, progressPointer, width, settings)
}

/**
  * Used for defining progress bar creation settings outside the component building process
  * @author Mikko Hilpinen
  * @since 13.02.2025, v1.6
  */
case class ProgressBarSetup(settings: ProgressBarSettings = ProgressBarSettings.default)
	extends ProgressBarSettingsWrapper[ProgressBarSetup]
		with FromContextComponentFactoryFactory[VariableColorContext, ContextualProgressBarFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(hierarchy: ComponentHierarchy, context: VariableColorContext) =
		ContextualProgressBarFactory(hierarchy, context, settings)
	override def withSettings(settings: ProgressBarSettings) = copy(settings = settings)
}

object ProgressBar extends ProgressBarSetup()
{
	// OTHER	--------------------
	
	def apply(settings: ProgressBarSettings) = withSettings(settings)
}

/**
  * A component used for displaying some level of progress
  * @author Mikko Hilpinen
  * @since 12.2.2025, v1.6
  */
class ProgressBar(override val hierarchy: ComponentHierarchy, context: VariableColorContext,
                  progressP: Changing[Double], stackWidth: StackLength, settings: ProgressBarSettings)
	extends ConcreteCustomDrawReachComponent
{
	// ATTRIBUTES   --------------------------
	
	override val calculatedStackSize: StackSize = {
		val height = context.margins.around(settings.height) * 0.5
		StackSize(stackWidth, height)
	}
	
	/**
	  * An animator that acts as a smooth progress pointer
	  */
	val progressAnimator = ProgressAnimator(progressP, widthPointer, settings.animationDuration,
		settings.maxJumpWithoutAnimation, linkedFlag)
	/**
	  * A pointer that contains the current left & right side color of this progress bar
	  */
	val colorsPointer = settings.colorFunction match {
		// Case: Color is based on a custom function
		case Some(colorFrom) => progressAnimator.mergeWith(context.backgroundPointer)(colorFrom)
		case None =>
			// Checks for a custom pointer setup
			settings.customColorsPointer.getOrElse {
				// Default: Color is based on a color role (pointer) within the current context
				context.colorPointer.forRole(settings.colorRolePointer).map { mainColor =>
					Pair(mainColor, mainColor.timesAlpha(0.5))
				}
			}
	}
	
	override val customDrawers: Seq[CustomDrawer] = Drawer +: settings.customDrawers
	
	
	// INITIAL CODE --------------------------
	
	context.actorHandler += progressAnimator
	
	// Adds automated repaints when progress changes
	if (settings.rounds)
		progressAnimator.addAnyChangeListener { repaint() }
	else
		progressAnimator.addListener { event =>
			val size = this.size
			val progressXRange = event.values.minMax.map { p => p * size.width }
			
			val affectedArea = Bounds(
				x = NumericSpan(progressXRange),
				y = NumericSpan(0.0, size.height))
			
			repaintArea(affectedArea, High)
		}
	// Also adds repaints when color (logic) changes
	if (settings.colorFunction.isDefined)
		context.backgroundPointer.addListenerWhile(linkedFlag) { _ => repaint() }
	else
		settings.customColorsPointer match {
			case Some(colorsP) => colorsP.addListenerWhile(linkedFlag) { _ => repaint() }
			case None => settings.colorRolePointer.addListenerWhile(linkedFlag) { _ => repaint() }
		}
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Visually displayed progress value [0,1]
	  */
	def visualProgress = progressAnimator.value
	
	
	// IMPLEMENTED  --------------------------
	
	override def updateLayout(): Unit = ()
	
	
	// NESTED   ------------------------------
	
	private object Drawer extends CustomDrawer
	{
		override val opaque: Boolean = false
		override val drawLevel: DrawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			// Uses anti-aliasing when drawing (if using rounding)
			val d = if (settings.rounds) drawer.antialiasing else drawer
			
			val leftWidth = bounds.width * visualProgress
			val (leftColor, rightColor) = colorsPointer.value.toTuple
			
			def drawBounds(b: Bounds)(implicit ds: DrawSettings) =
				if (settings.rounds) d.draw(b.toRoundedRectangle(1.0)) else d.draw(b)
			
			// Draws the right side line first
			val rightLineYRange = {
				if (settings.activeToInactiveHeightRatio == 1.0)
					bounds.y
				else {
					val rightLineAscend = bounds.height / settings.activeToInactiveHeightRatio / 2.0
					val center = bounds.centerY
					NumericSpan(center - rightLineAscend, center + rightLineAscend)
				}
			}
			DrawSettings.onlyFill(rightColor).use { implicit ds =>
				val baseBounds = {
					if (settings.rounds)
						bounds
					else
						bounds.mapX { _.mapStart { _ + leftWidth } }
				}
				drawBounds(baseBounds.withY(rightLineYRange))
			}
			// Next draws the left line
			DrawSettings.onlyFill(leftColor).use { implicit ds => drawBounds(bounds.leftSlice(leftWidth)) }
		}
	}
}
