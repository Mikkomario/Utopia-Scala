package utopia.genesis.graphics

import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.{Bounds, Matrix2D, Polygonic, Size}
import utopia.paradigm.transform.{AffineTransformable, LinearTransformable}
import utopia.paradigm.shape.shape3d.Matrix3D

import java.awt.{Font, Graphics2D, Toolkit}
import scala.util.Try

object LazyGraphics
{
	/**
	  * @param graphics Graphics instance to wrap (lazily called)
	  * @return A lazy graphics instance
	  */
	def apply(graphics: => ClosingGraphics) = new LazyGraphics(Left(Lazy(graphics)))
	
	/**
	  * @param graphics a (root level) graphics instance to wrap (called lazily)
	  * @return A lazy graphics instance
	  */
	def wrap(graphics: => Graphics2D): LazyGraphics = apply(ClosingGraphics(graphics))
}

/**
  * A graphics instance wrapper that applies predefined changes (transformation, custom mutations, clipping) lazily
  * @author Mikko Hilpinen
  * @since 28.1.2022, v2.6.3
  * @param parent The parent graphics instance. Either
  *               Left: A simple graphics object (lazy), or
  *               Right: A LazyGraphics instance.
  * @param newTransformation Transformation to apply over the parent's transformation state.
  *                       None if no transformation is necessary.
  * @param mutation A custom mutation to apply to the generated graphics instance. None if no mutation is necessary.
  * @param newClipping A clipping to overwrite parent clipping with (lazy). None if no custom clipping is required.
  * @param isClippingDisabled Whether parent clipping should be ignored, i.e. not inherited (default = false)
  */
