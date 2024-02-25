package utopia.reach.component.label.drawable

import utopia.firmament.context.BaseContext
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
import utopia.paradigm.enumeration.FillAreaLogic
import utopia.paradigm.enumeration.FillAreaLogic.{Fit, ScalePreservingShape}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.reach.component.factory.{ComponentFactoryFactory, FromContextComponentFactoryFactory, FromContextFactory}
import utopia.reach.component.factory.contextual.BaseContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{PartOfComponentHierarchy, ReachComponent}

import scala.concurrent.ExecutionContext

/**
  * Common trait for drawable canvas factories and settings
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 25.02.2024, v1.3
  */
trait DrawableCanvasSettingsLike[+Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Logic used for scaling the draw output when the component size doesn't match view area size.
	  */
	def scalingLogic: ScalePreservingShape
	/**
	  * Repaint frequency-limits assigned for specific draw-priorities
	  */
	def fpsLimits: Map[Priority, Fps]
	/**
	  * Minimum size assigned to the created component
	  */
	def minSize: Size
	
	/**
	  * Repaint frequency-limits assigned for specific draw-priorities
	  * @param limits New fps limits to use.
	  * Repaint frequency-limits assigned for specific draw-priorities
	  * @return Copy of this factory with the specified fps limits
	  */
	def withFpsLimits(limits: Map[Priority, Fps]): Repr
	/**
	  * Minimum size assigned to the created component
	  * @param size New min size to use.
	  * Minimum size assigned to the created component
	  * @return Copy of this factory with the specified min size
	  */
	def withMinSize(size: Size): Repr
	/**
	  * Logic used for scaling the draw output when the component size doesn't match view area size.
	  * @param logic New scaling logic to use.
	  * Logic used for scaling the draw output when the component size doesn't match view area size.
	  * @return Copy of this factory with the specified scaling logic
	  */
	def withScalingLogic(logic: ScalePreservingShape): Repr
	
	
	// OTHER	--------------------
	
	def mapFpsLimits(f: Map[Priority, Fps] => Map[Priority, Fps]) = withFpsLimits(f(fpsLimits))
	def mapMinSize(f: Size => Size) = withMinSize(f(minSize))
}

object DrawableCanvasSettings
{
	// ATTRIBUTES	--------------------
	
	val default = apply()
}

/**
  * Combined settings used when constructing drawable canvases
  * @param scalingLogic Logic used for scaling the draw output when the component size doesn't match view
  *  area size.
  * @param fpsLimits Repaint frequency-limits assigned for specific draw-priorities
  * @param minSize Minimum size assigned to the created component
  * @author Mikko Hilpinen
  * @since 25.02.2024, v1.3
  */
case class DrawableCanvasSettings(scalingLogic: ScalePreservingShape = FillAreaLogic.Fit,
                                  fpsLimits: Map[Priority, Fps] = Map(), minSize: Size = Size.zero)
	extends DrawableCanvasSettingsLike[DrawableCanvasSettings]
{
	// IMPLEMENTED	--------------------
	
	override def withFpsLimits(limits: Map[Priority, Fps]) = copy(fpsLimits = limits)
	override def withMinSize(size: Size) = copy(minSize = size)
	override def withScalingLogic(logic: ScalePreservingShape) = copy(scalingLogic = logic)
}

/**
  * Common trait for factories that wrap a drawable canvas settings instance
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 25.02.2024, v1.3
  */
trait DrawableCanvasSettingsWrapper[+Repr] extends DrawableCanvasSettingsLike[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Settings wrapped by this instance
	  */
	protected def settings: DrawableCanvasSettings
	/**
	  * @return Copy of this factory with the specified settings
	  */
	def withSettings(settings: DrawableCanvasSettings): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def fpsLimits = settings.fpsLimits
	override def minSize = settings.minSize
	override def scalingLogic = settings.scalingLogic
	
	override def withFpsLimits(limits: Map[Priority, Fps]) = mapSettings { _.withFpsLimits(limits) }
	override def withMinSize(size: Size) = mapSettings { _.withMinSize(size) }
	override def withScalingLogic(logic: ScalePreservingShape) = mapSettings { _.withScalingLogic(logic) }
	
	
	// OTHER	--------------------
	
	def mapSettings(f: DrawableCanvasSettings => DrawableCanvasSettings) = withSettings(f(settings))
}

/**
  * Common trait for factories that are used for constructing drawable canvases
  * @tparam Repr Implementing factory/settings type
  * @author Mikko Hilpinen
  * @since 25.02.2024, v1.3
  */
trait DrawableCanvasFactoryLike[+Repr]
	extends DrawableCanvasSettingsWrapper[Repr] with PartOfComponentHierarchy
{
	// ABSTRACT ------------------------
	
	/**
	  * @param viewAreaPointer A pointer that contains the viewed "view world" area
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @return A new canvas component for presenting Drawable items
	  */
	def apply(viewAreaPointer: Changing[Bounds])(implicit exc: ExecutionContext, log: Logger): DrawableCanvas
	
	
	// OTHER    ------------------------
	
	protected def _apply(viewAreaPointer: Changing[Bounds])(implicit exc: ExecutionContext, log: Logger) =
		new DrawableCanvas(parentHierarchy, viewAreaPointer, scalingLogic, fpsLimits, minSize)
}

