package utopia.terra.model.world

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.flow.operator._
import utopia.flow.operator.combine.{Combinable, LinearScalable}
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.sign.{Sign, SignOrZero, SignedOrZero}
import utopia.flow.view.immutable.caching.Lazy
import utopia.paradigm.measurement.Distance
import utopia.terra.controller.coordinate.world.VectorDistanceConversion

import scala.language.implicitConversions

object WorldDistance
{
	// COMPUTED --------------------
	
	/**
	  * @param conversion Implicit conversion to use in distance-altering functions
	  * @return A zero length distance
	  */
	def zero(implicit conversion: VectorDistanceConversion): WorldDistance = new ZeroWorldDistance
	
	
	// IMPLICIT --------------------
	
	/**
	  * @param distance   The amount of "real world" distance travelled
	  * @param conversion Implicit distance conversion to use
	  * @return A new distance instance
	  */
	implicit def apply(distance: Distance)(implicit conversion: VectorDistanceConversion): WorldDistance =
		new LazyWorldDistance(Lazy.initialized(distance), Lazy { conversion.vectorLengthOf(distance) })
	/**
	  * @param vectorDistance The amount of vector distance travelled
	  * @param conversion     Implicit distance conversion to use
	  * @return A new distance instance
	  */
	implicit def vector(vectorDistance: Double)(implicit conversion: VectorDistanceConversion): WorldDistance =
		new LazyWorldDistance(Lazy { conversion.distanceOf(vectorDistance) }, Lazy.initialized(vectorDistance))
	
	/**
	  * @param wd A world distance instance
	  * @return The distance represented by the specified world distance
	  */
	implicit def autoUnwrapDistance(wd: WorldDistance): Distance = wd.distance
	
	
	// OTHER    --------------------
	
	/**
	  * @param distance Distance travelled in the "real world"
	  * @param vectorLength Distance travelled in vector units
	  * @param conversion Implicit distance conversion to use
	  * @return A new distance instance
	  */
	def apply(distance: Distance, vectorLength: Double)(implicit conversion: VectorDistanceConversion): WorldDistance =
		new _WorldDistance(distance, vectorLength)
	
	
	// NESTED   --------------------
	
	private class _WorldDistance(override val distance: Distance, override val vectorLength: Double)
	                            (implicit override val conversion: VectorDistanceConversion)
		extends WorldDistance
	{
		override def sign: SignOrZero = Sign.of(vectorLength)
		override def isAboutZero: Boolean = vectorLength ~== 0.0
		
		override def +(amount: Double): WorldDistance =
			new LazyWorldDistance(Lazy { distance + conversion.distanceOf(amount) },
				Lazy.initialized(vectorLength + amount))
		override def +(other: Distance): WorldDistance = new LazyWorldDistance(Lazy.initialized(distance + other),
			Lazy { vectorLength + conversion.vectorLengthOf(other) })
		override def +(other: WorldDistance): WorldDistance =
			new LazyWorldDistance(Lazy { distance + other.distance }, Lazy { vectorLength + other.vectorLength })
		
		override def *(mod: Double): WorldDistance = new _WorldDistance(distance * mod, vectorLength * mod)
	}
	
	private class LazyWorldDistance(d: Lazy[Distance], v: Lazy[Double])
	                               (implicit override val conversion: VectorDistanceConversion)
		extends WorldDistance
	{
		// ATTRIBUTES   ----------------------
		
		override lazy val sign: SignOrZero = withEither { Sign.of(_) } { _.sign }
		
		
		// IMPLEMENTED  ----------------------
		
		override def distance: Distance = d.value
		override def vectorLength: Double = v.value
		
		override def isAboutZero: Boolean = withEither { _ ~== 0.0 } { _.isAboutZero }
		
		override def +(amount: Double): WorldDistance =
			new LazyWorldDistance(Lazy { distance + conversion.distanceOf(amount) }, v.mapCurrent { _ + amount })
		override def +(other: Distance): WorldDistance =
			new LazyWorldDistance(d.mapCurrent { _ + other }, Lazy { vectorLength + conversion.vectorLengthOf(other) })
		override def +(other: WorldDistance): WorldDistance =
			new LazyWorldDistance(Lazy { distance + other.distance }, Lazy { vectorLength + other.vectorLength })
		
		override def *(mod: Double): WorldDistance =
			new LazyWorldDistance(d.mapCurrent { _ * mod }, v.mapCurrent { _ * mod })
			
		
		// OTHER    -------------------------
		
		private def withEither[A](vf: Double => A)(vd: Distance => A) = v.current match {
			case Some(length) => vf(length)
			case None =>
				d.current match {
					case Some(distance) => vd(distance)
					case None => vf(vectorLength)
				}
		}
	}
	
	private class ZeroWorldDistance(implicit override val conversion: VectorDistanceConversion) extends WorldDistance
	{
		override def distance: Distance = Distance.zero
		override def vectorLength: Double = 0.0
		
		override def sign: SignOrZero = Neutral
		override def isAboutZero: Boolean = true
		
		override def +(other: Distance): WorldDistance = WorldDistance(other)
		override def +(amount: Double): WorldDistance = WorldDistance.vector(amount)
		override def +(other: WorldDistance): WorldDistance = other
		
		override def *(mod: Double): WorldDistance = this
	}
}

/**
  * Common trait for models that can represent distance both in
  * vector and "real world" form
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  */
trait WorldDistance
	extends EqualsBy with MayBeAboutZero[WorldDistance, WorldDistance] with Combinable[Distance, WorldDistance]
		with LinearScalable[WorldDistance] with SignedOrZero[WorldDistance]
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
	  * @return Conversion algorithm used for converting between vector- and real world distances
	  */
	protected implicit def conversion: VectorDistanceConversion
	
	/**
	  * @param amount Amount of distance to add
	  * @return Copy of this distance with the specified amount added
	  */
	def +(amount: Double): WorldDistance
	/**
	  * @param other Another distance
	  * @return Combination of these two distances
	  */
	def +(other: WorldDistance): WorldDistance
	
	
	// IMPLEMENTED  ------------------
	
	override def self: WorldDistance = this
	override def zero: WorldDistance = WorldDistance.zero
	
	override protected def equalsProperties: Seq[Any] = Vector(vectorLength)
	
	override def toString = distance.toString
	
	override def ~==(other: WorldDistance): Boolean = doubleEquals(vectorLength, other.vectorLength)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param vectorDistance A vector distance
	  * @return Whether this distance is somewhat equal to the specified vector distance
	  */
	def ~==(vectorDistance: Double): Boolean = doubleEquals(vectorLength, vectorDistance)
	/**
	  * @param distance A distance
	  * @return Whether this distance is somewhat equal to the specified distance
	  */
	def ~==(distance: Distance): Boolean = this.distance ~== distance
	
	/**
	  * @param amount Amount of distance to subtract
	  * @return Copy of this distance with the specified amount subtracted
	  */
	def -(amount: Double): WorldDistance = this + (-amount)
	/**
	  * @param other Another world distance
	  * @return This distance subtracted by the specified distance
	  */
	def -(other: WorldDistance): WorldDistance = this + (-other)
}
