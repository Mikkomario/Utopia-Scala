package utopia.reach.container.layered

import utopia.firmament.component.container.many.MultiContainer
import utopia.firmament.component.stack.StackSizeCalculating
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.genesis.graphics.DrawLevel.{Background, Foreground, Normal}
import utopia.firmament.model.stack.LengthExtensions._
import utopia.firmament.model.stack.StackSize
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.genesis.graphics.Drawer
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.{ContextualMixed, FromGenericContextFactory, Mixed}
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.{CustomDrawReachComponent, PartOfComponentHierarchy, ReachComponentLike}
import utopia.reach.component.wrapper.ComponentCreationResult.LayersResult
import utopia.reach.component.wrapper.Open
import utopia.reach.component.wrapper.OpenComponent.OpenLayerComponents
import utopia.reach.container.layered.LayerPositioning.{AlignedToSide, AnchoredTo, Free}

trait LayersFactoryLike[+Repr] extends PartOfComponentHierarchy with CustomDrawableFactory[Repr]
{
	/**
	  * Creates a new combined layers -container by wrapping a set of open components
	  * @param content Content to assign to this container
	  *                (containing the main component + layers under a single open component)
	  * @tparam M Type of the main component to wrap
	  * @tparam C Type of the layer components to wrap
	  * @tparam R Type of the additional creation result
	  * @return A component wrap result that contains the created Layers container +
	  *         the main component + the layer components + the additional creation result
	  */
	def apply[M <: ReachComponentLike, C <: ReachComponentLike, R](content: OpenLayerComponents[M, C, R]) =
	{
		val layers: Layers = new _Layers(parentHierarchy, content.component._1, content.component._2, customDrawers)
		content.attachTo(layers).mapChild { case (main, layers) => main -> layers.map { _._1 } }
	}
}

case class LayersFactory(parentHierarchy: ComponentHierarchy, customDrawers: Seq[CustomDrawer] = Empty)
	extends LayersFactoryLike[LayersFactory] with FromGenericContextFactory[Any, ContextualLayersFactory]
{
	// IMPLEMENTED  -----------------------
	
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): LayersFactory = copy(customDrawers = drawers)
	
	override def withContext[N <: Any](context: N): ContextualLayersFactory[N] =
		ContextualLayersFactory(parentHierarchy, context, customDrawers)
	
	
	// OTHER    ---------------------------
	
	/**
	  * Builds a layered view, along with its components
	  * @param mainFactory Factory used for constructing the main component
	  * @param layersFactory Factory used for constructing the additional layer components
	  * @param fill Function that accepts two prepared factories (main + layers) and yields a component creation result,
	  *             which specifies the constructed main component + layer components, where each layer is coupled with
	  *             information showing how it should be positioned.
	  * @tparam MF Type of the main component factory used
	  * @tparam CF Type of the layer component factory used
	  * @tparam M Type of the constructed main component
	  * @tparam C Type of the constructed layer components
	  * @tparam R Type of additional component creation result
	  * @return A new layered view + created components + additional creation result
	  */
	def build[MF, CF, M <: ReachComponentLike, C <: ReachComponentLike, R](mainFactory: Cff[MF], layersFactory: Cff[CF])
	                                                                      (fill: (MF, CF) => LayersResult[M, C, R]) =
		apply(Open[(M, Seq[(C, LayerPositioning)]), R] { hierarchy =>
			fill(mainFactory(hierarchy), layersFactory(hierarchy)) })
}

