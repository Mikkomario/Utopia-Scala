package utopia.reflection.component.swing

import utopia.flow.async.VolatileFlag
import utopia.genesis.color.Color
import utopia.genesis.event.{MouseButtonStateEvent, MouseMoveEvent, MouseWheelEvent}
import utopia.genesis.handling.{DrawableHandler, MouseButtonStateHandler, MouseButtonStateListener, MouseMoveHandler, MouseMoveListener, MouseWheelHandler, MouseWheelListener}
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.shape.LinearAcceleration
import utopia.genesis.shape.shape2D.{Bounds, Point, Size, Transformation}
import utopia.genesis.util.{Drawer, FPS}
import utopia.genesis.view.RepaintLoop
import utopia.inception.handling.immutable.Handleable
import utopia.reflection.component.ComponentLike
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.stack.{CachingStackable, StackLeaf, Stackable}
import utopia.reflection.container.stack.ScrollBarDrawer
import utopia.reflection.container.swing.{Panel, ScrollArea}
import utopia.reflection.shape.{StackLengthLimit, StackSize}
import utopia.reflection.util.ComponentContext

import scala.concurrent.ExecutionContext

object ScrollCanvas
{
	/**
	  * Creates a new scroll canvas using component creation context
	  * @param originalWorldSize The initial size of the drawn world
	  * @param drawHandler A handler that draws all the content in this canvas
	  * @param contentMouseButtonHandler A mouse button handler that is used by the content drawn in this canvas
	  * @param contentMouseMoveHandler A mouse move handler that is used by the content drawn in this canvas
	  * @param contentMouseWheelHandler A mouse wheel handler that is used by the content drawn in this canvas
	  * @param maxOptimalSize The maximum optimal size for this canvas (None if no maximum) (default = None)
	  * @param context Component creation context (implicit)
	  * @return A new scroll canvas
	  */
	def contextual(originalWorldSize: Size, drawHandler: DrawableHandler, contentMouseButtonHandler: MouseButtonStateHandler,
				   contentMouseMoveHandler: MouseMoveHandler, contentMouseWheelHandler: MouseWheelHandler,
				   maxOptimalSize: Option[Size] = None)(implicit context: ComponentContext) =
	{
		new ScrollCanvas(originalWorldSize, drawHandler, context.actorHandler, contentMouseButtonHandler,
			contentMouseMoveHandler, contentMouseWheelHandler, context.scrollPerWheelClick, context.scrollBarDrawer,
			context.scrollBarWidth, context.scrollBarIsInsideContent, context.scrollFriction, maxOptimalSize)
	}
}

/**
  * This canvas uses scroll views to make the whole content available. All of the content is custom drawn using a
  * drawable handler.
  * @author Mikko Hilpinen
  * @since 9.5.2019, v1+
  * @param originalWorldSize The initial size of the drawn world
  * @param drawHandler A handler that draws all the content in this canvas
  * @param actorHandler An actorHandler for drag scrolling
  * @param contentMouseButtonHandler A mouse button handler that is used by the content drawn in this canvas
  * @param contentMouseMoveHandler A mouse move handler that is used by the content drawn in this canvas
  * @param contentMouseWheelHandler A mouse wheel handler that is used by the content drawn in this canvas
  * @param scrollPerWheelClick How many pixels (without scaling) each mouse wheel 'click' represents
  * @param scrollBarDrawer An instance that draws the scroll bars
  * @param scrollBarWidth The width of the scroll bars
  * @param scrollBarIsInsideContent Whether the scroll bar should be placed inside (true) or outside (false) of drawn content
  * @param scrollFriction Friction applied to animated scrolling (pixels/s2)
  * @param maxOptimalSize The maximum optimal size for this canvas (None if no maximum)
  */
