package utopia.paradigm.transform

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.equality.ApproxEquals
import utopia.flow.util.NotEmpty
import utopia.paradigm.angular.{DirectionalRotation, Rotation}
import utopia.paradigm.animation.Animation
import utopia.paradigm.animation.transform.{AnimatedAffineTransformable, AnimatedAffineTransformation, AnimatedLinearTransformable, AnimatedLinearTransformation}
import utopia.paradigm.generic.ParadigmDataType.LinearTransformationType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

import scala.collection.immutable.VectorBuilder

object LinearTransformation extends SureFromModelFactory[LinearTransformation]
{
    // ATTRIBUTES   -----------------------------
    
    /**
      * Identity transformation which doesn't have any effect
      */
    val identity = apply()
    
    
    // IMPLEMENTED  -----------------------------
    
    override def parseFrom(model: template.ModelLike[Property]) =
        apply(model("scaling").vector2DOr(Vector2D.identity), model("rotation").getRotation, model("shear").getVector2D)
    
    
    // OTHER    ---------------------------------
    
    /**
      * This transformation scales the target by the provided amount
      */
    def scaling(amount: Vector2D) = apply(scaling = amount)
    
    /**
      * This transformation scales the target by the provided amount. Each coordinate is scaled with
      * the same factor
      */
    def scaling(amount: Double) = apply(scaling = Vector2D(amount, amount))
    
    /**
      * This transformation rotates the target around the zero origin (z-axis) by the provided amount
      */
    def rotation(amount: DirectionalRotation) = apply(rotation = amount)
    
    /**
      * This transformation shears the target by the provided amount
      */
    def shear(amount: Vector2D) = apply(shear = amount)
}

/**
 * A transformation state that consists of a scaling, a rotation and a shear factors
 * @author Mikko Hilpinen
 * @since Genesis 26.12.2020, v2.4
 */
case class LinearTransformation(scaling: Vector2D = Vector2D.identity,
                                rotation: DirectionalRotation = Rotation.clockwise.zero,
                                shear: Vector2D = Vector2D.zero)
    extends LinearTransformationLike[LinearTransformation] with JavaAffineTransformConvertible
        with LinearTransformable[Matrix2D] with AffineTransformable[Matrix3D]
        with AnimatedLinearTransformable[AnimatedLinearTransformation]
        with AnimatedAffineTransformable[AnimatedAffineTransformation] with ApproxEquals[LinearTransformation]
        with ValueConvertible with ModelConvertible
{
    // ATTRIBUTES   -----------------
    
    /**
      * A matrix representation of this transformation
      */
    lazy val toMatrix = {
        val base = Matrix2D(
            scaling.x, shear.x,
            shear.y, scaling.y)
        if (rotation.isZero)
            base
        else
            base.rotated(rotation)
    }
    
    
    // COMPUTED ---------------------
    
    /**
      * @return Whether this transformation is an identity transformation (which doesn't have any effect)
      */
    def isIdentity = scaling.isIdentity && rotation.isZero && shear.isZero
    
    /**
      * A negative copy of this transformation. Please note that this is not necessarily the inverse of this
      * transformation, as the order of the individual operations is not affected.
      */
    def unary_- = LinearTransformation(Vector2D.identity / scaling, -rotation, -shear)
    
    /**
      * @return This transformation as an affine transformation
      */
    def toAffineTransformation = AffineTransformation(Vector2D.zero, this)
    
    
    // IMPLEMENTED  -----------------
    
    override def identity: Matrix2D = toMatrix
    override def affineIdentity: Matrix3D = toMatrix.to3D
    
    override def toString = {
        val segments = new VectorBuilder[String]
        if (scaling != Vector2D.identity)
            segments += s"Scaling (${scaling.x} x ${scaling.y})"
        if (shear.nonZero)
            segments += s"Shearing (${shear.x} x ${shear.y})"
        if (rotation.nonZero)
            segments += s"Rotation ($rotation)"
        
        NotEmpty(segments.result()) match {
            case Some(segments) => segments.mkString(" & ")
            case None => "Identity transform"
        }
    }
    
    override def toJavaAffineTransform = toMatrix.toJavaAffineTransform
    
    override implicit def toValue: Value = new Value(Some(this), LinearTransformationType)
    override def toModel = Model.from("scaling" -> scaling, "rotation" -> rotation, "shear" -> shear)
    
    override protected def buildCopy(scaling: Vector2D, rotation: DirectionalRotation, shear: Vector2D) =
        LinearTransformation(scaling, rotation, shear)
    
    override def transformedWith(transformation: Matrix2D) = toMatrix.transformedWith(transformation)
    override def transformedWith(transformation: Matrix3D) = toMatrix.transformedWith(transformation)
    override def transformedWith(transformation: Animation[Matrix2D]) =
        AnimatedLinearTransformation { p => transformation(p)(toMatrix) }
    override def affineTransformedWith(transformation: Animation[Matrix3D]) =
        AnimatedAffineTransformation { p => transformation(p)(toMatrix) }
    
    /**
      * Checks whether the two transformations are practically (approximately) identical with each
      * other
      */
    override def ~==(other: LinearTransformation) = (scaling ~== other.scaling) &&
        (rotation ~== other.rotation) && (shear ~== other.shear)
    
    
    // OTHER    -----------------
    
    /**
      * @param translation Translation to add to this transformation
      * @return An affine transformation based on this one, including the specified translation
      */
    def withTranslation(translation: HasDoubleDimensions) = AffineTransformation(Vector2D.from(translation), this)
    
    /**
     * Transforms a vector into this transformed coordinate system
     * @param vector a (relative) vector that will be transformed to this coordinate system
     */
    def apply(vector: HasDoubleDimensions) = toMatrix(vector)
    /**
      * @param other Another linear transformation
      * @return A combination of these transformations where the other transformation is applied first and then
      *         this transformation is applied
      */
    def apply(other: LinearTransformation) = toMatrix(other.toMatrix)
    
    /**
     * Inverse transforms the specified vector, negating the effects of this transformation / coordinate system
     * @param vector a vector in this coordinate system
     * @return The (relative) vector that would produce the specified vector when transformed. None if this
      *         transformation maps all vectors to a single line or a point (scaling of 0 was applied)
     */
    def invert(vector: HasDoubleDimensions) = toMatrix.inverse.map { _(vector) }
    
    /**
      * @param transformable An instance to transform
      * @tparam A Type of transformation result
      * @return Transformation result
      */
    def transform[A](transformable: LinearTransformable[A]) = transformable.transformedWith(toMatrix)
}