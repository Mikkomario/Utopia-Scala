package utopia.reach.component.input.check

import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.context.color.StaticColorContext
import utopia.firmament.context.{AnimationContext, ComponentCreationDefaults}
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.HotKey
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.DrawLevel.Normal
import utopia.genesis.graphics.Priority.VeryHigh
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.handling.event.keyboard.Key.{LeftArrow, RightArrow}
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.AnimationLike.AnyAnimation
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.color.{Color, ColorRole, ColorShade}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.button.{AbstractButton, ButtonSettings, ButtonSettingsLike}
import utopia.reach.component.factory.contextual.ColorContextualFactory
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, PartOfComponentHierarchy}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener

import java.awt.event.KeyEvent
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
  * Common trait for switch factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait SwitchSettingsLike[+Repr] extends CustomDrawableFactory[Repr] with ButtonSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings that apply to switch functionality
	  */
	def buttonSettings: ButtonSettings
	
	/**
	  * Settings that apply to switch functionality
	  * @param settings New button settings to use.
	  *                 Settings that apply to switch functionality
	  * @return Copy of this factory with the specified button settings
	  */
	def withButtonSettings(settings: ButtonSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def enabledPointer = buttonSettings.enabledPointer
	override def focusListeners = buttonSettings.focusListeners
	override def hotKeys = buttonSettings.hotKeys
	
	override def withEnabledPointer(p: Changing[Boolean]) =
		withButtonSettings(buttonSettings.withEnabledPointer(p))
	override def withFocusListeners(listeners: Seq[FocusListener]) =
		withButtonSettings(buttonSettings.withFocusListeners(listeners))
	override def withHotKeys(keys: Set[HotKey]) = withButtonSettings(buttonSettings.withHotKeys(keys))
	
	
	// OTHER	--------------------
	
	def mapButtonSettings(f: ButtonSettings => ButtonSettings) = withButtonSettings(f(buttonSettings))
}

object SwitchSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
	
	
	// IMPLICIT ------------------------
	
	implicit def wrap(buttonSettings: ButtonSettings): SwitchSettings = apply(buttonSettings = buttonSettings)
}
/**
  * Combined settings used when constructing switchs
  * @param customDrawers  Custom drawers to assign to created components
  * @param buttonSettings Settings that apply to switch functionality
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class SwitchSettings(customDrawers: Seq[CustomDrawer] = Empty,
                          buttonSettings: ButtonSettings = ButtonSettings.default)
	extends SwitchSettingsLike[SwitchSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withButtonSettings(settings: ButtonSettings) = copy(buttonSettings = settings)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) = copy(customDrawers = drawers)
}

/**
  * Common trait for factories that wrap a switch settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait SwitchSettingsWrapper[+Repr] extends SwitchSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: SwitchSettings
	
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: SwitchSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def buttonSettings = settings.buttonSettings
	override def customDrawers = settings.customDrawers
	
	override def withButtonSettings(settings: ButtonSettings) = mapSettings { _.withButtonSettings(settings) }
	override def withCustomDrawers(drawers: Seq[CustomDrawer]) =
		mapSettings { _.withCustomDrawers(drawers) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: SwitchSettings => SwitchSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing switchs
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
trait SwitchFactoryLike[+Repr] extends SwitchSettingsWrapper[Repr] with PartOfComponentHierarchy
{
	import utopia.firmament.context.ComponentCreationDefaults.componentLogger
	
	// OTHER    -------------------
	
	/**
	  * Creates a new switch
	  * @param actorHandler      Actor handler that will deliver action events for animations
	  * @param color             Switch activation color
	  * @param knobDiameter      Diameter (2*r) used in the knob of this switch
	  * @param hoverExtraRadius  Additional radius applied for the hover effect (default = 0)
	  * @param knobShadowOffset  Offset applied for the knob shadow effect (default = 1px left and down)
	  * @param valuePointer      A mutable pointer to this switches pointer (default = new pointer)
	  * @param shade             The shade of this switch (dark or light). Call-by-name.
	  *                          Use a value suitable against the current background.
	  *                          Default = Light.
	  * @param animationDuration Duration it takes to complete the transition animation (default = global default)
	  * @return A new switch
	  */
	protected def _apply(actorHandler: ActorHandler, color: Color, knobDiameter: Double,
	                     hoverExtraRadius: Double = 0.0, knobShadowOffset: Vector2D = Vector2D(-1, 1),
	                     valuePointer: EventfulPointer[Boolean] = EventfulPointer(false),
	                     shade: => ColorShade = Light,
	                     animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration) =
	{
		val scaling = ComponentCreationDefaults.switchScalingFactor
		new Switch(parentHierarchy, actorHandler, color, math.round(knobDiameter * scaling).toDouble,
			math.round(hoverExtraRadius * scaling).toDouble,
			knobShadowOffset.map { o => math.round(o * scaling).toDouble },
			valuePointer, settings, shade, animationDuration)
	}
}

