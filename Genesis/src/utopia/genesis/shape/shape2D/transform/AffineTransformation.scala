package utopia.genesis.shape.shape2D.transform

import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.{JavaAffineTransformConvertible, Point, Vector2D}
import utopia.genesis.shape.shape3D.Matrix3D
import utopia.genesis.shape.template.Dimensional
import utopia.genesis.util.ApproximatelyEquatable

/**
 * A transformation state that consists of a linear transformation (scaling, rotation, shear) and a transition
 * @author Mikko Hilpinen
 * @since 26.12.2020, v2.4
 */
case class AffineTransformation(translation: Vector2D = Vector2D.zero, linear: LinearTransformation)
    extends LinearTransformationLike[AffineTransformation] with JavaAffineTransformConvertible
        with ApproximatelyEquatable[AffineTransformation]
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
    
    override def scaling = linear.scaling
    
    override def shear = linear.shear
    
    override protected def buildCopy(scaling: Vector2D, rotation: Rotation, shear: Vector2D) =
        copy(linear = LinearTransformation(scaling, rotation, shear))
    
    override def rotation = linear.rotation
    
    override def toJavaAffineTransform = toMatrix.toJavaAffineTransform
    
    override def ~==(other: AffineTransformation) = (translation ~== other.translation) && (linear ~== other.linear)
    
    
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
    def apply(vector: Dimensional[Double]) = toMatrix(vector)
    
    
    // OTHER METHODS    -------------
    
    /**
      * Inverse transforms the specified vector, negating the effects of this transformation / coordinate system
      * @param vector a vector in this coordinate system
      * @return The (relative) vector that would produce the specified vector when transformed. None if this
      *         transformation maps all vectors to a single line or a point (scaling of 0 was applied)
      */
    def invert(vector: Dimensional[Double]) = toMatrix.inverse.map { _(vector) }
    /*
    /**
     * Rotates the transformation around an absolute origin point
     * @param rotation the amount of rotation applied to this transformation
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    def absoluteRotated(rotation: Rotation, origin: Point) = 
            withTranslation(translation.rotatedAround(rotation, origin.toVector)).rotated(rotation)
    
    /**
     * Rotates the transformation around a relative origin point
     * @param rotation the amount of rotation applied to this transformation
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    def relativeRotated(rotation: Rotation, origin: Point) = absoluteRotated(rotation, apply(origin))
    */
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
}