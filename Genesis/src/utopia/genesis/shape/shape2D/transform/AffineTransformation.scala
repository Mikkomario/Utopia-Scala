package utopia.genesis.shape.shape2D.transform

import utopia.genesis.animation.Animation
import utopia.genesis.animation.transform.{AnimatedAffineTransformable, AnimatedAffineTransformation, AnimatedLinearTransformable}
import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.{JavaAffineTransformConvertible, Matrix2D, Point, Vector2D, Vector2DLike}
import utopia.genesis.shape.shape3D.Matrix3D
import utopia.genesis.shape.template.Dimensional
import utopia.genesis.util.ApproximatelyEquatable

object AffineTransformation
{
    // ATTRIBUTES   --------------------------
    
    /**
      * An identity transformation which doesn't have any effect
      */
    val identity = apply(Vector2D.zero, LinearTransformation.identity)
    
    
    // OTHER    ------------------------------
    
    /**
      * Creates a new transformation
      * @param translation Translation portion of this transformation (applied last) (default = zero vector)
      * @param scaling Scaling portion of this transformation (default = identity vector)
      * @param rotation Rotation portion of this transformation (default = 0 degrees)
      * @param shear Shear portion of this transformation (default = zero vector)
      * @return A new affine transformation
      */
    def apply(translation: Vector2D = Vector2D.zero, scaling: Vector2D = Vector2D.identity,
              rotation: Rotation = Rotation.zero, shear: Vector2D = Vector2D.zero): AffineTransformation =
        apply(translation, LinearTransformation(scaling, rotation, shear))
    
    /**
      * @param amount Translation to apply
      * @return An affine transformation that only translates instances
      */
    def translation(amount: Vector2D) = apply(amount, LinearTransformation.identity)
}

/**
 * A transformation state that consists of a linear transformation (scaling, rotation, shear) and a transition
 * @author Mikko Hilpinen
 * @since 26.12.2020, v2.4
 */
case class AffineTransformation(translation: Vector2D, linear: LinearTransformation)
    extends LinearTransformationLike[AffineTransformation] with JavaAffineTransformConvertible
        with ApproximatelyEquatable[AffineTransformation] with AffineTransformable[Matrix3D]
        with LinearTransformable[Matrix3D] with AnimatedLinearTransformable[AnimatedAffineTransformation]
        with AnimatedAffineTransformable[AnimatedAffineTransformation]
{
    // ATTRIBUTES   -----------------
    
    /**
      * A matrix representation of this transformation
      */
    lazy val toMatrix = Matrix3D.affineTransform(linear.toMatrix, translation)
    
    
    // COMPUTED PROPERTIES    -------
    
    /**
      * @return Whether this transformation is really a linear transformation
      */
    def isLinear = translation.isZero
    
    /**
     * The translation component of this transformation as a point
     */
    def position = translation.toPoint
    
    
    // IMPLEMENTED  -----------------
    
    override def toString =
    {
        if (translation.nonZero)
        {
            val translationSegment = s"Translation $translation"
            if (linear.isIdentity)
                translationSegment
            else
                s"$translationSegment & $linear"
        }
        else
            linear.toString
    }
    
    override def scaling = linear.scaling
    
    override def shear = linear.shear
    
    override protected def buildCopy(scaling: Vector2D, rotation: Rotation, shear: Vector2D) =
        copy(linear = LinearTransformation(scaling, rotation, shear))
    
    override def rotation = linear.rotation
    
    override def toJavaAffineTransform = toMatrix.toJavaAffineTransform
    
    override def ~==(other: AffineTransformation) = (translation ~== other.translation) && (linear ~== other.linear)
    
    override def transformedWith(transformation: Matrix3D) = toMatrix.transformedWith(transformation)
    
    override def transformedWith(transformation: Matrix2D) = toMatrix.transformedWith(transformation)
    
    override def transformedWith(transformation: Animation[Matrix2D]) =
        AnimatedAffineTransformation { p => this * transformation(p) }
    
    override def affineTransformedWith(transformation: Animation[Matrix3D]) =
        AnimatedAffineTransformation { p => transformation(p)(toMatrix) }
    
    
    // OPERATORS    -----------------
    
    /**
     * A negative copy of this transformation. Please note that this is not the inverse of this transformation, as
      * the order of the applied transformations is still the same
     */
    def unary_- = AffineTransformation(-translation, -linear)
    
    /**
      * Combines these two transformations together. <b>This is not the same as applying these transformations
      * back to back</b>, rather it simply sums the values (translation, scaling, rotation, shear) of each transformation
      */
    def +(other: AffineTransformation) = AffineTransformation(translation + other.translation, linear + other.linear)
    
    /**
      * @param other Another linear transformation
      * @return A subtraction of these transformations. <b>Please note that this isn't the same as applying the
      *         other transformation's invert, which would cancel the transformation</b>. Only the sums of the
      *         translation, scaling, rotation and shearing are affected.
      */
    def -(other: AffineTransformation) = this + (-other)
    
    /**
      * Transforms a vector into this transformed coordinate system
      * @param vector a (relative) vector that will be transformed to this coordinate system
      */
    def apply[V <: Vector2DLike[V]](vector: V) = vector * toMatrix
    
    
    // OTHER METHODS    -------------
    
    /**
      * Inverse transforms the specified vector, negating the effects of this transformation / coordinate system
      * @param vector a vector in this coordinate system
      * @return The (relative) vector that would produce the specified vector when transformed. None if this
      *         transformation maps all vectors to a single line or a point (scaling of 0 was applied)
      */
    def invert[V <: Vector2DLike[V]](vector: V) = toMatrix.inverse.map { vector * _ }
    
    /**
      * Rotates this transformation around the specified point or origin
      * @param origin Point around which this transformation is rotated (in transformed coordinate system)
      * @param rotation Rotation to apply
      * @tparam V Type of origin point vector
      * @return Rotated copy of this transformation
      */
    def rotatedAround[V <: Vector2DLike[V]](origin: V, rotation: Rotation) =
    {
        if (rotation.isZero)
            toMatrix
        else if (origin.isZero)
            rotated(rotation)
        else
        {
            // Translates so that the origin is at (0,0), then rotates and then translates back
            translated(-origin).rotated(rotation).translated(origin)
        }
    }
    
    /**
      * Rotates this transformation around the specified point or origin
      * @param origin Point around which this transformation is rotated (in pre-transformed coordinate system)
      * @param rotation Rotation to apply
      * @tparam V Type of origin point vector
      * @return Rotated copy of this transformation
      */
    def rotatedAroundRelative[V <: Vector2DLike[V]](origin: V, rotation: Rotation) =
        rotatedAround(apply(origin), rotation)
    
    /**
     * Copies this transformation, giving it a new translation vector
     */
    def withTranslation(translation: Vector2D) = copy(translation = translation)
    
    /**
      * @param translation A new translation value
      * @return A copy of this transformation with new translation
      */
    def withTranslation(translation: Dimensional[Double]) =
        copy(translation = Vector2D.withDimensions(translation.dimensions))
    
    /**
     * Copies this transformation, giving it a new position
     */
    def withPosition(position: Point) = withTranslation(position.toVector)
    
    /**
      * @param transformable An instance to transform
      * @tparam A Type of transformation result
      * @return Transformation result
      */
    def transform[A](transformable: AffineTransformable[A]) = transformable.transformedWith(toMatrix)
}