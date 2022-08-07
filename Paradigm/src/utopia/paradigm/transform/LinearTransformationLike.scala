package utopia.paradigm.transform

import utopia.paradigm.angular.Rotation
import utopia.paradigm.shape.shape2d.Vector2D

/**
 * A transformation state that consists of a scaling, a rotation and a shear factors
 * @author Mikko Hilpinen
 * @since Genesis 26.12.2020, v2.4
 */
trait LinearTransformationLike[+Repr]
{
    // ABSTRACT ---------------------
    
    /**
      * @return Scaling applied by this transformation
      */
    def scaling: Vector2D
    
    /**
      * @return Rotation applied by this transformation
      */
    def rotation: Rotation
    
    /**
      * @return Shearing applied by this transformation
      */
    def shear: Vector2D
    
    /**
      * @param scaling New scaling
      * @param rotation New rotation
      * @param shear New shear
      * @return A copy of this transformation with specified values
      */
    protected def buildCopy(scaling: Vector2D, rotation: Rotation, shear: Vector2D): Repr
    
    
    // COMPUTED PROPERTIES    -------
    
    /**
     * The rotation component of this transformation as an angle
     */
    def angle = rotation.toAngle
    
    /**
      * @return Copy of this transformation with only scaling applied
      */
    def onlyScaling = LinearTransformation.scaling(scaling)
    
    /**
      * @return Copy of this transformation with only rotation applied
      */
    def onlyRotation = LinearTransformation.rotation(rotation)
    
    /**
      * @return Copy of this transformation with only shearing applied
      */
    def onlyShear = LinearTransformation.shear(shear)
    
    
    // OPERATORS    -----------------
    
    /**
     * Combines these two transformations together. <b>This is not the same as applying these transformations
      * back to back</b>, rather it simply sums the values (scaling, rotation, shear) of each transformation
     */
    def +(other: LinearTransformation) = buildCopy(scaling * other.scaling, rotation + other.rotation,
        shear + other.shear)
    
    /**
      * @param rotation Amount of rotation to add to this transformation
      * @return A rotated copy of this transformation
      */
    def +(rotation: Rotation) = buildCopy(scaling, this.rotation + rotation, shear)
    
    /**
      * @param other Another linear transformation
      * @return A subtraction of these transformations. <b>Please note that this isn't the same as applying the
      *         other transformation's invert, which would cancel the transformation</b>. Only the sums of the scaling,
      *         rotation and shearing are affected.
      */
    def -(other: LinearTransformation) = this + (-other)
    
    
    // OTHER METHODS    -------------
    
    /**
     * Copies this transformation, giving it a new scaling
     */
    def withScaling(scaling: Vector2D) = buildCopy(scaling, rotation, shear)
    
    /**
     * Copies this transformation, giving it a new scaling
     */
    def withScaling(scaling: Double): Repr = withScaling(Vector2D(scaling, scaling))
    
    /**
     * Copies this transformation, using a different rotation
     */
    def withRotation(rotation: Rotation) = buildCopy(scaling, rotation, shear)
    
    /**
     * Copies this transformation, giving it a new shearing
     */
    def withShear(shear: Vector2D) = buildCopy(scaling, rotation, shear)
}