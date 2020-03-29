package utopia.genesis.shape.shape2D

import java.awt.geom.AffineTransform

import utopia.flow.generic.ValueConvertible
import utopia.flow.datastructure.immutable.Value
import utopia.genesis.generic.TransformationType
import utopia.flow.generic.ModelConvertible
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.FromModelFactory
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.genesis.generic.GenesisValue._
import utopia.genesis.shape.{Rotation, Vector3D, VectorLike}

import scala.util.Success

object Transformation extends FromModelFactory[Transformation]
{
    // ATTRIBUTES    -----------------
    
    /**
     * This transformation preserves the state of the target without transforming it
     */
    val identity = Transformation()
    
    
    // OPERATORS    -----------------
    
    override def apply(model: template.Model[Property]) = Success(Transformation(
            model("translation").getVector3D, model("scaling").vector3DOr(Vector3D.identity),
            Rotation(model("rotation").getDouble), model("shear").getVector3D))
    
    
    // OTHER METHODS    --------------
    
    /**
     * This transformation moves the coordinates of the target by the provided amount
     */
    def translation(amount: Vector3D) = Transformation(translation = amount)
    
    /**
      * @param x Translation x-wise
      * @param y Translation y-wise
      * @return A new translation transformation
      */
    def translation(x: Double, y: Double): Transformation = translation(Vector3D(x, y))
    
    /**
      * @param amount Translation amount (position)
      * @return A transformation that sets an object to specified position
      */
    def position(amount: Point) = translation(amount.toVector)
    
    /**
     * This transformation scales the target by the provided amount
     */
    def scaling(amount: Vector3D) = Transformation(scaling = amount)
    
    /**
     * This transformation scales the target by the provided amount. Each coordinate is scaled with
     * the same factor 
     */
    def scaling(amount: Double) = Transformation(scaling = Vector3D(amount, amount, amount))
    
    /**
     * This transformation rotates the target around the zero origin (z-axis) by the provided amount
     */
    def rotation(amount: Rotation) = Transformation(rotation = amount)
    
    /**
     * This transformation rotates the target around the zero origin by the provided amount of
     * radians. Rotation is made in clockwise direction.
     */
    def rotationRads(amountRads: Double) = rotation(Rotation ofRadians amountRads)
    
    /**
     * This transformation rotates the target around the zero origin by the provided amount of
     * degrees. Rotation is made in clockwise direction.
     */
    def rotationDegs(amountDegs: Double) = rotation(Rotation ofDegrees amountDegs)
    
    /**
     * This transformation shears the target by the provided amount
     */
    def shear(amount: Vector3D) = Transformation(shear = amount)
}

/**
 * Transformations represent an object's state in a world, it's angle, position, scaling, etc.
 * Transformations can be applied over each other or points, lines, etc.
 * @author Mikko Hilpinen
 * @since 29.12.2016
 */
