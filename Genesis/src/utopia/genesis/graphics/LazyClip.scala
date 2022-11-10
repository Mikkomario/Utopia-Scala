package utopia.genesis.graphics

import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.shape.shape2d.{Matrix2D, Polygonic}
import utopia.paradigm.transform.{AffineTransformable, LinearTransformable}
import utopia.paradigm.shape.shape3d.Matrix3D

object LazyClip
{
	/**
	  * @param clippingArea A clipping area (called lazily)
	  * @return That clipping area wrapped as a LazyClip instance
	  */
	def apply(clippingArea: => Polygonic) = new LazyClip(Left(Lazy(clippingArea)))
}

/**
  * A clipping zone wrapper that calculates transformations lazily
  * @author Mikko Hilpinen
  * @since 28.1.2022, v2.6.3
  */
class LazyClip(parent: Either[Lazy[Polygonic], (LazyClip, Lazy[Matrix3D])])
	extends Lazy[Polygonic] with LinearTransformable[LazyClip] with AffineTransformable[LazyClip]
{
	// ATTRIBUTES   -----------------------------
	
	// Calculates the actual (relative) clipping shape lazily, as late as possible, utilizing parent calculations
	// if such have been performed at this point
	private val cache: Lazy[Polygonic] = Lazy {
		parent match {
			// Case: No lazy parent => presents the wrapped clipping area
			case Left(area) => area.value
			// Case: Lazy parent + transformation => transforms the parent shape
			case Right((parent, transformation)) =>
				parent.materials match {
					// Case: Parent had already calculated clipping shape => applies latest transformation only
					case Right(shape) => shape.transformedWith(transformation.value)
					// Case: Parent is yet to calculate their clipping shape =>
					// applies all pending transformations multiplied
					// (because matrix multiplication should be more efficient than clipping shape transformation)
					case Left((shape, firstTransformation)) =>
						// TODO: Make sure transformations are applied in the correct order here
						shape.transformedWith(transformation.value(firstTransformation.value))
				}
		}
	}
	
	
	// COMPUTED ---------------------------------
	
	// Returns materials used in clipping calculation. Either:
	// Right: A pre-calculated clipping shape in current transformation context
	// Left: Some clipping shape + transformation to apply to that shape (lazy)
	private def materials: Either[(Polygonic, Lazy[Matrix3D]), Polygonic] = cache.current match {
		case Some(shape) => Right(shape)
		case None =>
			parent match {
				case Left(shape) => Right(shape.value)
				case Right((parent, transformation)) =>
					parent.materials match {
						case Right(shape) => Left(shape, transformation)
						case Left((shape, firstTransformation)) =>
							// TODO: Make sure the transformations are in the correct order (apply vs *)
							Left(shape, Lazy { transformation.value(firstTransformation.value) })
					}
			}
	}
	
	
	// IMPLEMENTED  -----------------------------
	
	override def repr = this
	
	override def value = cache.value
	override def current = cache.current
	
	override def transformedWith(transformation: Matrix2D): LazyClip =
		new LazyClip(Right(this -> Lazy { transformation.to3D }))
	override def transformedWith(transformation: Matrix3D) = new LazyClip(Right(this -> Lazy.initialized(transformation)))
}
