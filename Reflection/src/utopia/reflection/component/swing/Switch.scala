package utopia.reflection.component.swing

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.util.TimeExtensions._
import utopia.genesis.color.Color
import utopia.genesis.event.{ConsumeEvent, MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.{Actor, MouseButtonStateListener}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.shape2D.{Bounds, Circle, Point}
import utopia.genesis.util.Drawer
import utopia.inception.handling.{HandlerType, Mortal}
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.input.InteractionWithPointer
import utopia.reflection.component.stack.StackLeaf
import utopia.reflection.component.swing.label.EmptyLabel
import utopia.reflection.shape.{StackLength, StackSize}
import utopia.reflection.util.ComponentContext

import scala.concurrent.duration.FiniteDuration

object Switch
{
	private val animationDuration = 100.millis
	private val maxHeightRatio = 0.5
	
	/**
	  * Creates a new switch using contextual information
	  * @param context Component creation context
	  * @return A new switch
	  */
	def contextual(implicit context: ComponentContext) = new Switch(context.switchWidth, context.highlightColor,
		context.actorHandler)
}

/**
  * Switches are used for setting a value either on or off
  * @author Mikko Hilpinen
  * @since 4.5.2019, v1+
  */
class Switch(val targetWidth: StackLength, val color: Color, actorHandler: ActorHandler)
	extends AwtComponentWrapperWrapper with CustomDrawableWrapper with InteractionWithPointer[Boolean] with StackLeaf
{
	// ATTRIBUTES	-----------------
	
	private val label = new EmptyLabel()
	
	// private var _value = false
	private var _isEnabled = true
	
	override val stackSize = StackSize(targetWidth, targetWidth.noMax * Switch.maxHeightRatio)
	override val valuePointer = new PointerWithEvents(false)
	
	
	// INITIAL CODE	-----------------
	
	{
		// Sets up custom drawing
		val drawer = new SwitchDrawer()
		addCustomDrawer(drawer)
		actorHandler += drawer
		
		// Sets up mouse listening
		addMouseButtonListener(new ClickHandler())
		label.setHandCursor()
	}
	
	
	// COMPUTED	---------------------
	
	def isEnabled = _isEnabled
	def isEnabled_=(enabled: Boolean) =
	{
		_isEnabled = enabled
		if (enabled) label.setHandCursor() else label.setArrowCursor()
	}
	
	def isOn = value
	def isOn_=(newState: Boolean) = value = newState
	
	def isOff = !isOn
	
	
	// IMPLEMENTED	-----------------
	
	override def stackId = hashCode()
	
	override protected def wrapped = label
	
	override def drawable = label
	
	override def updateLayout() = Unit
	
	override def resetCachedSize() = Unit
	
	
	// NESTED CLASSES	-------------
	
	private class ClickHandler extends MouseButtonStateListener
	{
		// Only listens to left mouse button presses inside switch area
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.leftPressedFilter && MouseEvent.isOverAreaFilter(bounds)
		
		// When this switch is pressed, hanges its state
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			value = !value
			Some(ConsumeEvent("Switch activation"))
		}
		
		override def parent = None
		
		// Only enabled items are interactive
		override def allowsHandlingFrom(handlerType: HandlerType) = isEnabled
	}
	
	private class SwitchDrawer extends CustomDrawer with Actor with Mortal
	{
		// ATTRIBUTES	-------------
		
		private var wasDisplayed = component.isDisplayable
		private var x = 0.0
		
		
		// IMPLEMENTED	-------------
		
		override def drawLevel = Normal
		
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			// Calculates necessary bounds information
			val height = (bounds.size.width * Switch.maxHeightRatio) min bounds.size.height
			val r = height / 2 * 0.9
			
			val areaBounds = bounds.translated(0, (bounds.height - height) / 2).withHeight(height)
			val minX = areaBounds.position.x + height / 2
			val maxX = areaBounds.bottomRight.x - height / 2
			
			val circle = Circle(Point(minX + x * (maxX - minX), areaBounds.position.y + height / 2), r)
			
			// Determines the draw color
			val baseColor = if (isEnabled) color.timesSaturation(x) else color.timesSaturation(x).timesAlpha(0.55)
			val knobColor = if (isEnabled) Color.white else Color.white.timesAlpha(0.55)
			
			// Performs the actual drawing
			drawer.noEdges.disposeAfter
			{
				d =>
					d.withFillColor(baseColor).draw(areaBounds.toRoundedRectangle(1))
					d.withFillColor(knobColor).draw(circle)
			}
		}
		
		override def act(duration: FiniteDuration) =
		{
			val shouldUpdate =
			{
				// First checks until the component becomes displayed
				if (!wasDisplayed)
				{
					if (component.isDisplayable)
					{
						wasDisplayed = true
						true
					}
					else
						false
				}
				else
					true
			}
			
			if (shouldUpdate)
			{
				// Updates animation, if necessary
				val transition = duration / Switch.animationDuration
				if (isOn)
					x = (x + transition) min 1.0
				else
					x = (x - transition) max 0.0
				
				repaint()
			}
		}
		
		// Dies after component is no longer displayable
		override def isDead = wasDisplayed && !component.isDisplayable
		
		override def parent = None
		
		// Only allows handling when update is required
		override def allowsHandlingFrom(handlerType: HandlerType) = if (isOn) x < 1 else x > 0
	}
}
