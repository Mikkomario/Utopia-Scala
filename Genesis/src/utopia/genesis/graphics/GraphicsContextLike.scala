package utopia.genesis.graphics

import utopia.paradigm.shape.shape2d.{Bounds, Matrix2D, Polygonic}
import utopia.paradigm.transform.{AffineTransformable, LinearTransformable}
import utopia.paradigm.shape.shape3d.Matrix3D

import java.awt.{AlphaComposite, Font, RenderingHints}

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
	  * @return Font metrics specified within this context (intended for contexts which have defined a font)
	  */
	def fontMetrics = graphics.fontMetrics
	/**
	  * @return Area where this drawer clips the drawn contents. I.e. nothing is drawn outside of the clipped area.
	  */
	def clipping = graphics.clipping
	/**
	  * @return Area where this drawer clips the drawn contents, as a set of bounds.
	  *         The actual clipping shape may differ.
	  */
	def clippingBounds = graphics.clippingBounds
	
	/**
	  * @return A copy of this context without any clipping applied
	  */
	def withoutClipping = withGraphics(graphics.withoutClipping)
	
	/**
	  * @return A copy of this drawer where anti-aliasing is enabled
	  */
	def antialiasing = withAntialiasingState(true)
	/**
	  * @return A copy of this drawer where anti-aliasing is disabled
	  */
	def withoutAntialiasing = withAntialiasingState(false)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def transformedWith(transformation: Matrix2D): Repr = transformedWith(transformation.to3D)
	override def transformedWith(transformation: Matrix3D) = mapGraphics { _.transformedWith(transformation) }
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param font A font
	  * @return Font metrics to use with that font
	  */
	def fontMetricsWith(font: Font) = FontMetricsWrapper(graphics.value.getFontMetrics(font))
	
	/**
	  * @param f A mapping function for the graphics context
	  * @return A copy of this context with the modified graphics instance
	  */
	def mapGraphics(f: LazyGraphics => LazyGraphics) = withGraphics(f(graphics))
	/**
	  * @param f A mutator function for a graphics instance
	  * @return A mutated copy of this context
	  */
	def withMutatedGraphics(f: ClosingGraphics => Unit) = mapGraphics { _.mutatedWith(f) }
	
	/**
	  * @param clipping A new clipping area to apply (lazy).
	  *                 The area should be within this graphics's transformation context.
	  * @return A copy of this graphics context, clipped to that area (overwrites any current clipping)
	  */
	def withClip(clipping: => Polygonic): Repr = mapGraphics { _.withClip(clipping) }
	/**
	  * @param clippingBounds A new set of clipping bounds. Should be set within this instance's transformation context.
	  * @return A copy of this context where clipping is reduced to the specified bounds.
	  *         Applies current clipping area bounds (not necessarily shape) as well.
	  */
	def clippedToBounds(clippingBounds: Bounds) = mapGraphics { _.clippedToBounds(clippingBounds) }
	
	/**
	  * @param alpha An alpha value, between 0 and 1 where 0 means fully transparent and 1 means fully visible.
	  * @return A copy of this drawer with the specified alpha modifier.
	  */
	def withAlpha(alpha: Double) = withMutatedGraphics {
		_.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.toFloat))
	}
	
	/**
	  * Enables or disables anti-aliasing
	  * @param antialiasingEnabled Whether anti-aliasing should be enabled
	  * @return Copy of this drawer with the specified rendering hint active
	  */
	def withAntialiasingState(antialiasingEnabled: Boolean) = withMutatedGraphics { graphics =>
		val value = if (antialiasingEnabled) RenderingHints.VALUE_ANTIALIAS_ON else RenderingHints.VALUE_ANTIALIAS_OFF
		graphics.wrapped.setRenderingHint(RenderingHints.KEY_ANTIALIASING, value)
	}
}
