package utopia.reach.container.layered

import utopia.firmament.component.container.many.MultiContainer
import utopia.firmament.component.stack.StackSizeCalculating
import utopia.flow.collection.CollectionExtensions._
import utopia.genesis.graphics.Drawer
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.container.layered.LayerPositioning.{AlignedToSide, AnchoredTo, Free}
import utopia.firmament.drawing.template.DrawLevel.{Background, Foreground, Normal}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackSize

/**
  * A template trait for containers which hold multiple overlaid layers of components
  * @author Mikko Hilpinen
  * @since 18.4.2021, v1.0
  */
trait LayeredViewLike[+C <: ReachComponentLike] extends ReachComponentLike with MultiContainer[C]
	with StackSizeCalculating
{
	// ABSTRACT -----------------------------
	
	protected def mainLayer: C
	
	// TODO: Add support for visual effects
	protected def overlays: Seq[(C, LayerPositioning)]
	
	
	// IMPLEMENTED  -------------------------
	
	override def children = components
	
	override def components = mainLayer +: overlays.map { _._1 }
	
	override def calculatedStackSize =
	{
		if (overlays.isEmpty)
			mainLayer.stackSize
		else {
			// Combines the layer sizes
			val combinedLayerSize = StackSize.combine(overlays.map { _._1.stackSize })
			// Prefers the main layer size, but may be limited by other layers' min sizes
			val mainLayerSize = mainLayer.stackSize
			// Case: No expansion required
			if (combinedLayerSize.min.fitsWithin(mainLayerSize.optimal))
				mainLayerSize
			// Case: Some lengths need to be adjusted
			else
				mainLayerSize.withMin(combinedLayerSize.min)
		}
	}
	
	// This view is opaque if the main layer is opaque or if there exists an opaque layer that spans this whole view
	override def transparent = mainLayer.transparent &&
		overlays.forall { case (l, _) => l.transparent || !size.fitsWithin(l.size) }
	
	override def updateLayout() =
	{
		// The main layer is always set to fill the whole area
		mainLayer.size = size
		overlays.foreach { case (layer, positioning) =>
			val targetArea = Bounds(Point.origin, size)
			layer.bounds = positioning match {
				// Case: Custom positioning function
				case Free(calculate) => calculate(targetArea, layer.bounds, layer.stackSize)
				case AnchoredTo(component, alignment, margin, primaryAxis) =>
					component.positionRelativeTo(this) match {
						// Case: Anchoring to component
						case Some(anchorPosition) =>
							alignment.positionNextToWithin(layer.stackSize, Bounds(anchorPosition, component.size),
								targetArea, margin, primaryAxis)
						// Case: Anchoring fails
						case None => Bounds(layer.position, layer.stackSize.optimal).fittedInto(targetArea)
					}
				case AlignedToSide(alignment, optimalMargin, expandIfPossible) =>
					if (expandIfPossible)
						alignment.positionStretching(layer.stackSize, targetArea, optimalMargin.downscaling)
					else
						alignment.positionWithInsets(layer.stackSize.optimal, targetArea, optimalMargin.any)
			}
		}
	}
	
	// TODO: Add support for visual effects (with buffering)
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) =
	{
		paintContent(drawer, Background, clipZone)
		paintContent(drawer, Normal, clipZone)
		
		// Paints the layers from the bottom to the top. Won't paint areas behind opaque layers.
		val childDrawer = drawer.translated(position)
		clipZone match {
			case Some(clipZone) =>
				val layerClipZone = clipZone - position
				// Skips layers that don't overlap with the clip zone
				val layers = overlays.map { _._1 }.filter { _.bounds.overlapsWith(layerClipZone) }
				val layersToDraw = layers.lastIndexWhereOption { layer =>
					layer.opaque &&
						layer.bounds.contains(layerClipZone)
				} match {
					// Case: Some layers are hidden
					case Some(opaqueLayerIndex) => layers.drop(opaqueLayerIndex)
					// Case: No opaque layers
					case None => mainLayer +: layers
				}
				// Draws the layers in order
				layersToDraw.foreach { _.paintWith(childDrawer, Some(layerClipZone)) }
			case None => (mainLayer +: overlays.map { _._1 }).foreach { _.paintWith(childDrawer) }
		}
		
		paintContent(drawer, Foreground, clipZone)
	}
}