/**
  * Factory class used for constructing drawable canvases using contextual component creation information
  * @author Mikko Hilpinen
  * @since 25.02.2024, v1.3
  */
case class ContextualDrawableCanvasFactory(parentHierarchy: ComponentHierarchy, context: BaseContext,
                                           settings: DrawableCanvasSettings = DrawableCanvasSettings.default)
	extends DrawableCanvasFactoryLike[ContextualDrawableCanvasFactory]
		with BaseContextualFactory[ContextualDrawableCanvasFactory]
{
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override def withContext(context: BaseContext) = copy(context = context)
	override def withSettings(settings: DrawableCanvasSettings) =
		copy(settings = settings)
	
	override def apply(viewAreaPointer: Changing[Bounds])(implicit exc: ExecutionContext, log: Logger) = {
		val canvas = _apply(viewAreaPointer)
		// Adds the actor handler to the view handlers
		canvas.viewHandlers.addHandler(context.actorHandler)
		canvas
	}
}

/**
  * Factory class that is used for constructing drawable canvases without using contextual information
  * @author Mikko Hilpinen
  * @since 25.02.2024, v1.3
  */
case class DrawableCanvasFactory(parentHierarchy: ComponentHierarchy,
                                 settings: DrawableCanvasSettings = DrawableCanvasSettings.default)
	extends DrawableCanvasFactoryLike[DrawableCanvasFactory]
		with FromContextFactory[BaseContext, ContextualDrawableCanvasFactory]
{
	// IMPLEMENTED	--------------------
	
	override def withContext(context: BaseContext) =
		ContextualDrawableCanvasFactory(parentHierarchy, context, settings)
	
	override def withSettings(settings: DrawableCanvasSettings) = copy(settings = settings)
	
	override def apply(viewAreaPointer: Changing[Bounds])(implicit exc: ExecutionContext, log: Logger) =
		_apply(viewAreaPointer)
}

/**
  * Used for defining drawable canvas creation settings outside of the component building process
  * @author Mikko Hilpinen
  * @since 25.02.2024, v1.3
  */
case class DrawableCanvasSetup(settings: DrawableCanvasSettings = DrawableCanvasSettings.default)
	extends DrawableCanvasSettingsWrapper[DrawableCanvasSetup]
		with ComponentFactoryFactory[DrawableCanvasFactory]
		with FromContextComponentFactoryFactory[BaseContext, ContextualDrawableCanvasFactory]
{
	// IMPLEMENTED	--------------------
	
	override def apply(hierarchy: ComponentHierarchy) = DrawableCanvasFactory(hierarchy, settings)
	
	override def withContext(hierarchy: ComponentHierarchy, context: BaseContext) =
		ContextualDrawableCanvasFactory(hierarchy, context, settings)
	override def withSettings(settings: DrawableCanvasSettings) = copy(settings = settings)
}

object DrawableCanvas extends DrawableCanvasSetup()
{
	// OTHER	--------------------
	
	def apply(settings: DrawableCanvasSettings) = withSettings(settings)
}

/**
  * A component that displays a Drawable item (or items, in case of a DrawableHandler).
  * The drawn item will always be drawn to the (0,0) coordinates by default,
  * although certain aspect ratio -changes might affect this.
  *
  * @author Mikko Hilpinen
  * @since 09/02/2024, v1.3
  */
class DrawableCanvas(override val parentHierarchy: ComponentHierarchy, viewAreaPointer: Changing[Bounds],
                     scalingLogic: ScalePreservingShape = Fit, fpsLimits: Map[Priority, Fps] = Map(),
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
	val viewHandlers = MutableHandlers(drawHandler)
	
	
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
		// Repaint requests are relative to the drawBounds while parent requests are relative to this component's bounds
		val actualRegion = Bounds(Point.origin, size).overlapWith(region match {
			case Some(r) => r + wrapper.drawBounds.position
			case None => wrapper.drawBounds
		})
		actualRegion.foreach { b => repaintArea(b.ceil, prio) }
	}
	
	// Distributes relativized mouse events to the Repositioner, which relays them to other components
	viewHandlers.addHandlers(wrapper.setupMouseEvents(relativeMouseHandlers, disableMouseToWrapped = true))
	
	
	// IMPLEMENTED  ------------------------
	
	override def toView[V <: DoubleVectorLike[V]](p: V) = wrapper.toView(p)
	override def view[V <: DoubleVectorLike[V]](viewPoint: V) = wrapper.view(viewPoint)
	
	override def calculatedStackSize = StackSize(minSize, viewAreaPointer.value.size, None)
	
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
