package utopia.genesis.generic

import utopia.genesis.shape.Vector3D
import utopia.flow.datastructure.immutable.Value
import utopia.genesis.shape.shape2D.{Bounds, Circle, Line, Point, Rectangle, Size, Transformation}

object GenesisValue
{
    implicit class GValue(val v: Value) extends AnyVal
    {
        /**
         * A 3D vector value of this value. None if the value couldn't be casted.
         */
        def vector3D = v.objectValue(Vector3DType).map { _.asInstanceOf[Vector3D] }
        
        /**
         * A 2D point value of this value. None if this value couldn't be casted
         */
        def point = v.objectValue(PointType).map { _.asInstanceOf[Point] }
        
        /**
         * A line value of this value. None if the value couldn't be casted.
         */
        def line = v.objectValue(LineType).map { _.asInstanceOf[Line] }
        
        /**
         * A size value of this value. None if this value couldn't be casted
         */
        def size = v.objectValue(SizeType).map { _.asInstanceOf[Size] }
        
        /**
         * A circle value of this value. None if the value couldn't be casted.
         */
        def circle = v.objectValue(CircleType).map { _.asInstanceOf[Circle] }
        
        /**
         * A bounds value of this value. None if the value couldn't be casted.
         */
        def bounds = v.objectValue(BoundsType).map { _.asInstanceOf[Bounds] }
        
        /**
         * A transformation value of this value. None if the value couldn't be casted.
         */
        def transformation = v.objectValue(TransformationType).map { _.asInstanceOf[Transformation] }
        
        /**
         * The vector value of this value, or the provided default value in case the value couldn't
         * be cast.
         * @param default The default vector value. Defaults to a zero vector.
         */
        def vector3DOr(default: => Vector3D = Vector3D.zero) = vector3D.getOrElse(default)
        
        /**
         * The point value of this value, or the provided default value in case casting failed
         */
        def pointOr(default: => Point = Point.origin) = point.getOrElse(default)
        
        /**
         * The line value of this value, or the provided default value in case the value couldn't
         * be cast.
         * @param default The default line value. Defaults to a line from zero to zero.
         */
        def lineOr(default: => Line = Line.zero) = line.getOrElse(default)
        
        /**
         * The size value of this value, or the provided default value if casting failed
         */
        def sizeOr(default: => Size = Size.zero) = size.getOrElse(default)
        
        /**
         * The circle value of this value, or the provided default value in case the value couldn't
         * be cast.
         * @param default The default circle value. Defaults to a circle at zero origin with zero
         * radius.
         */
        def circleOr(default: => Circle = Circle(Point.origin, 0)) = circle.getOrElse(default)
        
        /**
         * The bounds value of this value, or the provided default value in case the value
         * couldn't be cast.
         * @param default the default bounds value. Defaults to bounds with zero position and
         * size.
         */
        def boundsOr(default: => Bounds = Bounds.zero) = bounds.getOrElse(default)
        
        /**
         * The transformation value of this value, or the provided default value in case the value
         * couldn't be cast.
         * @param default The default transformation value. Defaults to identity transformation,
         * which doesn't modify an object's state
         */
        def transformationOr(default: => Transformation = Transformation.identity) = 
                transformation.getOrElse(default)
    
        /**
          * @return 3D Vector o this value or a zero vector
          */
        def getVector3D = vector3DOr()
    
        /**
          * @return Point of this value or a (0, 0) point
          */
        def getPoint = pointOr()
    
        /**
          * @return Size of this value or 0 size
          */
        def getSize = sizeOr()
    
        /**
          * @return Line of this value or a 0 -> 0 line
          */
        def getLine = lineOr()
    
        /**
          * @return Circle of this value or a 0 sized circle
          */
        def getCircle = circleOr()
    
        /**
          * @return Bounds of this value or a 0 bounds
          */
        def getBounds = boundsOr()
    
        /**
          * @return Transformation of this value or an identity transformation
          */
        def getTransformation = transformationOr()
    }
}