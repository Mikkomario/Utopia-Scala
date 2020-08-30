package utopia.reflection.component.swing.input

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.event.{ChangeEvent, ChangeListener}
import utopia.genesis.animation.Animation
import utopia.genesis.color.Color
import utopia.genesis.event.{ConsumeEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling.{Actor, ActorHandlerType, MouseButtonStateListener}
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point}
import utopia.genesis.util.Drawer
import utopia.inception.handling.HandlerType
import utopia.reflection.color.TextColorStandard.{Dark, Light}
import utopia.reflection.component.context.{AnimationContextLike, ColorContextLike}
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.template.input.InteractionWithPointer
import utopia.reflection.component.template.layout.stack.Stackable
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.component.swing.template.AwtComponentWrapperWrapper
import utopia.reflection.event.StackHierarchyListener
import utopia.reflection.shape.stack.{StackLength, StackSize}
import utopia.reflection.util.ComponentCreationDefaults

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
				  (implicit context: ColorContextLike, animationContext: AnimationContextLike) =
	{
		val knobColor = context.containerBackground.textColorStandard match
		{
			case Light => Color.white
			case Dark => Color.black
		}
		new Switch(animationContext.actorHandler, width, context.secondaryColor, knobColor,
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
	extends AwtComponentWrapperWrapper with CustomDrawableWrapper with InteractionWithPointer[Boolean] with Stackable
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
				removeListener(ClickHandler)
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
		
		private var currentAnimation: Animation[Double] = Animation.fixed(if (isOn) 1.0 else 0.0)
		private var currentProgress: Double = 1.0
		
		
		// COMPUTED	-----------------
		
		private def state = currentAnimation(currentProgress)
		
		
		// IMPLEMENTED	-------------
		
		override def allowsHandlingFrom(handlerType: HandlerType) =
			if (handlerType == ActorHandlerType) currentProgress < 1.0 else true
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			val x = state
			
			// Calculates necessary bounds information
			val height = (bounds.size.width * Switch.maxHeightRatio) min bounds.size.height
			val r = height / 2 * 0.9
			
			val areaBounds = bounds.translated(0, (bounds.height - height) / 2).withHeight(height)
			val minX = areaBounds.position.x + height / 2
			val maxX = areaBounds.bottomRight.x - height / 2
			
			val circle = Circle(Point(minX + x * (maxX - minX), areaBounds.position.y + height / 2), r)
			
			// Determines the draw color
			val baseColor = if (isEnabled) color.timesSaturation(x) else color.timesSaturation(x).timesAlpha(0.55)
			val drawColor = if (isEnabled) knobColor else knobColor.timesAlpha(0.55)
			
			// Performs the actual drawing
			drawer.noEdges.disposeAfter
			{
				d =>
					d.withFillColor(baseColor).draw(areaBounds.toRoundedRectangle(1))
					d.withFillColor(drawColor).draw(circle)
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
}
