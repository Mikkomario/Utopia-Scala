package utopia.paradigm.shape.shape2d

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.DoubleType
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.paradigm.generic.ParadigmDataType.{PointType, Vector2DType}
import utopia.paradigm.generic.ParadigmValue._

import scala.util.Try

object Rectangle extends FromModelFactory[Rectangle]
{
	/**
	  * A rectangle at origin position with zero size
	  */
	val zero = Rectangle(Point.origin, Vector2D.zero, 0)
	
	/**
	  * A schema used when converting models to rectangles
	  */
	val schema = ModelDeclaration("topLeft" -> PointType, "top" -> Vector2DType, "rightEdgeLength" -> DoubleType)
	
	override def apply(model: template.ModelLike[Property]): Try[Rectangle] = {
		schema.validate(model).toTry.map { valid =>
			Rectangle(valid("topLeft").getPoint, valid("top").getVector2D, valid("rightEdgeLength").getDouble)
		}
	}
}

/**
  * Rectangles are 2D shapes that have four sides and where each corner is 90 degrees
  * @param topLeftCorner The top left corner of this rectangle
  * @param topEdge The top vector of this rectangle
  * @param rightEdgeLength The length of the right edge of this rectangle
  */
case class Rectangle(topLeftCorner: Point, topEdge: Vector2D, rightEdgeLength: Double)
	extends Rectangular with ModelConvertible
{
	// ATTRIBUTES	-----------------
	
	override lazy val rightEdge = super.rightEdge
	
	
	// COMPUTED	---------------------
	
	/**
	  * @return The size of this rectangle (rotation lost)
	  */
	def toSize = Size(topEdge.length, rightEdgeLength)
	/**
	  * @return The bounds of this rectangle (rotation lost)
	  */
	def toBounds = Bounds(topLeftCorner, toSize)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override def toModel =
		Model(Vector(("topLeft", topLeftCorner), ("top", topEdge), ("rightEdgeLength", rightEdgeLength)))
}