/**
  * Factory class used for constructing switches using contextual component creation information
  * @param colorRole Switch color when activated
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class ContextualSwitchFactory(parentHierarchy: ComponentHierarchy, context: StaticColorContext,
                                   settings: SwitchSettings = SwitchSettings.default,
                                   colorRole: ColorRole = ColorRole.Secondary)
	extends SwitchFactoryLike[ContextualSwitchFactory] with ColorContextualFactory[ContextualSwitchFactory]
{
	// IMPLEMENTED	---------------------------------
	
	override def self: ContextualSwitchFactory = this
	
	override def withContext(newContext: StaticColorContext) = copy(context = newContext)
	override def withSettings(settings: SwitchSettings) = copy(settings = settings)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * @param color Switch color when activated
	  * @return Copy of this factory with the specified color role
	  */
	def withColor(color: ColorRole) = copy(colorRole = color)
	
	/**
	  * Creates a new switch
	  * @param valuePointer   A mutable pointer to this switches pointer (default = new pointer)
	  * @return A new switch
	  */
	def apply(valuePointer: EventfulPointer[Boolean] = EventfulPointer(false))
	         (implicit animationContext: AnimationContext) =
	{
		val knobR = context.margins.medium
		val xOffset = (knobR * 0.2) min 1.0
		val yOffset = (knobR * 0.3) min 2.0
		val shade = context.background.shade.opposite
		_apply(animationContext.actorHandler, context.withEnhancedColorContrast.color(colorRole), knobR * 2, knobR * 0.75,
			Vector2D(-xOffset, yOffset), valuePointer, shade, animationContext.animationDuration)
	}
}

/**
  * Factory class that is used for constructing switchs without using contextual information
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class SwitchFactory(parentHierarchy: ComponentHierarchy,
                         settings: SwitchSettings = SwitchSettings.default)
	extends SwitchFactoryLike[SwitchFactory] with FromContextFactory[StaticColorContext, ContextualSwitchFactory]
{
	import utopia.firmament.context.ComponentCreationDefaults.componentLogger
	
	// IMPLEMENTED	--------------------
	
	override def withContext(context: StaticColorContext) = ContextualSwitchFactory(parentHierarchy, context, settings)
	override def withSettings(settings: SwitchSettings) = copy(settings = settings)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * Creates a new switch
	  * @param actorHandler      Actor handler that will deliver action events for animations
	  * @param color             Switch activation color
	  * @param knobDiameter      Diameter (2*r) used in the knob of this switch
	  * @param hoverExtraRadius  Additional radius applied for the hover effect (default = 0)
	  * @param knobShadowOffset  Offset applied for the knob shadow effect (default = 1px left and down)
	  * @param valuePointer      A mutable pointer to this switches pointer (default = new pointer)
	  * @param shade             The shade of this switch (dark or light). Call-by-name.
	  *                          Use a value suitable against the current background.
	  *                          Default = Light.
	  * @param animationDuration Duration it takes to complete the transition animation (default = global default)
	  * @return A new switch
	  */
	def apply(actorHandler: ActorHandler, color: Color, knobDiameter: Double,
	          hoverExtraRadius: Double = 0.0, knobShadowOffset: Vector2D = Vector2D(-1, 1),
	          valuePointer: EventfulPointer[Boolean] = EventfulPointer(false),
	          shade: => ColorShade = Light,
	          animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration) =
		_apply(actorHandler, color, knobDiameter, hoverExtraRadius, knobShadowOffset, valuePointer, shade,
			animationDuration)
}

