package utopia.paradigm.shape.shape2d.vector

import utopia.flow.collection.CollectionExtensions.RichSeqLike
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.generic.ParadigmDataType.Vector2DType
import utopia.paradigm.motion.motion2d.Velocity2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorFactory, DoubleVectorLike}
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

import scala.concurrent.duration.Duration

object Vector2D extends DoubleVectorFactory[Vector2D] with SureFromModelFactory[Vector2D]
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * A (0,0) vector
	  */
	override val zero = super.zero
	/**
	  * A (1,1) vector
	  */
	val identity = apply(1, 1)
	/**
	  * A (1,0) vector
	  */
	val unit = apply(1)
	
	
	// IMPLEMENTED  --------------------------
	
	override def parseFrom(model: template.ModelLike[Property]) =
		apply(model("x").getDouble, model("y").getDouble)
	
	override def apply(dimensions: Dimensions[Double]) = new Vector2D(dimensions.withLength(2))
	override def from(other: HasDimensions[Double]) = other match {
		case v: Vector2D => v
		case o => apply(o.dimensions)
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * Converts a coordinate map into a vector
	  */
	@deprecated("Replaced with apply", "v1.2")
	def of(map: Map[Axis2D, Double]) = Vector2D(map.getOrElse(X, 0), map.getOrElse(Y, 0))
	
	/**
	  * @param dimensions A set of dimensions
	  * @return A 2D vector from those dimensions (uses the first 2 dimensions)
	  */
	@deprecated("Replaced with apply", "v1.2")
	def withDimensions(dimensions: Seq[Double]) = Vector2D(dimensions.headOption.getOrElse(0.0),
		dimensions.getOrElse(1, 0.0))
}

/**
  * A 2-dimensional vector
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  */
class Vector2D private(override val dimensions: Dimensions[Double])
	extends DoubleVectorLike[Vector2D] with DoubleVector with ValueConvertible with ModelConvertible with EqualsBy
{
	// COMPUTED   -----------------------------
	
	/**
	  * @return A 3D-copy of this vector
	  */
	def in3D = Vector3D.from(this)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def self = this
	override protected def factory = Vector2D
	
	override def zero = Vector2D.zero
	
	override protected def equalsProperties = Single(dimensions)
	
	override implicit def toValue: Value = new Value(Some(this), Vector2DType)
	override def toModel = Model.from("x" -> x, "y" -> y)
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param duration Time period
	  * @return A velocity that represents this movement in specified duration
	  */
	def traversedIn(duration: Duration) = Velocity2D(this, duration)
}
