package utopia.reach.component.label.drawable

import utopia.genesis.graphics.DrawLevel2
import utopia.firmament.model.stack.StackSize
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.{Drawer, Priority2}
import utopia.genesis.handling.drawing.{DrawableHandler2, Repositioner}
import utopia.genesis.util.Fps
import utopia.paradigm.enumeration.FillAreaLogic.ScalePreservingShape
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
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
                     scalingLogic: ScalePreservingShape, fpsLimits: Map[Priority2, Fps] = Map(),
                     minSize: Size = Size.zero)
                    (implicit exc: ExecutionContext, log: Logger)
	extends ReachComponent
{
	// ATTRIBUTES   ------------------------
	
	private val visualSizePointer = CopyOnDemand(sizePointer)
	
	private val drawHandler = DrawableHandler2.withClipPointer(viewAreaPointer)
		.withVisibilityPointer(parentHierarchy.linkPointer).withFpsLimits(fpsLimits).empty
	private val wrapper = new Repositioner(drawHandler, Left(Fixed(Point.origin), visualSizePointer), scalingLogic)
	
	
	// INITIAL CODE ------------------------
	
	// Distributes mouse events to the Repositioner, which relays them to other components
	// TODO: Add mouse listeners (requires Reach refactoring)
	
	
	// IMPLEMENTED  ------------------------
	
	override def calculatedStackSize = StackSize(minSize, wrapper.drawBounds.size, None)
	
	override def transparent = !scalingLogic.fillsTargetArea || !wrapper.opaque
	
	// Matches the visual size with the actual size of this component
	override def updateLayout() = visualSizePointer.update()
	
	override def paintContent(drawer: Drawer, drawLevel: DrawLevel2, clipZone: Option[Bounds]) = {
		// Draws the content on the normal draw level
		if (drawLevel == DrawLevel2.Normal) {
			// Always clips to this component's bounds, at least
			val clip = clipZone match {
				case Some(clip) => bounds.overlapWith(clip)
				case None => Some(bounds)
			}
			clip.foreach { clip => wrapper.draw(drawer.clippedToBounds(clip), bounds) }
		}
	}
}