/**
  * Used for defining switch creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 21.06.2023, v1.1
  */
case class SwitchSetup(settings: SwitchSettings = SwitchSettings.default)
	extends SwitchSettingsWrapper[SwitchSetup] with ComponentFactoryFactory[SwitchFactory]
		with FromContextComponentFactoryFactory[StaticColorContext, ContextualSwitchFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = SwitchFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: StaticColorContext) =
		ContextualSwitchFactory(hierarchy, context, settings)
	override def withSettings(settings: SwitchSettings) = copy(settings = settings)
}

object Switch extends SwitchSetup()
{
	// ATTRIBUTES	--------------------
	
	private val shadowDs = DrawSettings.onlyFill(Color.black.withAlpha(0.15))
	
	
	// OTHER	--------------------
	
	def apply(settings: SwitchSettings) = withSettings(settings)
}

/**
  * Used for toggling a setting on or off
  * @author Mikko Hilpinen
  * @since 19.11.2020, v0.1
  */
class Switch(override val parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, color: Color,
             knobDiameter: Double, hoverExtraRadius: Double = 0.0, knobShadowOffset: Vector2D = Vector2D(-1, 1),
             override val valuePointer: EventfulPointer[Boolean] = EventfulPointer(false)(ComponentCreationDefaults.componentLogger),
             settings: SwitchSettings = SwitchSettings.default, shade: => ColorShade = Light,
             animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration)
	extends AbstractButton(settings) with CustomDrawReachComponent with InteractionWithPointer[Boolean]
{
	// ATTRIBUTES	--------------------------------
	
	// The "bar" is exactly two knobs wide and 70% knob high
	// The width and height are then extended on both sides by extra hover radius, which is not included in the
	// minimum size. The switch extends horizontally, but only up to 200%
	override val calculatedStackSize = {
		val standardWidth = knobDiameter * 2
		val optimalHeight = knobDiameter + hoverExtraRadius * 2
		StackSize(StackLength(standardWidth, standardWidth + hoverExtraRadius * 2,
			standardWidth * 2 + hoverExtraRadius * 2), StackLength(knobDiameter, optimalHeight, optimalHeight * 1.5))
	}
	
	override val customDrawers = SwitchDrawer +: settings.customDrawers
	
	
	// INITIAL CODE	--------------------------------
	
	setup()
	valuePointer.addContinuousListener { event => SwitchDrawer.updateTarget(event.newValue) }
	addHierarchyListener { isAttached =>
		if (isAttached) {
			actorHandler += SwitchDrawer
			KeyboardEvents += ArrowKeyListener
		}
		else {
			actorHandler -= SwitchDrawer
			KeyboardEvents -= ArrowKeyListener
		}
	}
	
	
	// COMPUTED	------------------------------------
	
	private def knobRadius = knobDiameter / 2
	
	
	// IMPLEMENTED	--------------------------------
	
	override def updateLayout() = ()
	
	override protected def trigger() = value = !value
	
	override def cursorToImage(cursor: Cursor, position: Point) = if (value) cursor.over(color) else cursor.over(shade)
	
	
	// NESTED	------------------------------------
	
	private object SwitchDrawer extends CustomDrawer with Actor
	{
		// ATTRIBUTES	-------------
		
		private val progressPointer = Volatile.eventful(1.0)
		override val handleCondition: Flag = progressPointer.map { _ < 1.0 }
		
		private var currentAnimation: AnyAnimation[Double] = Animation.fixed(if (value) 1.0 else 0.0)
		
		
		// COMPUTED	-----------------
		
		private def state = currentAnimation(progressPointer.value)
		
		
		// IMPLEMENTED	-------------
		
		override def opaque = false
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			if (bounds.size.sign.isPositive) {
				val actualDrawer = (if (enabled) drawer else drawer.withAlpha(0.66)).antialiasing
				val baseColor = shade match {
					case Light => Color.white
					case Dark => Color.black
				}
				
				// Calculates drawn area bounds
				val shorterSide = bounds.size.minDimension
				val knobR = knobRadius min (shorterSide / 2)
				val hoverR = hoverExtraRadius min (shorterSide / 2 - knobR)
				
				val y = bounds.center.y
				val minX = bounds.leftX + hoverR
				val maxX = bounds.rightX - hoverR
				
				val minKnobX = minX + knobR
				val maxKnobX = maxX - knobR
				val progress = state
				val knobX = minKnobX + (maxKnobX - minKnobX) * progress
				
				// Draws the hover area, if necessary
				if (hoverR > 0 && enabled) {
					val hoverAlpha = Switch.this.state.hoverAlpha
					if (hoverAlpha > 0) {
						val baseHoverColor = if (value) color else baseColor
						actualDrawer.draw(Circle(Point(knobX, y), knobR + hoverR))(
							DrawSettings.onlyFill(baseHoverColor.timesAlpha(hoverAlpha)))
					}
				}
				
				// Draws the bar (gray + coloured)
				val barR = knobR * 0.7
				val barStartY = y - barR
				val barHeight = 2 * barR
				// Draws the left background side (gray)
				if (progress < 1) {
					actualDrawer.draw(
						Bounds(Point(knobX - barR, barStartY), Size(maxKnobX - knobX + 2 * barR, barHeight))
							.toRoundedRectangle(1.0))(
						DrawSettings.onlyFill(color.timesAlpha(0.66).grayscale))
				}
				// Draws the right background side (color)
				if (progress > 0) {
					actualDrawer.draw(
						Bounds(Point(minKnobX - barR, barStartY), Size(knobX - minKnobX + 2 * barR, barHeight))
							.toRoundedRectangle(1.0))(
						DrawSettings.onlyFill(color.timesAlpha(0.5)))
				}
				
				// Draws knob shadow and the knob itself
				val knobColor = {
					if (progress >= 1)
						color
					else if (progress <= 0)
						baseColor
					else
						color.average(baseColor, progress, 1 - progress)
				}
				val knob = Circle(Point(knobX, y), knobR)
				if (enabled && knobShadowOffset.nonZero)
					actualDrawer.draw(knob + knobShadowOffset)(Switch.shadowDs)
				actualDrawer.draw(knob)(DrawSettings.onlyFill(knobColor))
			}
		}
		
		override def act(duration: FiniteDuration) = {
			val increment = duration / animationDuration
			progressPointer.update { p => (p + increment) min 1.0 }
			repaint(VeryHigh)
		}
		
		
		// OTHER	-----------------
		
		def updateTarget(newStatus: Boolean) = {
			val startValue = state
			val newTarget = if (newStatus) 1.0 else 0.0
			if (startValue != newTarget) {
				val transition = newTarget - startValue
				currentAnimation = Animation { p => startValue + p * transition }.projectileCurved
				progressPointer.value = 0.0
			}
		}
	}
	
	private object ArrowKeyListener extends KeyStateListener
	{
		// ATTRIBUTES   ----------------------
		
		override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter(RightArrow, LeftArrow)
		
		
		// IMPLEMENTED  ----------------------
		
		override def handleCondition: Flag = focusPointer
		
		override def onKeyState(event: KeyStateEvent) = value = event.index == KeyEvent.VK_RIGHT
	}
}
