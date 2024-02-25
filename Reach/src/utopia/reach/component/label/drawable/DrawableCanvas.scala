package utopia.reach.component.label.drawable

import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Identity
import utopia.flow.util.Mutate
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.{DrawLevel, Drawer, Priority}
import utopia.genesis.handling.drawing.{CoordinateTransform, DrawableHandler, Repositioner}
import utopia.genesis.handling.event.consume.{Consumable, ConsumeChoice}
import utopia.genesis.handling.event.mouse._
import utopia.genesis.handling.template.{Handleable, Handlers, MutableHandlers}
import utopia.genesis.util.Fps
import utopia.paradigm.enumeration.FillAreaLogic.ScalePreservingShape
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponent

import scala.concurrent.ExecutionContext

/**
  * A component that displays a Drawable item (or items, in case of a DrawableHandler).
  * The drawn item will always be drawn to the (0,0) coordinates by default,
  * although certain aspect ratio -changes might affect this.
  *
  * @author Mikko Hilpinen
  * @since 09/02/2024, v1.3
  */
class DrawableCanvas(override val parentHierarchy: ComponentHierarchy, viewAreaPointer: Changing[Bounds],
                     scalingLogic: ScalePreservingShape, fpsLimits: Map[Priority, Fps] = Map(),
                     minSize: Size = Size.zero)
                    (implicit exc: ExecutionContext, log: Logger)
	extends ReachComponent with CoordinateTransform
{
	// ATTRIBUTES   ------------------------
	
	private val visualSizePointer = CopyOnDemand(sizePointer)
	
	/**
	  * Handler where the items drawn within this canvas should be placed
	  */
	val drawHandler = DrawableHandler.withClipPointer(viewAreaPointer)
		.withVisibilityPointer(parentHierarchy.linkPointer).withFpsLimits(fpsLimits).empty
	private val wrapper = new Repositioner(drawHandler, Left(Fixed(Point.origin), visualSizePointer), scalingLogic)
	
	private val relativeMouseButtonHandler = MouseButtonStateHandler.empty
	private val relativeMouseMoveHandler = MouseMoveHandler.empty
	private val relativeMouseWheelHandler = MouseWheelHandler.empty
	private val relativeMouseHandlers =
		Handlers(relativeMouseButtonHandler, relativeMouseMoveHandler, relativeMouseWheelHandler)
	/**
	  * Handlers that apply in the "view world" context. Mutable.
	  * Mouse events are transformed to the view-world coordinates (instead of component coordinates)
	  */
	val viewHandlers = MutableHandlers.empty
	
	
	// INITIAL CODE ------------------------
	
	// When the viewed region changes, repaints
	// If the area size changes, also updates the stack size
	viewAreaPointer.addListener { event =>
		if (event.values.isSymmetricBy { _.size })
			repaint()
		else
			revalidate()
	}
	
	// Relays the repaint requests to the component hierarchy
	wrapper.addRepaintListener { (_, region, prio) =>
		region match {
			case Some(region) => repaintArea(region, prio)
			case None => repaint(prio)
		}
	}
	
	// Distributes relativized mouse events to the Repositioner, which relays them to other components
	viewHandlers.addHandlers(wrapper.setupMouseEvents(relativeMouseHandlers, disableMouseToWrapped = true))
	
	
	// IMPLEMENTED  ------------------------
	
	override def toView[V <: DoubleVectorLike[V]](p: V) = wrapper.toView(p)
	override def view[V <: DoubleVectorLike[V]](viewPoint: V) = wrapper.view(viewPoint)
	
	override def calculatedStackSize = StackSize(minSize, wrapper.drawBounds.size, None)
	
	override def transparent = !scalingLogic.fillsTargetArea || !wrapper.opaque
	
	// Matches the visual size with the actual size of this component
	override def updateLayout() = visualSizePointer.update()
	
	override def paintContent(drawer: Drawer, drawLevel: DrawLevel, clipZone: Option[Bounds]) = {
		// Draws the content on the normal draw level
		if (drawLevel == DrawLevel.Normal) {
			// Always clips to this component's bounds, at least
			val clip = clipZone match {
				case Some(clip) => bounds.overlapWith(clip)
				case None => Some(bounds)
			}
			clip.foreach { clip => wrapper.draw(drawer.clippedToBounds(clip), bounds) }
		}
	}
	
	@deprecated("Mouse events are set up automatically")
	override def setupMouseEvents(parentHandlers: Handlers, disableMouseToWrapped: Boolean) = viewHandlers
	
	// Distributes relativized copies of the received mouse events to the view handlers
	override def distributeMouseButtonEvent(event: MouseButtonStateEvent) =
		distributeConsumableMouseEvent(event, mouseButtonHandler,
			relativeMouseButtonHandler) { _ onMouseButtonStateEvent _ }
	override def distributeMouseWheelEvent(event: MouseWheelEvent) =
		distributeConsumableMouseEvent(event, mouseWheelHandler, relativeMouseWheelHandler) { _ onMouseWheelRotated _ }
	
	override def distributeMouseMoveEvent(event: MouseMoveEvent) = {
		if (mouseMoveHandler.mayBeHandled)
			mouseMoveHandler.onMouseMove(event)
		if (relativeMouseMoveHandler.mayBeHandled)
			relativeMouseMoveHandler.onMouseMove(relativize(event))
	}
	
	
	// OTHER    ----------------------------
	
	private def distributeConsumableMouseEvent[E <: MouseEvent[E] with Consumable[E], L <: Handleable]
	(event: E, primaryHandler: L, relativeHandler: L)(deliver: (L, E) => ConsumeChoice) =
	{
		event.transformAndDistribute[L, E](Pair[(L, Mutate[E])](
			primaryHandler -> Identity, relativeHandler -> relativize)
			.filter { _._1.mayBeHandled })(deliver)._2
	}
	
	private def relativize[E <: MouseEvent[E]](event: E) = event.relativeTo(position)
}