class LazyGraphics(parent: Either[Lazy[ClosingGraphics], LazyGraphics],
                   newTransformation: Option[Lazy[Matrix3D]] = None,
                   mutation: Option[ClosingGraphics => Unit] = None, newClipping: Option[LazyClip] = None,
                   isClippingDisabled: Boolean = false)
	extends AutoCloseable with Lazy[ClosingGraphics]
		with LinearTransformable[LazyGraphics] with AffineTransformable[LazyGraphics]
{
	// ATTRIBUTES   -------------------------------
	
	// Calculates the graphic instance only when necessary
	private val baseCache: Lazy[ClosingGraphics] = Lazy {
		// Acquires a parent graphics instance and checks which transformations and mutations to apply to it
		val (graphics, fullTransformation, allMutations) = parent match {
			// Case: Parent is a graphics instance => uses that
			case Left(graphics) => (graphics.value, newTransformation, mutation.toVector)
			// Case: Parent is lazily calculated => uses available state
			case Right(parent) =>
				parent.materials match {
					// Case: Parent has prepared a graphics instance => uses that
					case Right(graphics) => (graphics, newTransformation, mutation.toVector)
					// Case: Parent is yet to prepare a graphics instance => uses some other parent and applies
					// all pending transformations and mutations
					case Left((graphics, parentTransformation, parentMutations)) =>
						(graphics, combineTransforms(parentTransformation), parentMutations ++ mutation)
				}
		}
		// Case: No transformations needed => uses parent graphics instance
		if (fullTransformation.isEmpty && allMutations.isEmpty)
			graphics
		// Case: Mutations / transformations to apply => creates a modified child instance to use
		else {
			val newGraphics = graphics.createChild()
			fullTransformation.foreach { t => newGraphics.transform(t.value) }
			allMutations.foreach { _(newGraphics) }
			newGraphics
		}
	}
	
	/**
	  * Clipping area applied to this graphics instance.
	  * The area is specified within this instance's transformation context.
	  */
	lazy val clipping = _clipping.map { _.value }
	// A clipped version of the available graphics instance
	private val clippedCache = Lazy {
		clipping match {
			// Case: Clipping is required => applies it to a new child graphics instance
			case Some(clipping) =>
				val graphics = baseCache.value.createChild()
				graphics.clip(clipping.toShape)
				graphics
			// Case: No clipping is required => uses the base graphics instance
			case None => baseCache.value
		}
	}
	
	// Sequence of transformations leading to this graphics state
	private lazy val transformationSequence: LazyTransformationSequence = newTransformation match {
		case Some(transformation) =>
			parent match {
				case Left(_) => LazyTransformationSequence.root(transformation.value)
				case Right(parent) => parent.transformationSequence.transformedWith(transformation.value)
			}
		case None =>
			parent match {
				case Left(_) => LazyTransformationSequence.origin
				case Right(parent) => parent.transformationSequence
			}
	}
	
	
	/**
	  * Clipping boundaries applied when using this graphics instance.
	  * The boundaries are specified within this instance's transformation context.
	  */
	lazy val clippingBounds = clipping.map { _.bounds }
	
	/**
	  * @return The font metrics instance associated with this graphics instance, with the current font
	  */
	lazy val fontMetrics = FontMetricsWrapper(value.getFontMetrics)
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return The current transformation of this graphics instance
	  */
	def transformation = transformationSequence.value
	
	/**
	  * @return A copy of this graphics instance without any clipping applied
	  */
	def withoutClipping = {
		if (!isClippingDisabled || newClipping.isDefined)
			new LazyGraphics(Right(this), isClippingDisabled = true)
		else
			this
	}
	
	// The clipping area to apply when using this graphics instance. None if there is no clipping.
	private def _clipping: Option[LazyClip] = newClipping.orElse {
		if (isClippingDisabled) None else parent.toOption.flatMap { _._clipping }
	}
	
	// Materials used when constructing this (or dependent) graphics context
	// Either
	//      Right: A pre-calculated graphics instance
	//      Left: Some graphics instance + changes to perform on that graphics instance:
	//          1) Transformation to apply (lazy)
	//          2) Mutations to apply (ordered)
	private def materials: Either[(ClosingGraphics, Option[Lazy[Matrix3D]], Seq[ClosingGraphics => Unit]), ClosingGraphics] =
		baseCache.current match {
			// Case: Has already prepared a graphics object => uses that
			case Some(graphics) => Right(graphics)
			// Case: A graphics object is yet to be fully prepared
			case None =>
				parent match {
					// Case: Parent is a pre-calculated graphics object => uses that as the base
					case Left(graphics) => Left((graphics.value, newTransformation, mutation.toVector))
					// Case: Parent is lazily calculated => Uses available data
					case Right(parent) =>
						parent.materials match {
							// Case: Parent has pre-calculated graphics => Uses those as the base
							case Right(graphics) => Left((graphics, newTransformation, mutation.toVector))
							// Case: Parent is yet to calculate their own graphics version => combines changes
							case Left((graphics, parentTransformation, parentMutations)) =>
								Left((graphics, combineTransforms(parentTransformation), parentMutations ++ mutation))
						}
				}
		}
	
	
	// IMPLEMENTED  -----------------------------
	
	override def current = clippedCache.current
	override def value = clippedCache.value
	
	override def transformedWith(transformation: Matrix2D): LazyGraphics = transformedWith(transformation.to3D)
	override def transformedWith(transformation: Matrix3D) =
		new LazyGraphics(Right(this), newTransformation = Some(Lazy.initialized(transformation)),
			// Transforms the existing clipping with the inverse of the applied view transformation
			// (so that the resulting shape stays the same after applying view transformation)
			newClipping = _clipping.map { clip =>
				transformation.inverse match {
					case Some(inv) => clip.transformedWith(inv)
					case None => clip
				}
			})
	
	override def close() = baseCache.current.foreach { _.close() }
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param mutator A graphics mutator function
	  * @return A copy of this graphics instance lazily mutated with the specified function
	  */
	def mutatedWith(mutator: ClosingGraphics => Unit) = new LazyGraphics(Right(this), mutation = Some(mutator))
	
	/**
	  * @param clipping A new clipping area to apply (lazy).
	  *                 The area should be within this graphics's transformation context.
	  * @return A copy of this graphics instance, which is clipped to that area (overwrites any current clipping)
	  */
	def withClip(clipping: LazyClip) = new LazyGraphics(Right(this), newClipping = Some(clipping))
	/**
	  * @param clipping A new clipping area to apply (lazy).
	  *                 The area should be within this graphics's transformation context.
	  * @return A copy of this graphics instance, which is clipped to that area (overwrites any current clipping)
	  */
	  // FIXME: Doesn't work
	def withClip(clipping: => Polygonic): LazyGraphics = withClip(LazyClip(clipping))
	
	/**
	  * @param clippingBounds A new set of clipping bounds. Should be set within this instance's transformation context.
	  * @return A copy of this graphics instance where clipping is reduced to the specified bounds.
	  *         Applies current clipping area bounds (not necessarily shape) as well.
	  */
	def clippedToBounds(clippingBounds: Bounds) = withClip {
		this.clippingBounds match {
			case Some(existingBounds) =>
				clippingBounds.overlapWith(existingBounds).getOrElse(clippingBounds.withSize(Size.zero))
			case None => clippingBounds
		}
	}
	
	/**
	  * @param font Font to use when drawing text
	  * @param textColor Color to use when drawing text
	  * @return A copy of this graphics instance that is prepared for drawing text
	  */
	def forTextDrawing(font: Font, textColor: Color = Color.textBlack) = mutatedWith { g =>
		g.setColor(textColor.toAwt)
		g.setFont(font)
		// Sets rendering hints based on desktop settings
		Try { Option(Toolkit.getDefaultToolkit.getDesktopProperty("awt.font.desktophints"))
			.map { _.asInstanceOf[java.util.Map[_, _]] }.foreach(g.setRenderingHints) }
	}
	
	private def combineTransforms(parentTransform: Option[Lazy[Matrix3D]]) = {
		newTransformation match {
			// Case: New transformation being applied
			case Some(myTransformation) =>
				parentTransform match {
					// Case: Parent transformation also defined => merges the two
					case Some(parentTransform) =>
						Some(Lazy { parentTransform.value(myTransformation.value) })
					// Case: No parent transformation => Uses own transformation only
					case None => Some(myTransformation)
				}
			// Case: No new transformation to apply => Uses parent state
			case None => parentTransform
		}
	}
}
