package utopia.reflection.component.swing.display

import utopia.firmament.context.{ComponentCreationDefaults, ScrollingContext}
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.firmament.drawing.template.{CustomDrawer, ScrollBarDrawerLike}
import utopia.firmament.model.stack.StackSize
import utopia.firmament.model.stack.modifier.MaxOptimalSizeModifier
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.graphics.DrawLevel2.Normal
import utopia.genesis.graphics.Drawer
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.drawing.DrawableHandler
import utopia.genesis.handling.event.mouse._
import utopia.genesis.util.Fps
import utopia.genesis.view.RepaintLoop
import utopia.paradigm.color.Color
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.component.swing.template._
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackLeaf, ReflectionStackable}
import utopia.reflection.container.swing.Panel
import utopia.reflection.container.swing.layout.wrapper.scrolling.ScrollArea

import scala.concurrent.ExecutionContext

@deprecated("Deprecated for removal. If possible, please convert to using DrawableCanvas in Reach instead", "v2.1.2")
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
	               maxOptimalSize: Option[Size] = None)(implicit context: ScrollingContext) =
	{
		new ScrollCanvas(originalWorldSize, drawHandler, context.actorHandler, contentMouseButtonHandler,
			contentMouseMoveHandler, contentMouseWheelHandler, maxOptimalSize, context.scrollBarDrawer,
			context.scrollBarWidth, context.scrollPerWheelClick, context.scrollFriction, context.scrollBarIsInsideContent)
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
  * @param maxOptimalSize The maximum optimal size for this canvas (None if no maximum)
  * @param scrollBarDrawer An instance that draws the scroll bars
  * @param scrollBarWidth The width of the scroll bars (defaults to global default)
  * @param scrollPerWheelClick How many pixels (without scaling) each mouse wheel 'click' represents
  *                            (defaults to global default)
  * @param scrollFriction Friction applied to animated scrolling (pixels/s2) (defaults to global default)
  * @param scrollBarIsInsideContent Whether the scroll bar should be placed inside (true) or outside (false) of drawn
  *                                 content (default = false)
  */
@deprecated("Deprecated for removal. If possible, please convert to using DrawableCanvas in Reach instead", "v2.1.2")
class ScrollCanvas(originalWorldSize: Size, val drawHandler: DrawableHandler, actorHandler: ActorHandler,
                   val contentMouseButtonHandler: MouseButtonStateHandler, val contentMouseMoveHandler: MouseMoveHandler,
                   val contentMouseWheelHandler: MouseWheelHandler, maxOptimalSize: Option[Size],
                   scrollBarDrawer: ScrollBarDrawerLike, scrollBarWidth: Int = ComponentCreationDefaults.scrollBarWidth,
                   scrollPerWheelClick: Double = ComponentCreationDefaults.scrollAmountPerWheelClick,
                   scrollFriction: LinearAcceleration = ComponentCreationDefaults.scrollFriction,
                   scrollBarIsInsideContent: Boolean = false)
	extends StackableAwtComponentWrapperWrapper
{
	// ATTRIBUTES	------------------------
	
	private val canvas = new Canvas()
	private val scrollArea = new ScrollArea(canvas, actorHandler, scrollBarDrawer, scrollBarWidth, scrollPerWheelClick,
		scrollFriction, limitsToContentSize = true, scrollBarIsInsideContent)
	
	private val started = new VolatileFlag()
	
	private var _worldSize = originalWorldSize
	private var _scaling = 1.0
	
	
	// INITIAL CODE	------------------------
	
	maxOptimalSize.foreach { s => scrollArea.addConstraint(MaxOptimalSizeModifier(s)) }
	
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
	
	override protected def wrapped: ReflectionStackable with AwtComponentRelated = scrollArea
	
	
	// OTHER	----------------------------
	
	/**
	  * Starts continuously drawing this canvas in a background thread
	  * @param maxFPS The largest frames per second rate allowed (default = 60 Hrz)
	  * @param context The asynchronous execution context (implicit)
	  */
	def startDrawing(maxFPS: Fps = Fps.default)(implicit context: ExecutionContext, logger: Logger) = {
		if (started.set()) {
			canvas.addCustomDrawer(new CustomDraw())
			val repaintLoop = new RepaintLoop(canvas.component, maxFPS)
			repaintLoop.runAsync()
		}
	}
	
	
	// NESTED CLASSES	--------------------
	
	private class MouseEventHandler extends MouseButtonStateListener with MouseMoveListener with MouseWheelListener
	{
		// IMPLEMENTED	--------------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] = AcceptAll
		override def mouseMoveEventFilter: Filter[MouseMoveEvent] = AcceptAll
		override def mouseWheelEventFilter: Filter[MouseWheelEvent] = AcceptAll
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			contentMouseButtonHandler.onMouseButtonStateEvent(event.mapPosition { _.mapRelative(convertMousePosition) })
		}
		override def onMouseMove(event: MouseMoveEvent) =
			contentMouseMoveHandler.onMouseMove(event.mapPosition { _.mapRelative(convertMousePosition) })
		override def onMouseWheelRotated(event: MouseWheelEvent) =
			contentMouseWheelHandler.onMouseWheelRotated(event.mapPosition { _.mapRelative(convertMousePosition) })
		
		
		// OTHER	------------------------
		
		// Origin is at canvas origin. Scaling is also applied.
		private def convertMousePosition(original: Point) = (original - canvas.position) / scaling
	}
	
	private class CustomDraw extends CustomDrawer
	{
		override def drawLevel = Normal
		
		override def opaque = false
		
		// Draws the game world items with scaling
		override def draw(drawer: Drawer, bounds: Bounds) = {
			val scaledDrawer = if (_scaling == 1.0) drawer else drawer.scaled(_scaling)
			drawHandler.draw(scaledDrawer, drawHandler.drawBounds + bounds.position)
		}
	}
	
	private class Canvas extends AwtComponentWrapperWrapper with SwingComponentRelated with CachingReflectionStackable
		with MutableCustomDrawableWrapper with ReflectionStackLeaf
	{
		// ATTRIBUTES	--------------------
		
		private val panel = new Panel[ReflectionComponentLike with AwtComponentRelated]()
		
		
		// INITIAL CODE	--------------------
		
		background = Color.white
		
		
		// IMPLEMENTED	--------------------
		
		override def drawable = panel
		
		override def component = panel.component
		
		override protected def wrapped: JWrapper = panel
		
		override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].visible_=(visible)
		
		override def calculatedStackSize = StackSize.fixed(_worldSize * scaling)
		
		override def updateLayout() = ()
	}
}
