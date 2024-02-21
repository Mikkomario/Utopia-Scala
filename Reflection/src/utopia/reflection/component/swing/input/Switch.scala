package utopia.reflection.component.swing.input

import utopia.firmament.component.input.InteractionWithPointer
import utopia.firmament.context.{AnimationContext, ColorContext, ComponentCreationDefaults}
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.model.stack.{StackLength, StackSize}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.DrawLevel2.Normal
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent2, MouseButtonStateListener2, MouseEvent2}
import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.AnimationLike.AnyAnimation
import utopia.paradigm.color.Color
import utopia.paradigm.color.ColorShade.{Dark, Light}
import utopia.paradigm.enumeration.Axis.Y
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.event.StackHierarchyListener

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
	override val valuePointer = new EventfulPointer(initialState)
	
	override var stackHierarchyListeners = Vector[StackHierarchyListener]()
	
	
	// INITIAL CODE	-----------------
	
	addCustomDrawer(SwitchDrawer2)
	label.setHandCursor()
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return Whether this switch is currently enabled (true) or disabled (false)
	  */
	def enabled = _enabled
	def enabled_=(newState: Boolean) = {
		_enabled = newState
		if (newState) label.setHandCursor() else label.setArrowCursor()
	}
	
	/**
	  * @return Whether this switch is currently enabled (true) or disabled (false)
	  */
	@deprecated("Replaced with .enabled", "v2.1.1")
	def isEnabled = enabled
	
	/**
	  * @return Whether this switch is currently on (value == true)
	  */
	def isOn = value
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
		if (newAttachmentStatus != _attached) {
			_attached = newAttachmentStatus
			if (newAttachmentStatus) {
				actorHandler += SwitchDrawer2
				valuePointer.addListener(StatusChangeListener)
				addMouseButtonListener(ClickHandler)
			}
			else {
				actorHandler -= SwitchDrawer2
				valuePointer.removeListener(StatusChangeListener)
				removeMouseListener(ClickHandler)
			}
			fireStackHierarchyChangeEvent(newAttachmentStatus)
		}
	}
	
	
	// NESTED CLASSES	-------------
	
	private object ClickHandler extends MouseButtonStateListener2
	{
		// Only listens to left mouse button presses inside switch area
		override val mouseButtonStateEventFilter =
			MouseButtonStateEvent2.filter.leftPressed && MouseEvent2.filter.over(bounds)
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		// When this switch is pressed, changes its state
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2) = {
			// Only enabled items are interactive
			if (enabled) {
				value = !value
				Consume("Switch activation")
			}
			else
				Preserve
		}
	}
	
	private object StatusChangeListener extends ChangeListener[Boolean]
	{
		override def onChangeEvent(event: ChangeEvent[Boolean]) = SwitchDrawer2.updateTarget(event.newValue)
	}
	
	private object SwitchDrawer2 extends CustomDrawer with Actor
	{
		// ATTRIBUTES	-------------
		
		private val progressPointer = Volatile(1.0)
		override val handleCondition: FlagLike = progressPointer.map { _ < 1.0 }
		
		private var currentAnimation: AnyAnimation[Double] = Animation.fixed(if (isOn) 1.0 else 0.0)
		
		
		// COMPUTED	-----------------
		
		private def state = currentAnimation(progressPointer.value)
		
		
		// IMPLEMENTED	-------------
		
		override def opaque = false
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) = {
			val x = state
			
			// Calculates necessary bounds information
			val height = (bounds.size.width * Switch.maxHeightRatio) min bounds.size.height
			val r = height / 2 * 0.9
			
			val areaBounds = (bounds + Y(bounds.height - height) / 2).withHeight(height)
			val minX = areaBounds.position.x + height / 2
			val maxX = areaBounds.bottomRight.x - height / 2
			
			val circle = Circle(Point(minX + x * (maxX - minX), areaBounds.position.y + height / 2), r)
			
			// Determines the draw color
			val baseColor = if (enabled) color.timesSaturation(x) else color.timesSaturation(x).timesAlpha(0.55)
			val drawColor = if (enabled) knobColor else knobColor.timesAlpha(0.55)
			
			// Performs the actual drawing
			drawer.draw(areaBounds.toRoundedRectangle(1))(DrawSettings.onlyFill(baseColor))
			drawer.draw(circle)(DrawSettings.onlyFill(drawColor))
		}
		
		override def act(duration: FiniteDuration) = {
			val increment = duration / animationDuration
			progressPointer.update { p => (p + increment) min 1.0 }
			repaint()
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
}
