package utopia.reach.container

import utopia.genesis.shape.Axis.Y
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Bounds
import utopia.reach.component.template.ReachComponentLike
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.stack.StackSize

/**
  * An enumeration for different ways to place layers in a layered view
  * @author Mikko Hilpinen
  * @since 18.4.2021, v1.0
  */
sealed trait LayerPositioning

object LayerPositioning
{
	/**
	  * A layer is free to move by itself, but will be kept within the view bounds, if possible
	  * @param calculateBounds A function for calculating new layer bounds. Accepts container bounds,
	  *                        current layer bounds and layer stack size. Returns new layer bounds.
	  */
	case class Free(calculateBounds: (Bounds, Bounds, StackSize) => Bounds) extends LayerPositioning
	
	/**
	  * A layer is anchored to a component position
	  * @param component Component to which the layer is anchored to
	  * @param alignment Alignment / direction towards which the layer will be placed of the component,
	  *                  if possible (default = Center)
	  * @param optimalMargin Margin placed between the component and the layer when possible (default = 0.0)
	  * @param primaryAxis Axis to consider first when the alignment affects both of the axes.
	  *                    Default = Y = Vertical position will be assigned first for BottomX and TopX alignments.
	  */
	case class AnchoredTo(component: ReachComponentLike, alignment: Alignment = Center, optimalMargin: Double = 0.0,
	                      primaryAxis: Axis2D = Y)
		extends LayerPositioning
	
	/**
	  * A layer will be aligned within the parent container
	  * @param alignment Alignment used when placing the layer
	  * @param optimalMargin Margin placed around the layer when possible (default = 0.0)
	  * @param expandIfPossible Whether the layer should attempted to be expanded to cover the
	  *                         whole side of the container (default = true)
	  */
	case class AlignedToSide(alignment: Alignment, optimalMargin: Double = 0.0, expandIfPossible: Boolean = true)
		extends LayerPositioning
}
