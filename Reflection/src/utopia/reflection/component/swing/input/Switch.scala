package utopia.reflection.component.swing.input

import utopia.firmament.context.{AnimationContext, ColorContext, ComponentCreationDefaults}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.event.{ConsumeEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{Actor, ActorHandlerType, MouseButtonStateListener}
import utopia.inception.handling.HandlerType
import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.AnimationLike.AnyAnimation
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Point}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.template.DrawLevel.Normal
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.event.StackHierarchyListener
import utopia.firmament.model.stack.{StackLength, StackSize}

import scala.concurrent.duration.FiniteDuration

object Switch
{
	private val maxHeightRatio = 0.5
	
	/**
	  * Creates a new switch using contextual information
	 *  @param width Switch component width
	 *  @param initialState First state of this switch (default = false = off)
	  * @param context Component creation context
	  * @return A new switch
	  */
	def contextual(width: StackLength, initialState: Boolean = false)
				  (implicit context: ColorContext, animationContext: AnimationContext) =
	{
		val knobColor = context.background.shade match {
			case Light => Color.black
			case Dark => Color.white
		}
		new Switch(animationContext.actorHandler, width, context.color.secondary, knobColor,
			animationContext.animationDuration, initialState)
	}
}

/**
  * Switches are used for setting a value either on or off
  * @author Mikko Hilpinen
  * @since 4.5.2019, v1+
  */
class Switch(actorHandler: ActorHandler, val targetWidth: StackLength, val color: Color, knobColor: Color = Color.white,
			 animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
			 initialState: Boolean = false)
	extends AwtComponentWrapperWrapper with MutableCustomDrawableWrapper with InteractionWithPointer[Boolean]
		with ReflectionStackable
{
	// ATTRIBUTES	-----------------
	
	private val label = new EmptyLabel()
	
	private var _enabled = true
	private var _attached = false
	
	override val stackSize = StackSize(targetWidth, targetWidth.noMax * Switch.maxHeightRatio)
	override val valuePointer = new PointerWithEvents(initialState)
	
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	
	// INITIAL CODE	-----------------
	
	addCustomDrawer(SwitchDrawer2)
	label.setHandCursor()
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return Whether this switch is currently enabled (true) or disabled (false)
	  */
	def enabled = _enabled
	def enabled_=(newState: Boolean) =
	{
		_enabled = newState
		if (newState) label.setHandCursor() else label.setArrowCursor()
	}
	
	/**
	  * @return Whether this switch is currently enabled (true) or disabled (false)
	  */
	def isEnabled = enabled
	@deprecated("Please use enabled = _ instead", "v1.2")
	def isEnabled_=(newState: Boolean) = enabled = newState
	
	/**
	  * @return Whether this switch is currently on (value == true)
	  */
	def isOn = value
	@deprecated("Please use value = _ instead", "v1.2")
	def isOn_=(newState: Boolean) = value = newState
	
	/**
	  * @return Whether this switch is currently off (value == false)
	  */
	def isOff = !isOn
	
	
	// IMPLEMENTED	-----------------
	
	override def stackId = hashCode()
	
	override protected def wrapped = label
	
	override def drawable = label
	
	override def updateLayout() = ()
	
	override def resetCachedSize() = ()
	
	override def isAttachedToMainHierarchy = _attached
	
	override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
	{
		if (newAttachmentStatus != _attached)
		{
			_attached = newAttachmentStatus
			if (newAttachmentStatus)
			{
				actorHandler += SwitchDrawer2
				valuePointer.addListener(StatusChangeListener)
				addMouseButtonListener(ClickHandler)
			}
			else
			{
				actorHandler -= SwitchDrawer2
				valuePointer.removeListener(StatusChangeListener)
				removeMouseListener(ClickHandler)
			}
			fireStackHierarchyChangeEvent(newAttachmentStatus)
		}
	}
	
	
	// NESTED CLASSES	-------------
	
	private object ClickHandler extends MouseButtonStateListener
	{
		// Only listens to left mouse button presses inside switch area
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter && MouseEvent.isOverAreaFilter(bounds)
		
		// When this switch is pressed, hanges its state
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			value = !value
			Some(ConsumeEvent("Switch activation"))
		}
		
		// Only enabled items are interactive
		override def allowsHandlingFrom(handlerType: HandlerType) = isEnabled
	}
	
	private object StatusChangeListener extends ChangeListener[Boolean]
	{
		override def onChangeEvent(event: ChangeEvent[Boolean]) = SwitchDrawer2.updateTarget(event.newValue)
	}
	
	private object SwitchDrawer2 extends CustomDrawer with Actor
	{
		// ATTRIBUTES	-------------
		
		private var currentAnimation: AnyAnimation[Double] = Animation.fixed(if (isOn) 1.0 else 0.0)
		private var currentProgress: Double = 1.0
		
		
		// COMPUTED	-----------------
		
		private def state = currentAnimation(currentProgress)
		
		
		// IMPLEMENTED	-------------
		
		override def opaque = false
		
		override def allowsHandlingFrom(handlerType: HandlerType) =
			if (handlerType == ActorHandlerType) currentProgress < 1.0 else true
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			val x = state
			
			// Calculates necessary bounds information
			val height = (bounds.size.width * Switch.maxHeightRatio) min bounds.size.height
			val r = height / 2 * 0.9
			
			val areaBounds = (bounds + Y(bounds.height - height) / 2).withHeight(height)
			val minX = areaBounds.position.x + height / 2
			val maxX = areaBounds.bottomRight.x - height / 2
			
			val circle = Circle(Point(minX + x * (maxX - minX), areaBounds.position.y + height / 2), r)
			
			// Determines the draw color
			val baseColor = if (isEnabled) color.timesSaturation(x) else color.timesSaturation(x).timesAlpha(0.55)
			val drawColor = if (isEnabled) knobColor else knobColor.timesAlpha(0.55)
			
			// Performs the actual drawing
			drawer.draw(areaBounds.toRoundedRectangle(1))(DrawSettings.onlyFill(baseColor))
			drawer.draw(circle)(DrawSettings.onlyFill(drawColor))
		}
		
		override def act(duration: FiniteDuration) = {
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
}
