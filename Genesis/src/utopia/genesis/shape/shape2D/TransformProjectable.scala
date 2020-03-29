package utopia.genesis.shape.shape2D

import utopia.genesis.shape.{Rotation, Vector3D, VectorLike}

/**
 * Transformations can be applied to (immutable) transformable shapes / elements
 * @tparam T The type of object that results from a transformation
 * @author Mikko Hilpinen
 * @since 9.7.2017
 */
trait TransformProjectable[+T]
{
    // ABSTRACT --------------------------
    
    /**
     * Transforms this instance with the specified transformation
     */
    def transformedWith(transformation: Transformation): T
    
    
    // OTHER    --------------------------
    
    /**
      * @param translation Amount of translation applied
      * @return A translated copy of this item
      */
    def translated(translation: Vector3D) = transformedWith(Transformation.translation(translation))
    
    /**
      * @param rotation Amount of rotation applied
      * @return A rotated copy of this item
      */
    def rotated(rotation: Rotation) = transformedWith(Transformation.rotation(rotation))
    
    /**
      * @param scaling Amount of scaling applied (1 keeps this instance as is)
      * @return A scaled copy of this item
      */
    def scaled(scaling: Double) = transformedWith(Transformation.scaling(scaling))
    
    /**
      * @param scaling Amount of scaling applied (on each axis)
      * @return A scaled copy of this item
      */
    def scaled(scaling: Vector3D) = transformedWith(Transformation.scaling(scaling))
    
    /**
      * @param rotation Amount of rotation applied
      * @return A rotated copy of this item
      */
    def +(rotation: Rotation) = rotated(rotation)
}