case class Transformation(translation: Vector3D = Vector3D.zero, scaling: Vector3D = Vector3D.identity,
                          rotation: Rotation = Rotation.zero, shear: Vector3D = Vector3D.zero,
                          useReverseOrder: Boolean = false) extends ValueConvertible with ModelConvertible
{
    // COMPUTED PROPERTIES    -------
    
    override def toValue = new Value(Some(this), TransformationType)
    
    // TODO: Handle rotation data type
    override def toModel = Model(Vector("translation" -> translation, "scaling" -> scaling, 
            "rotation" -> rotation.toDouble, "shear" -> shear))
    
    /**
     * The translation component of this transformation as a point
     */
    def position = translation.toPoint
    
    /**
     * The rotation component of this transformation as an angle
     */
    def angle = rotation.toAngle
    
    /**
     * How much the target is rotated clockwise in radians
     */
    def rotationRads = rotation.toDouble
    
    /**
     * How much the target is rotated in degrees (clockwise)
     */
    def rotationDegs = rotationRads.toDegrees
    
    /**
     * This transformation represented as an affine transform instance. A new instance is generated 
     * on each call since it is mutable while transformation is not
     */
    def toAffineTransform = 
    {
        val t = new AffineTransform()
        
        // Reverse transformations do the transforms in reverse order too
        if (useReverseOrder)
        {
            t.shear(shear.x, shear.y)
            t.scale(scaling.x, scaling.y)
            t.rotate(rotationRads)
            t.translate(translation.x, translation.y)
        }
        else
        {
            t.translate(translation.x, translation.y)
            t.rotate(rotationRads)
            t.scale(scaling.x, scaling.y)
            t.shear(shear.x, shear.y)
        }
        t
    }
    
    /**
     * Converts this transform instance into an affine transform and inverts it
     */
    def toInvertedAffineTransform = 
    {
        val t = toAffineTransform
        
        // If the transformation can't be inverted, simply inverts the position
        if (t.getDeterminant != 0)
        {
            t.invert()
            t
        }
        else
        {
            Transformation(-translation).toAffineTransform
        }
    }
    
    /**
     * The translation component of this transformation
     */
    def translationTransformation = Transformation.translation(translation)
    
    /**
     * The scaling component of this transformation
     */
    def scalingTransformation = Transformation.scaling(scaling)
    
    /**
     * The rotation component of this transformation
     */
    def rotationTransformation = Transformation.rotation(rotation)
    
    /**
     * The shear component of this transformation
     */
    def shearTransformation = Transformation.shear(shear)
    
    
    // OPERATORS    -----------------
    
    /**
     * Inverts this transformation
     */
    def unary_- = Transformation(-translation, Vector3D.identity / scaling, -rotation, -shear, !useReverseOrder)
    
    /**
     * Combines the two transformations together. The applied translation is not depended of the 
     * scaling or rotation of this transformation. If you want the results of applying first this
     * transformation and then the second, use apply(Transformation) instead.
     */
    def +(other: Transformation) = Transformation(translation + other.translation, 
            scaling * other.scaling, rotation + other.rotation, shear + other.shear, useReverseOrder)
    
    /*
     * Negates a transformation from this transformation
     */
    // def -(other: Transformation) = this + (-other)
    
    /**
     * Checks whether the two transformations are practically (approximately) identical with each
     * other
     */
    def ~==(other: Transformation) = (translation ~== other.translation) && (scaling ~== other.scaling) && 
            (rotation ~== other.rotation) && (shear ~== other.shear)
    
    /**
     * Transforms a <b>relative</b> point <b>into an absolute</b> point
     * @param relative a relative point that will be transformed
     */
    def apply(relative: Point) = Point of toAffineTransform.transform(relative.toAwtPoint2D, null)
    
    /**
     * Transforms a <b>relative</b> point <b>into an absolute</b> point
     * @param relative a relative point that will be transformed
     */
    def apply(relative: Vector3D): Vector3D = apply(relative.toPoint).toVector
    
    /**
     * Transforms a shape <b>from relative space to absolute space</b>
     */
    def apply[B](relative: TransformProjectable[B]) = relative.transformedWith(this)
    
    /**
     * Combines these two transformations together. The end result is effectively same as transforming
     * a target with the provided transformation 'other', then with 'this' transformation.<p>
     * Please notice that the scaling and rotation affect the scaling and translation applied (for
     * example, adding translation of (1, 0, 0) to a transformation with
     * zero position and scaling of 2 will create a transformation with (2, 0, 0) position and
     * scaling 2
     */
    def apply(other: Transformation): Transformation = (this + other).withTranslation(apply(other.translation))
    
    
    // OTHER METHODS    -------------
    
    /**
     * Inverse transforms an <b>absolute</b> coordinate point <b>into relative</b> space
     * @param absolute a vector in absolute world space
     * @return The absolute point in relative world space
     */
    def invert(absolute: Point) = Point of toInvertedAffineTransform.transform(absolute.toAwtPoint2D, null)
    
    /**
     * Inverse transforms an <b>absolute</b> coordinate point <b>into relative</b> space
     * @param absolute a vector in absolute world space
     * @return The absolute point in relative world space
     */
    def invert(absolute: Vector3D): Vector3D = invert(absolute.toPoint).toVector
    
    /**
     * Transforms a shape <b>from absolute space to relative space</b>
     */
    def invert[B](absolute: TransformProjectable[B]) = absolute.transformedWith(-this)
    
    /**
     * Converts an absolute coordinate into a relative one. Same as calling invert(Vector3D)
     */
    def toRelative(absolute: Point) = invert(absolute)
    
    /**
     * Converts an absolute coordinate into a relative one. Same as calling invert(Vector3D)
     */
    def toRelative(absolute: Vector3D) = invert(absolute)
    
    /**
     * Converts an absolute shape to a relative one. Same as calling invert(...)
     */
    def toRelative[B](absolute: TransformProjectable[B]) = invert(absolute)
    
    /**
     * Converts a relative coordinate into an absolute one. Same as calling apply(Point)
     */
    def toAbsolute(relative: Point) = apply(relative)
    
    /**
     * Converts a relative coordinate into an absolute one. Same as calling apply(Point)
     */
    def toAbsolute(relative: Vector3D) = apply(relative)
    
    /**
     * Converts a relative shape to an absolute one. Same as calling apply(...)
     */
    def toAbsolute[B](relative: TransformProjectable[B]) = apply(relative)
    
    /**
     * Rotates the transformation around an absolute origin point
     * @param rotation the amount of rotation applied to this transformation
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    def absoluteRotated(rotation: Rotation, origin: Point) = 
            withTranslation(translation.rotated(rotation, origin.toVector)).rotated(rotation)
    
    /**
     * Rotates the transformation around an absolute origin point
     * @param rotationRads the amount of radians the transformation is rotated (clockwise)
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    @deprecated("Please start using absoluteRotated instead", "v1.1.2")
    def absoluteRotatedRads(rotationRads: Double, origin: Point) = absoluteRotated(Rotation ofRadians rotationRads, origin)
    
    /**
     * Rotates the transformation around an absolute origin point
     * @param rotationDegs the amount of degrees the transformation is rotated (clockwise)
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    @deprecated("Please start using absoluteRotated instead", "v1.1.2")
    def absoluteRotatedDegs(rotationDegs: Double, origin: Point) = absoluteRotated(
            Rotation ofDegrees rotationDegs, origin)
    
    /**
     * Rotates the transformation around a relative origin point
     * @param rotation the amount of rotation applied to this transformation
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    def relativeRotated(rotation: Rotation, origin: Point) = absoluteRotated(rotation, apply(origin))
    
    /**
     * Rotates the transformation around a relative origin point
     * @param rotationRads the amount of radians the transformation is rotated (clockwise)
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    @deprecated("Please start using relativeRotated instead", "v1.1.2")
    def relativeRotatedRads(rotationRads: Double, origin: Point) = relativeRotated(Rotation ofRadians rotationRads, origin)
    
    /**
     * Rotates the transformation around an relative origin point
     * @param rotationDegs sthe amount of degrees the transformation is rotated (clockwise)
     * @param origin the point of origin around which the transformation is rotated
     * @return the rotated transformation
     */
    @deprecated("Please start using relativeRotated instead", "v1.1.2")
    def relativeRotatedDegs(rotationDegs: Double, origin: Point) = relativeRotated(Rotation ofDegrees rotationDegs, origin)
    
    /**
     * Copies this transformation, giving it a new translation vector
     */
    def withTranslation(translation: Vector3D) = copy(translation = translation)
    
    /**
     * Copies this transformation, giving it a new position
     */
    def withPosition(position: Point) = withTranslation(position.toVector)
    
    /**
     * Copies this transformation, giving it a new scaling
     */
    def withScaling(scaling: Vector3D) = copy(scaling = scaling)
    
    /**
     * Copies this transformation, giving it a new scaling
     */
    def withScaling(scaling: Double): Transformation = withScaling(Vector3D(scaling, scaling, scaling))
    
    /**
     * Copies this transformation, using a different rotation
     */
    def withRotation(rotation: Rotation) = copy(rotation = rotation)
    
    /**
     * Copies this transformation, giving it a new rotation (clockwise)
     */
    @deprecated("Please use withRotation instead", "v1.1.2")
    def withRotationRads(rotationRads: Double) = withRotation(Rotation ofRadians rotationRads)
    
    /**
     * Copies this transformation, giving it a new rotation (clockwise)
     */
    @deprecated("Please use withRotation instead", "v1.1.2")
    def withRotationDegs(rotationDegs: Double) = withRotation(Rotation ofDegrees rotationDegs)
    
    /**
     * Copies this transformation, giving it a new shearing
     */
    def withShear(shear: Vector3D) = copy(shear = shear)
    
    /**
     * Copies this transformation, changing the translation by the provided amount
     */
    def translated(translation: VectorLike[_]) = withTranslation(this.translation + translation)
    
    /**
     * Copies this transformation, changing the scaling by the provided amount
     */
    def scaled(scaling: VectorLike[_]) = withScaling(this.scaling * scaling)
    
    /**
     * Copies this transformation, changing the scaling by the provided amount
     */
    def scaled(scaling: Double) = withScaling(this.scaling * scaling)
    
    /**
     * Copies this transformation, changing rotation by specified amount
     */
    def rotated(rotation: Rotation) = withRotation(this.rotation + rotation)
    
    /**
     * Copies this transformation, changing the rotation by the provided amount (clockwise)
     */
    @deprecated("Please use rotated instead", "v1.1.2")
    def rotatedRads(rotationRads: Double) = rotated(Rotation ofRadians rotationRads)
    
    /**
     * Copies this transformation, changing the rotation by the provided amount (clockwise)
     */
    @deprecated("Please use rotated instead", "v1.1.2")
    def rotatedDegs(rotationDegs: Double) = rotated(Rotation ofDegrees rotationDegs)
    
    /**
     * Copies this transformation, changing the shearing by the provided amount
     */
    def sheared(shearing: VectorLike[_]) = withShear(shear + shearing)
}