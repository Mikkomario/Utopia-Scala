package utopia.reach.component.input.check

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{AlwaysTrue, ChangingLike}
import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.AnimationLike.AnyAnimation
import utopia.paradigm.color.Color
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{Actor, KeyStateListener}
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Point, Size, Vector2D}
import utopia.genesis.util.Drawer
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{ButtonLike, CustomDrawReachComponent}
import utopia.reach.cursor.Cursor
import utopia.reach.focus.FocusListener
import utopia.reach.util.Priority.VeryHigh
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.{AnimationContextLike, ColorContextLike}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.template.input.InteractionWithPointer
import utopia.reflection.event.ButtonState
import utopia.reflection.shape.stack.{StackLength, StackSize}
import utopia.reflection.util.ComponentCreationDefaults

import java.awt.event.KeyEvent
import scala.concurrent.duration.FiniteDuration

object Switch extends ContextInsertableComponentFactoryFactory[ColorContextLike, SwitchFactory, ContextualSwitchFactory]
{
	// ATTRIBUTES	--------------------------------
	
	private val shadowColor = Color.black.withAlpha(0.1)
	
	
	// IMPLEMENTED	--------------------------------
	
	override def apply(hierarchy: ComponentHierarchy) = new SwitchFactory(hierarchy)
}

class SwitchFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[ColorContextLike, ContextualSwitchFactory]
{
	// IMPLEMENTED	--------------------------------
	
	override def withContext[N <: ColorContextLike](context: N) =
		ContextualSwitchFactory(this, context)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * Creates a new switch
	  * @param actorHandler Actor handler that will deliver action events for animations
	  * @param color Switch activation color
	  * @param knobDiameter Diameter (2*r) used in the knob of this switch
	  * @param hoverExtraRadius Additional radius applied for the hover effect (default = 0)
	  * @param knobShadowOffset Offset applied for the knob shadow effect (default = 1px left and down)
	  * @param valuePointer A mutable pointer to this switches pointer (default = new pointer)
	  * @param enabledPointer A pointer containing the enabled status of this switch (default = always enabled)
	  * @param animationDuration Duration it takes to complete the transition animation (default = global default)
	  * @param customDrawers Custom drawers applied to this switch (default = empty)
	  * @param focusListeners Focus listeners applied to this switch (default = empty)
	  * @return A new switch
	  */
	def apply(actorHandler: ActorHandler, color: Color,
			  knobDiameter: Double, hoverExtraRadius: Double = 0.0, knobShadowOffset: Vector2D = Vector2D(-1, 1),
			  valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			  enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
			  animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
			  customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector()) =
		new Switch(parentHierarchy, actorHandler, color, knobDiameter, hoverExtraRadius, knobShadowOffset,
			valuePointer, enabledPointer, animationDuration, customDrawers, focusListeners)
}

case class ContextualSwitchFactory[N <: ColorContextLike](factory: SwitchFactory, context: N)
	extends ContextualComponentFactory[N, ColorContextLike, ContextualSwitchFactory]
{
	// IMPLEMENTED	---------------------------------
	
	override def withContext[N2 <: ColorContextLike](newContext: N2) = copy(context = newContext)
	
	
	// OTHER	-------------------------------------
	
	/**
	  * Creates a new switch
	  * @param valuePointer A mutable pointer to this switches pointer (default = new pointer)
	  * @param enabledPointer A pointer containing the enabled status of this switch (default = always enabled)
	  * @param colorRole Color role used in this switch when active (default = Secondary)
	  * @param customDrawers Custom drawers applied to this switch (default = empty)
	  * @param focusListeners Focus listeners applied to this switch (default = empty)
	  * @return A new switch
	  */
	def apply(valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			  enabledPointer: ChangingLike[Boolean] = AlwaysTrue, colorRole: ColorRole = Secondary,
			  customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector())
			 (implicit animationContext: AnimationContextLike) =
	{
		val knobR = context.margins.medium * 0.75
		val xOffset = (knobR * 0.1) min 1.0
		val yOffset = (knobR * 0.2) min 2.0
		factory(animationContext.actorHandler, context.color(colorRole), knobR * 2, knobR * 0.75,
			Vector2D(-xOffset, yOffset), valuePointer, enabledPointer, animationContext.animationDuration,
			customDrawers, focusListeners)
	}
}

/**
  * Used for toggling a setting on or off
  * @author Mikko Hilpinen
  * @since 19.11.2020, v0.1
  */
