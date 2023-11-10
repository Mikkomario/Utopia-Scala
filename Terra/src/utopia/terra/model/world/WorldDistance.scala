package utopia.terra.model.world

import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{ApproxEquals, Combinable, EqualsBy, LinearScalable}
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.measurement.Distance
import utopia.terra.controller.coordinate.world.VectorDistanceConversion

import scala.language.implicitConversions

object WorldDistance
{
	// OTHER    --------------------
	
	/**
	  * @param distance The amount of "real world" distance travelled
	  * @param conversion Implicit distance conversion to use
	  * @return A new distance instance
	  */
	implicit def apply(distance: Distance)(implicit conversion: VectorDistanceConversion): WorldDistance =
		new LazyWorldDistance(Lazy.initialized(distance), Lazy { conversion.vectorLengthOf(distance) })
	/**
	  * @param vectorDistance   The amount of vector distance travelled
	  * @param conversion Implicit distance conversion to use
	  * @return A new distance instance
	  */
	implicit def vector(vectorDistance: Double)(implicit conversion: VectorDistanceConversion): WorldDistance =
		new LazyWorldDistance(Lazy { conversion.distanceOf(vectorDistance) }, Lazy.initialized(vectorDistance))
	
	
	// NESTED   --------------------
	
	private class LazyWorldDistance(d: Lazy[Distance], v: Lazy[Double])(implicit conversion: VectorDistanceConversion)
		extends WorldDistance
	{
		override def distance: Distance = d.value
		override def vectorLength: Double = v.value
		
		override def +(amount: Double): WorldDistance =
			new LazyWorldDistance(Lazy { distance + conversion.distanceOf(amount) }, v.mapCurrent { _ + amount })
		override def +(other: Distance): WorldDistance =
			new LazyWorldDistance(d.mapCurrent { _ + other }, Lazy { vectorLength + conversion.vectorLengthOf(other) })
		
		override def *(mod: Double): WorldDistance =
			new LazyWorldDistance(d.mapCurrent { _ * mod }, v.mapCurrent { _ * mod })
	}
}

/**
  * Common trait for models that can represent distance both in
  * vector and "real world" form
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  */
trait WorldDistance extends EqualsBy with ApproxEquals[WorldDistance]
	with Combinable[Distance, WorldDistance] with LinearScalable[WorldDistance]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The "real world" representation of this distance
	  */
	def distance: Distance
	/**
	  * @return The vector representation of this distance
	  */
	def vectorLength: Double
	
	/**
	  * @param amount Amount of distance to add
	  * @return Copy of this distance with the specified amount added
	  */
	def +(amount: Double): WorldDistance
	
	
	// IMPLEMENTED  ------------------
	
	override def self: WorldDistance = this
	
	override protected def equalsProperties: Iterable[Any] = Vector(vectorLength)
	
	override def ~==(other: WorldDistance): Boolean = vectorLength ~== other.vectorLength
	
	
	// OTHER    ----------------------
	
	/**
	  * @param amount Amount of distance to subtract
	  * @return Copy of this distance with the specified amount subtracted
	  */
	def -(amount: Double) = this + (-amount)
	/**
	  * @param amount Amount of distance to subtract
	  * @return Copy of this distance with the specified amount subtracted
	  */
	def -(amount: Distance) = this + (-amount)
}
