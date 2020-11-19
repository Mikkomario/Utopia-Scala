package utopia.reflection.component.reach.input

import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.Changing
import utopia.genesis.animation.Animation
import utopia.genesis.color.Color
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{Actor, KeyStateListener}
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point, Vector2D}
import utopia.genesis.util.Drawer
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reflection.color.ColorRole
import utopia.reflection.color.ColorRole.Secondary
import utopia.reflection.component.context.{AnimationContextLike, ColorContextLike}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{ButtonLike, CustomDrawReachComponent}
import utopia.reflection.component.template.input.InteractionWithPointer
import utopia.reflection.cursor.Cursor
import utopia.reflection.event.{ButtonState, FocusListener}
import utopia.reflection.shape.stack.{StackLength, StackSize}
import utopia.reflection.util.ComponentCreationDefaults

import scala.concurrent.duration.FiniteDuration

object Switch extends ContextInsertableComponentFactoryFactory[ColorContextLike, SwitchFactory, ContextualSwitchFactory]
{
	// ATTRIBUTES	--------------------------------
	
	private val shadowColor = Color.black.withAlpha(0.2)
	
	
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
			  enabledPointer: Changing[Boolean] = Changing.wrap(true),
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
			  enabledPointer: Changing[Boolean] = Changing.wrap(true), colorRole: ColorRole = Secondary,
			  customDrawers: Vector[CustomDrawer] = Vector(), focusListeners: Seq[FocusListener] = Vector())
			 (implicit animationContext: AnimationContextLike) =
	{
		val knobR = context.margins.medium * 0.75
		val offset = (knobR * 0.1) min 1.0
		factory(animationContext.actorHandler, context.color(colorRole), knobR * 2, knobR * 0.75,
			Vector2D(-offset, offset), valuePointer, enabledPointer, animationContext.animationDuration,
			customDrawers, focusListeners)
	}
}

/**
  * Used for toggling a setting on or off
  * @author Mikko Hilpinen
  * @since 19.11.2020, v2
  */
class Switch(override val parentHierarchy: ComponentHierarchy, actorHandler: ActorHandler, color: Color,
			 knobDiameter: Double, hoverExtraRadius: Double = 0.0, knobShadowOffset: Vector2D = Vector2D(-1, 1),
			 override val valuePointer: PointerWithEvents[Boolean] = new PointerWithEvents(false),
			 enabledPointer: Changing[Boolean] = Changing.wrap(true),
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
	
	override def cursorToImage(cursor: Cursor, position: Point) =
		if (value) cursor.over(color) else cursor.over(Color.white)
	
	
	// NESTED	------------------------------------
	
	private object SwitchDrawer extends CustomDrawer with Actor
	{
		// ATTRIBUTES	-------------
		
		private var currentAnimation: Animation[Double] = Animation.fixed(if (value) 1.0 else 0.0)
		private var currentProgress: Double = 1.0
		
		
		// COMPUTED	-----------------
		
		private def state = currentAnimation(currentProgress)
		
		
		// IMPLEMENTED	-------------
		
		override def allowsHandlingFrom(handlerType: HandlerType) = currentProgress < 1.0
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			if (bounds.size.isPositive)
			{
				// Calculates drawn area bounds
				val shorterSide = bounds.size.minDimension
				val knobR = knobRadius min (shorterSide / 2)
				val hoverR = hoverExtraRadius min (shorterSide / 2 - knobR)
				
				val y = bounds.center.y
				val minX = bounds.x + hoverR
				val maxX = bounds.rightX - hoverR
				
				val minKnobX = minX + knobR
				val maxKnobX = maxX - knobR
				val knobX = minKnobX + (maxKnobX - minKnobX) * state
				
				// Draws the hover area, if necessary
				if (hoverR > 0 && enabled)
				{
					val hoverAlpha =
					{
						if (isPressed)
							0.2
						else if (hasFocus)
							0.15
						else if (isMouseOver)
							0.1
						else
							0.0
					}
					if (hoverAlpha > 0)
					{
						val baseHoverColor = if (value) color else Color.black
						drawer.onlyFill(baseHoverColor.timesAlpha(hoverAlpha))
							.draw(Circle(Point(knobX, y), knobR + hoverR))
					}
				}
				
				// Draws the bar
				val barColor =
				{
					val base = if (value) color.timesAlpha(0.5) else color.timesAlpha(0.66).grayscale
					if (enabled) base else base.timesAlpha(0.66)
				}
				val barR = knobR * 0.7
				drawer.onlyFill(barColor).draw(Bounds.between(Point(minKnobX - barR, y - barR),
					Point(maxKnobX + barR, y + barR)).toRoundedRectangle(1))
				
				// Draws knob shadow and the knob itself
				val knobColor =
				{
					val base = if (value) color else Color.white
					if (enabled) base else base.timesAlpha(0.66)
				}
				val knob = Circle(Point(knobX, y), knobR)
				if (knobShadowOffset.nonZero)
					drawer.onlyFill(Switch.shadowColor).draw(knob + knobShadowOffset)
				drawer.onlyFill(knobColor).draw(knob)
			}
		}
		
		override def act(duration: FiniteDuration) =
		{
			val increment = duration / animationDuration
			currentProgress = (currentProgress + increment) min 1.0
			repaint()
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
