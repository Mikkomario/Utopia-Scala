package utopia.paradigm.enumeration

import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * An enumeration for different ways to adjust an item's size to fill, fit, etc. a specific area.
  * @author Mikko Hilpinen
  * @since 11/02/2024, v1.5.1
  */
trait FillAreaLogic
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Whether the resulting size always fits within the targeted region,
	  *         and never extends outside of it.
	  */
	def fitsWithinTargetArea: Boolean
	/**
	  * @return Whether the resulting size always covers the targeted region
	  */
	def fillsTargetArea: Boolean
	
	/**
	  * Modifies a size to match the targeted size, according to this logic
	  * @param original Original size
	  * @param target Targeted (optimal) size
	  * @return Modified size, positioned relative to the top left corner of the targeted size frame
	  */
	def apply(original: Size, target: Size): Bounds
	
	
	// OTHER    ---------------------
	
	/**
	  * Resizes the original size and positions it
	  * @param original Original size
	  * @param target Targeted (optimal) bounds
	  * @return Modified bounds
	  */
	def apply(original: Size, target: Bounds): Bounds = {
		val relative = apply(original, target.size)
		relative + target.position
	}
}

object FillAreaLogic
{
	/**
	  * Scales the area, preserving shape.
	  * Positions the scaled area at the center of the targeted area.
	  * May not fill the whole area, or may expand outside of it, depending on the implementation.
	  */
	sealed trait ScalePreservingShape extends FillAreaLogic
	{
		// ABSTRACT ----------------------
		
		/**
		  * @param original Original size
		  * @param target Targeted size
		  * @return How much the original size should be scaled
		  */
		def scalingFor(original: Size, target: Size): Double
		
		
		// IMPLEMENTED  ------------------
		
		override def apply(original: Size, target: Size): Bounds = {
			val scaled = original * scalingFor(original, target)
			Bounds(Center.position(scaled, target), scaled)
		}
	}
	
	/**
	  * Scales the area, preserving shape, until one of its edges hit the targeted area.
	  * The resulting area will always completely fit within the targeted area.
	  */
	case object Fit extends ScalePreservingShape
	{
		override def fitsWithinTargetArea: Boolean = true
		override def fillsTargetArea: Boolean = false
		
		override def scalingFor(original: Size, target: Size): Double = (target / original).minDimension
	}
	/**
	  * Scales the area, preserving shape,
	  * so that the resulting area fills the targeted area on both axes, but only just.
	  * The result will typically expand outside of the targeted area.
	  */
	case object Fill extends ScalePreservingShape
	{
		override def fitsWithinTargetArea: Boolean = false
		override def fillsTargetArea: Boolean = true
		
		override def scalingFor(original: Size, target: Size): Double = (target / original).maxDimension
	}
	
	/**
	  * If the original area is too large, scales it down (preserving shape), so that it completely fits within
	  * the targeted area.
	  */
	case object DownscaleToFit extends ScalePreservingShape
	{
		override def fitsWithinTargetArea: Boolean = true
		override def fillsTargetArea: Boolean = false
		
		override def scalingFor(original: Size, target: Size): Double = (original / target).minDimension min 1.0
	}
	/**
	  * If the original area is too large, scales it down (preserving shape),
	  * so that both dimensions of the targeted area are just covered.
	  */
	case object DownscaleToFill extends ScalePreservingShape
	{
		override def fitsWithinTargetArea: Boolean = false
		override def fillsTargetArea: Boolean = false
		
		override def scalingFor(original: Size, target: Size): Double = (original / target).maxDimension min 1.0
	}
	
	/**
	  * Scales the original area, preserving shape,
	  * so that it's length along the specified axis matches that of the target area along that same axis.
	  * @param axis Axis that determines the scaling applied
	  */
	case class FillAlong(axis: Axis2D) extends ScalePreservingShape
	{
		override def fitsWithinTargetArea: Boolean = false
		override def fillsTargetArea: Boolean = false
		
		override def scalingFor(original: Size, target: Size): Double = target(axis) / original(axis)
	}
	
	/**
	  * This resize logic doesn't respect the original size, and simply overwrites it with the new size.
	  */
	case object Overwrite extends FillAreaLogic
	{
		override def fitsWithinTargetArea: Boolean = true
		override def fillsTargetArea: Boolean = true
		
		override def apply(original: Size, target: Size): Bounds = Bounds(Point.origin, target)
	}
	
	/**
	  * If the original area is too large, crops edges from it in order to fit within the targeted area.
	  * Doesn't preserve shape.
	  */
	case object Crop extends FillAreaLogic
	{
		override def fitsWithinTargetArea: Boolean = true
		override def fillsTargetArea: Boolean = false
		
		override def apply(original: Size, target: Size): Bounds = {
			val cropped = original.mergeWith(target) { _ min _ }
			Bounds(Center.position(cropped, target), cropped)
		}
	}
}