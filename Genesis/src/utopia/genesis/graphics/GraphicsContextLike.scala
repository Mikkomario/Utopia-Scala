package utopia.genesis.graphics

import utopia.genesis.shape.shape2D.{Bounds, Matrix2D, Polygonic}
import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}
import utopia.genesis.shape.shape3D.Matrix3D

import java.awt.Font

/**
  * Provides read access to graphics related settings
  * @author Mikko Hilpinen
  * @since 15.5.2021, v2.5.1
  */
trait GraphicsContextLike[+Repr] extends LinearTransformable[Repr] with AffineTransformable[Repr]
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Graphics instance wrapped by this context
	  */
	protected def graphics: LazyGraphics
	
	/**
	  * @param newGraphics A new graphics instance
	  * @return A copy of this context that wraps the specified graphics instance instead of the current one
	  */
	protected def withGraphics(newGraphics: LazyGraphics): Repr
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Transformation applied by this context
	  */
	def transformation = graphics.transformation
	
	/**
	  * @return A copy of this context without any clipping applied
	  */
	def withoutClipping = withGraphics(graphics.withoutClipping)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def transformedWith(transformation: Matrix2D): Repr = transformedWith(transformation.to3D)
	override def transformedWith(transformation: Matrix3D) =
		withGraphics(graphics.transformedWith(transformation))
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param font A font
	  * @return Font metrics to use with that font
	  */
	def fontMetricsWith(font: Font) = graphics.value.getFontMetrics(font)
	
	/**
	  * @param clipping A new clipping area to apply (lazy).
	  *                 The area should be within this graphics's transformation context.
	  * @return A copy of this graphics context, clipped to that area (overwrites any current clipping)
	  */
	def withClip(clipping: => Polygonic): Repr = withGraphics(graphics.withClip(clipping))
	/**
	  * @param clippingBounds A new set of clipping bounds. Should be set within this instance's transformation context.
	  * @return A copy of this context where clipping is reduced to the specified bounds.
	  *         Applies current clipping area bounds (not necessarily shape) as well.
	  */
	def reducedToBounds(clippingBounds: Bounds) = withGraphics(graphics.reducedToBounds(clippingBounds))
}