class ScrollCanvas(originalWorldSize: Size, val drawHandler: DrawableHandler, actorHandler: ActorHandler,
				   val contentMouseButtonHandler: MouseButtonStateHandler, val contentMouseMoveHandler: MouseMoveHandler,
				   val contentMouseWheelHandler: MouseWheelHandler, scrollPerWheelClick: Double,
				   scrollBarDrawer: ScrollBarDrawer, scrollBarWidth: Int, scrollBarIsInsideContent: Boolean,
				   scrollFriction: LinearAcceleration, maxOptimalSize: Option[Size]) extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES	------------------------
	
	private val canvas = new Canvas()
	private val scrollArea = new ScrollArea(canvas, actorHandler, scrollPerWheelClick, scrollBarDrawer, scrollBarWidth,
		scrollBarIsInsideContent, scrollFriction, StackLengthLimit.sizeLimit(maxOptimal = maxOptimalSize), limitsToContentSize = true)
	
	private val started = new VolatileFlag()
	
	private var _worldSize = originalWorldSize
	private var _scaling = 1.0
	
	
	// INITIAL CODE	------------------------
	
	{
		// Adds mouse event handling
		val mouseListener = new MouseEventHandler()
		canvas.addMouseButtonListener(mouseListener)
		canvas.addMouseMoveListener(mouseListener)
		canvas.addMouseWheelListener(mouseListener)
	}
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return The current size of the game world
	  */
	def worldSize = _worldSize
	def worldSize_=(newSize: Size) =
	{
		_worldSize = newSize
		canvas.revalidate()
	}
	
	/**
	  * @return The current scaling / zoom factor for the game world
	  */
	def scaling = _scaling
	def scaling_=(newScaling: Double) =
	{
		_scaling = newScaling
		canvas.revalidate()
	}
	
	
	// IMPLEMENTED	------------------------
	
	override protected def wrapped: Stackable with AwtComponentRelated = scrollArea
	
	
	// OTHER	----------------------------
	
	/**
	  * Starts continuously drawing this canvas in a background thread
	  * @param maxFPS The largest frames per second rate allowed (default = 60 Hrz)
	  * @param context The asynchronous execution context (implicit)
	  */
	def startDrawing(maxFPS: FPS = FPS.default)(implicit context: ExecutionContext) = started.runAndSet
	{
		canvas.addCustomDrawer(new CustomDraw())
		
		val repaintLoop = new RepaintLoop(canvas.component, maxFPS)
		repaintLoop.registerToStopOnceJVMCloses()
		repaintLoop.startAsync()
	}
	
	
	// NESTED CLASSES	--------------------
	
	private class MouseEventHandler extends MouseButtonStateListener with MouseMoveListener with MouseWheelListener with Handleable
	{
		// IMPLEMENTED	--------------------
		
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			contentMouseButtonHandler.onMouseButtonState(
				event.copy(mousePosition = convertMousePosition(event.mousePosition)))
		}
		
		override def onMouseMove(event: MouseMoveEvent) = contentMouseMoveHandler.onMouseMove(event.copy(
			mousePosition = convertMousePosition(event.mousePosition),
			previousMousePosition = convertMousePosition(event.previousMousePosition)))
		
		override def onMouseWheelRotated(event: MouseWheelEvent) =
		{
			contentMouseWheelHandler.onMouseWheelRotated(
				event.copy(mousePosition = convertMousePosition(event.mousePosition)))
		}
		
		
		// OTHER	------------------------
		
		// Origin is at canvas origin. Scaling is also applied.
		private def convertMousePosition(original: Point) = (original - canvas.position) / scaling
	}
	
	private class CustomDraw extends CustomDrawer
	{
		override def drawLevel = Normal
		
		// Draws the game world items with scaling
		override def draw(drawer: Drawer, bounds: Bounds) =
		{
			val scaledDrawer = if (_scaling == 1.0) drawer else drawer.transformed(Transformation.scaling(_scaling))
			drawHandler.draw(scaledDrawer)
		}
	}
	
	private class Canvas extends AwtComponentWrapperWrapper with SwingComponentRelated with CachingStackable
		with CustomDrawableWrapper with StackLeaf
	{
		// ATTRIBUTES	--------------------
		
		private val panel = new Panel[ComponentLike with AwtComponentRelated]()
		
		
		// INITIAL CODE	--------------------
		
		background = Color.white
		
		
		// IMPLEMENTED	--------------------
		
		override def drawable = panel
		
		override def component = panel.component
		
		override protected def wrapped: JWrapper = panel
		
		override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].isVisible_=(visible)
		
		override def calculatedStackSize = StackSize.fixed(_worldSize * scaling)
		
		override def updateLayout() = Unit
	}
}
