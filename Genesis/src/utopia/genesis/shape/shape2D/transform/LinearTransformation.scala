package utopia.genesis.shape.shape2D.transform

import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.{AffineTransformable, JavaAffineTransformConvertible, LinearTransformable, Matrix2D, Transformation, Vector2D}
import utopia.genesis.shape.shape3D.Matrix3D
import utopia.genesis.shape.template.Dimensional
import utopia.genesis.util.ApproximatelyEquatable

object LinearTransformation
{
    /**
      * This transformation scales the target by the provided amount
      */
    def scaling(amount: Vector2D) = Transformation(scaling = amount)
    
    /**
      * This transformation scales the target by the provided amount. Each coordinate is scaled with
      * the same factor
      */
    def scaling(amount: Double) = Transformation(scaling = Vector2D(amount, amount))
    
    /**
      * This transformation rotates the target around the zero origin (z-axis) by the provided amount
      */
    def rotation(amount: Rotation) = Transformation(rotation = amount)
    
    /**
      * This transformation shears the target by the provided amount
      */
    def shear(amount: Vector2D) = Transformation(shear = amount)
}

/**
 * A transformation state that consists of a scaling, a rotation and a shear factors
 * @author Mikko Hilpinen
 * @since 26.12.2020, v2.4
 */
case class LinearTransformation(scaling: Vector2D = Vector2D.identity, rotation: Rotation = Rotation.zero,
                                shear: Vector2D = Vector2D.zero) extends LinearTransformationLike[LinearTransformation]
    with JavaAffineTransformConvertible with LinearTransformable[Matrix2D] with AffineTransformable[Matrix3D]
    with ApproximatelyEquatable[LinearTransformation]
{
    // ATTRIBUTES   -----------------
    
    /**
      * A matrix representation of this transformation
      */
    lazy val toMatrix =
    {
        val base = Matrix2D(
            scaling.x, shear.x,
            shear.y, scaling.y)
        if (rotation.isZero)
            base
        else
            base.rotated(rotation)
    }
    
    
    // IMPLEMENTED  -----------------
    
    override protected def buildCopy(scaling: Vector2D, rotation: Rotation, shear: Vector2D) =
        LinearTransformation(scaling, rotation, shear)
    
    override def toJavaAffineTransform = toMatrix.toJavaAffineTransform
    
    override def transformedWith(transformation: Matrix2D) = toMatrix.transformedWith(transformation)
    
    override def transformedWith(transformation: Matrix3D) = toMatrix.transformedWith(transformation)
    
    /**
      * Checks whether the two transformations are practically (approximately) identical with each
      * other
      */
    override def ~==(other: LinearTransformation) = (scaling ~== other.scaling) &&
        (rotation ~== other.rotation) && (shear ~== other.shear)
    
    
    // OPERATORS    -----------------
    
    /**
     * A negative copy of this transformation. Please note that this is not necessarily the inverse of this
      * transformation, as the order of the individual operations is not affected.
     */
    def unary_- = LinearTransformation(Vector2D.identity / scaling, -rotation, -shear)
    
    /**
     * Transforms a vector into this transformed coordinate system
     * @param vector a (relative) vector that will be transformed to this coordinate system
     */
    def apply(vector: Dimensional[Double]) = toMatrix(vector)
    
    /**
      * @param other Another linear transformation
      * @return A combination of these transformations where the other transformation is applied first and then
      *         this transformation is applied
      */
    def apply(other: LinearTransformation) = toMatrix(other.toMatrix)
    
    
    // OTHER METHODS    -------------
    
    /**
     * Inverse transforms the specified vector, negating the effects of this transformation / coordinate system
     * @param vector a vector in this coordinate system
     * @return The (relative) vector that would produce the specified vector when transformed. None if this
      *         transformation maps all vectors to a single line or a point (scaling of 0 was applied)
     */
    def invert(vector: Dimensional[Double]) = toMatrix.inverse.map { _(vector) }
}