class Switch(override val parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, color: Color,
			 knobDiameter: Double, hoverExtraRadius: Double = 0.0, knobShadowOffset: Vector2D = Vector2D(-1, 1),
			 override val valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			 enabledPointer: ChangingLike[Boolean] = AlwaysTrue,
			 animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
			 additionalDrawers: Vector[CustomDrawer] = Vector(),
			 additionalFocusListeners: Seq[FocusListener] = Vector())
	extends CustomDrawReachComponent with ButtonLike with InteractionWithPointer[Boolean]
{
	// ATTRIBUTES	--------------------------------
	
	private val baseStatePointer = new PointerWithEvents(ButtonState.default)
	
	override val statePointer = baseStatePointer.mergeWith(enabledPointer) { (state, enabled) =>
		state.copy(isEnabled = enabled) }
	
	// The "bar" is exactly two knobs wide and 70% knob high
	// The width and height are then extended on both sides by extra hover radius, which is not included in the
	// minimum size. The switch extends horizontally, but only up to 200%
	override val calculatedStackSize =
	{
		val standardWidth = knobDiameter * 2
		val optimalHeight = knobDiameter + hoverExtraRadius * 2
		StackSize(StackLength(standardWidth, standardWidth + hoverExtraRadius * 2,
			standardWidth * 2 + hoverExtraRadius * 2), StackLength(knobDiameter, optimalHeight, optimalHeight * 1.5))
	}
	
	override val customDrawers = SwitchDrawer +: additionalDrawers
	override val focusListeners = new ButtonDefaultFocusListener(baseStatePointer) +: additionalFocusListeners
	override val focusId = hashCode()
	
	
	// INITIAL CODE	--------------------------------
	
	setup(baseStatePointer)
	
	valuePointer.addListener { event => SwitchDrawer.updateTarget(event.newValue) }
	
	addHierarchyListener { isAttached =>
		if (isAttached)
		{
			actorHandler += SwitchDrawer
			GlobalKeyboardEventHandler += ArrowKeyListener
		}
		else
		{
			actorHandler -= SwitchDrawer
			GlobalKeyboardEventHandler -= ArrowKeyListener
		}
	}
	
	
	// COMPUTED	------------------------------------
	
	private def knobRadius = knobDiameter / 2
	
	
	// IMPLEMENTED	--------------------------------
	
	override def updateLayout() = ()
	
	override protected def trigger() = value = !value
	
	// TODO: Handle unselected color selection better
	override def cursorToImage(cursor: Cursor, position: Point) =
		if (value) cursor.over(color) else cursor.over(Color.white)
	
	
	// NESTED	------------------------------------
	
	private object SwitchDrawer extends CustomDrawer with Actor
	{
		// ATTRIBUTES	-------------
		
		private var currentAnimation: AnyAnimation[Double] = Animation.fixed(if (value) 1.0 else 0.0)
		private var currentProgress: Double = 1.0
		
		
		// COMPUTED	-----------------
		
		private def state = currentAnimation(currentProgress)
		
		
		// IMPLEMENTED	-------------
		
		override def opaque = false
		
		override def allowsHandlingFrom(handlerType: HandlerType) = currentProgress < 1.0
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			if (bounds.size.isPositive)
			{
				val actualDrawer = if (enabled) drawer else drawer.withAlpha(0.66)
				
				// Calculates drawn area bounds
				val shorterSide = bounds.size.minDimension
				val knobR = knobRadius min (shorterSide / 2)
				val hoverR = hoverExtraRadius min (shorterSide / 2 - knobR)
				
				val y = bounds.center.y
				val minX = bounds.x + hoverR
				val maxX = bounds.rightX - hoverR
				
				val minKnobX = minX + knobR
				val maxKnobX = maxX - knobR
				val progress = state
				val knobX = minKnobX + (maxKnobX - minKnobX) * progress
				
				// Draws the hover area, if necessary
				if (hoverR > 0 && enabled)
				{
					val hoverAlpha = Switch.this.state.hoverAlpha
					if (hoverAlpha > 0)
					{
						val baseHoverColor = if (value) color else Color.textBlackDisabled
						actualDrawer.onlyFill(baseHoverColor.timesAlpha(hoverAlpha))
							.draw(Circle(Point(knobX, y), knobR + hoverR))
					}
				}
				
				// Draws the bar (gray + coloured)
				val barR = knobR * 0.7
				val barStartY = y - barR
				val barHeight = 2 * barR
				if (progress < 1)
				{
					actualDrawer.onlyFill(color.timesAlpha(0.66).grayscale)
						.draw(Bounds(Point(knobX - barR, barStartY), Size(maxKnobX - knobX + 2 * barR, barHeight))
							.toRoundedRectangle(1.0))
				}
				if (progress > 0)
				{
					actualDrawer.onlyFill(color.timesAlpha(0.5))
						.draw(Bounds(Point(minKnobX - barR, barStartY), Size(knobX - minKnobX + 2 * barR, barHeight))
							.toRoundedRectangle(1.0))
				}
				
				// Draws knob shadow and the knob itself
				val knobColor =
				{
					if (progress >= 1)
						color
					else if (progress <= 0)
						Color.white
					else
						color.average(Color.white, progress, 1 - progress)
				}
				val knob = Circle(Point(knobX, y), knobR)
				if (enabled && knobShadowOffset.nonZero)
					actualDrawer.onlyFill(Switch.shadowColor).draw(knob + knobShadowOffset)
				actualDrawer.onlyFill(knobColor).draw(knob)
			}
		}
		
		override def act(duration: FiniteDuration) =
		{
			val increment = duration / animationDuration
			currentProgress = (currentProgress + increment) min 1.0
			repaint(VeryHigh)
		}
		
		
		// OTHER	-----------------
		
		def updateTarget(newStatus: Boolean) =
		{
			val startValue = state
			val newTarget = if (newStatus) 1.0 else 0.0
			if (startValue != newTarget)
			{
				val transition = newTarget - startValue
				currentAnimation = Animation { p => startValue + p * transition }.projectileCurved
				currentProgress = 0.0
			}
		}
	}
	
	private object ArrowKeyListener extends KeyStateListener
	{
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter &&
			KeyStateEvent.keysFilter(KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT)
		
		override def onKeyState(event: KeyStateEvent) = value = event.index == KeyEvent.VK_RIGHT
		
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus
	}
}
