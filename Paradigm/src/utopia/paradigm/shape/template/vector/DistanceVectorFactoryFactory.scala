package utopia.paradigm.shape.template.vector

import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.paradigm.shape.template.{DimensionalBuilder, DimensionalFactory, Dimensions, FromDimensionsFactory, HasDimensions}

import scala.language.implicitConversions

object DistanceVectorFactoryFactory
{
	// IMPLICIT ----------------------------
	
	implicit def implicitUnit[V](ff: DistanceVectorFactoryFactory[V])
	                            (implicit unit: DistanceUnit): DistanceVectorFactory[V] =
		ff(unit)
}

/**
  * An abstract implementation of a distance vector factory
  * where the default measurement unit has not yet been specified.
  * @author Mikko Hilpinen
  * @since 01.01.2025, v1.7.1
  */
abstract class DistanceVectorFactoryFactory[+V]
	extends FromDimensionsFactory[Distance, V] with DimensionalFactory[Distance, V]
{
	// ATTRIBUTES   -------------------------
	
	private val factories = Cache[DistanceUnit, DistanceVectorFactory[V]] { FactoryWithUnit(_) }
	
	
	// ABSTRACT -----------------------------
	
	/**
	  * @param dimensions Dimensions to wrap
	  * @param defaultUnit Default unit of measurement used in double number interactions
	  * @return A new vector instance, wrapping the specified dimensions
	  */
	def apply(dimensions: Dimensions[Distance], defaultUnit: DistanceUnit): V
	/**
	  * @param other Another item with real length dimensions
	  * @param defaultUnit Default unit of measurement used in double number interactions
	  * @return Specified instance as a correctly formed vector instance
	  */
	def from(other: HasDimensions[Distance], defaultUnit: DistanceUnit): V
	
	
	// IMPLEMENTED  -------------------------
	
	override def newBuilder: DimensionalBuilder[Distance, V] = Dimensions.distance.newBuilder.mapResult(apply)
	
	override def apply(dimensions: Dimensions[Distance]): V = apply(dimensions, dimensions.x.unit)
	override def apply(values: IndexedSeq[Distance]): V = apply(Dimensions.distance(values))
	override def apply(values: Map[Axis, Distance]): V = {
		if (values.isEmpty)
			empty
		else
			apply(Dimensions.distance(values), values.valuesIterator.next().unit)
	}
	
	override def from(other: HasDimensions[Distance]): V = from(other, other.dimensions.x.unit)
	override def from(values: IterableOnce[Distance]): V = apply(Dimensions.distance.from(values))
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param unit Default distance unit to apply in ambiguous cases
	  * @return A factory for constructing vectors
	  */
	def apply(unit: DistanceUnit) = factories(unit)
	
	
	// NESTED   -----------------------------
	
	private case class FactoryWithUnit(unit: DistanceUnit) extends DistanceVectorFactory[V](unit)
	{
		override def apply(dimensions: Dimensions[Distance]): V = DistanceVectorFactoryFactory.this(dimensions, unit)
		override def from(other: HasDimensions[Distance]): V = DistanceVectorFactoryFactory.this.from(other, unit)
	}
}