case class ContextualLayersFactory[+N](parentHierarchy: ComponentHierarchy, context: N,
                                       customDrawers: Seq[CustomDrawer] = Empty)
	extends LayersFactoryLike[ContextualLayersFactory[N]] with GenericContextualFactory[N, Any, ContextualLayersFactory]
{
	// IMPLEMENTED  ----------------------------
	
	override def withContext[N2 <: Any](newContext: N2): ContextualLayersFactory[N2] = copy(context = newContext)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualLayersFactory[N] =
		copy(customDrawers = drawers)
		
	
	// OTHER    --------------------------------
	
	/**
	  * Builds a new layered view, along with its components
	  * @param mainFactory Component factory used for constructing the main component
	  * @param layersFactory Component factory used for constructing the layer components
	  * @param fill A function that accepts two prepared component factories
	  *             (one for the main component and one for the layers) and yields the following:
	  *             1. The main component
	  *             1. The additional layer components, each coupled with their positioning instructions
	  *             1. Additional component creation result
	  * @tparam MF Type of factory used for constructing the main component
	  * @tparam CF Type of factory used for constructing the layer components
	  * @tparam M Type of the constructed main component
	  * @tparam C Type of the constructed layer components
	  * @tparam R Type of the additional creation result
	  * @return A result that contains:
	  *         1. Created container
	  *         1. Created main component
	  *         1. Created layer components
	  *         1. Additional component creation result
	  */
	def build[MF, CF, M <: ReachComponentLike, C <: ReachComponentLike, R](mainFactory: Ccff[N, MF], layersFactory: Ccff[N, CF])
	                          (fill: (MF, CF) => LayersResult[M, C, R]) =
		apply(Open.contextual(context).apply[ContextualMixed[N], (M, Seq[(C, LayerPositioning)]), R](Mixed) { factories =>
			fill(factories(mainFactory), factories(layersFactory))
		})
}

object Layers extends Cff[LayersFactory]
{
	// IMPLEMENTED  -------------------------
	
	override def apply(hierarchy: ComponentHierarchy): LayersFactory = LayersFactory(hierarchy)
}

/**
  * A template trait for containers which hold multiple overlaid layers of components
  * @author Mikko Hilpinen
  * @since 18.4.2021, v1.0
  */
// TODO: Later, add support for additional focus systems / layers
trait Layers extends CustomDrawReachComponent with MultiContainer[ReachComponentLike] with StackSizeCalculating
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The primary (bottom) component wrapped by this container
	  */
	protected def mainLayer: ReachComponentLike
	/**
	  * @return The additional layers that are drawn above the main component
	  */
	protected def overlays: Seq[(ReachComponentLike, LayerPositioning)]
	
	
	// IMPLEMENTED  -------------------------
	
	override def children = components
	override def components = mainLayer +: overlays.map { _._1 }
	
	override def calculatedStackSize = {
		if (overlays.isEmpty)
			mainLayer.stackSize
		else {
			// Combines the layer sizes
			// TODO: May be refactored to just calculate the minimum size?
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
	
	// TODO: Overwrite mouse event distribution? At least the component ordering seems wrong in that regard
	
	override def updateLayout() = {
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
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = {
		paintContent(drawer, Background, clipZone)
		// Paints the layers from the bottom to the top. Won't paint areas behind opaque layers.
		val childDrawer = drawer.translated(position)
		clipZone match {
			case Some(clipZone) =>
				val layerClipZone = clipZone - position
				// Skips layers that don't overlap with the clip zone
				val layers = overlays.map { _._1 }.filter { _.bounds.overlapsWith(layerClipZone) }
				val (layersToDraw, paintMainLayer) = layers.findLastIndexWhere { layer =>
					layer.opaque &&
						layer.bounds.contains(layerClipZone)
				} match {
					// Case: Some layers are hidden
					case Some(opaqueLayerIndex) => layers.drop(opaqueLayerIndex) -> false
					// Case: No opaque layers
					case None => layers -> true
				}
				// Draws the layers in order
				if (paintMainLayer)
					mainLayer.paintWith(childDrawer, Some(layerClipZone))
				// Draws the additional effects (i.e. the "normal" layer) above the main layer
				paintContent(drawer, Normal, Some(clipZone))
				layersToDraw.foreach { _.paintWith(childDrawer, Some(layerClipZone)) }
			case None =>
				mainLayer.paintWith(childDrawer)
				paintContent(drawer, Normal, clipZone)
				overlays.foreach { _._1.paintWith(childDrawer) }
		}
		paintContent(drawer, Foreground, clipZone)
	}
}

private class _Layers(override val parentHierarchy: ComponentHierarchy,
                      override val mainLayer: ReachComponentLike,
                      override val overlays: Seq[(ReachComponentLike, LayerPositioning)],
                      override val customDrawers: Seq[CustomDrawer])
	extends Layers