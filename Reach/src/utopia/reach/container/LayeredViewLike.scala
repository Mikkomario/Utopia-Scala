package utopia.reach.container

import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.genesis.util.Drawer
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.container.LayerPositioning.{AlignedToSide, AnchoredTo, Free}
import utopia.reflection.component.drawing.template.DrawLevel
import utopia.reflection.component.template.layout.stack.{StackSizeCalculating, Stackable2}
import utopia.reflection.container.template.MultiContainer2
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.stack.{StackLength, StackSize}

/**
  * A template trait for containers which hold multiple overlaid layers of components
  * @author Mikko Hilpinen
  * @since 18.4.2021, v1.0
  */
trait LayeredViewLike[+C <: ReachComponentLike] extends ReachComponentLike with MultiContainer2[C]
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
		else
		{
			// Combines the layer sizes
			val combinedLayerSize = StackSize.combine(overlays.map { _._1.stackSize })
			// Prefers the main layer size, but may be limited by other layers' min sizes
			val mainLayerSize = mainLayer.stackSize
			// Case: No expansion required
			if (combinedLayerSize.min.fitsInto(mainLayerSize.optimal))
				mainLayerSize
			// Case: Some lengths need to be adjusted
			else
				mainLayerSize.withMin(combinedLayerSize.min)
		}
	}
	
	// This view is opaque if the main layer is opaque or if there exists an opaque layer that spans this whole view
	override def transparent = mainLayer.transparent &&
		overlays.forall { case (l, _) => l.transparent || !size.fitsInto(l.size) }
	
	// TODO: Continue programming
	override def updateLayout() =
	{
		// The main layer is always set to fill the whole area
		mainLayer.size = size
		overlays.foreach { case (layer, positioning) =>
			layer.bounds = positioning match
			{
				// Case: Custom positioning function
				case Free(calculate) => calculate(size, layer.bounds, layer.stackSize)
				case AnchoredTo(component, alignment, margin) =>
					val targetArea = Bounds(Point.origin, size)
					component.positionRelativeTo(this) match
					{
						// Case: Anchoring to component
						case Some(anchorPosition) =>
							// Case: Displaying directly over the component
							if (alignment == Center)
								Bounds.centered(anchorPosition + component.size / 2, layer.stackSize.optimal)
									.fittedInto(targetArea)
							// Case: Displaying at one side of the component
							else
							{
								???
							}
						// Case: Anchoring fails
						case None => ???
					}
				case AlignedToSide(alignment, optimalMargin, expandIfPossible) => ???
			}
		}
	}
	
	override def paintWith(drawer: Drawer, clipZone: Option[Bounds]) = super.paintWith(drawer, clipZone)
}
