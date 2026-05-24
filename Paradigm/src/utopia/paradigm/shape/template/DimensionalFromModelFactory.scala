package utopia.paradigm.shape.template

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.template.DimensionalFromModelFactory.MappedFactory

import scala.util.Try

object DimensionalFromModelFactory
{
	// NESTED   --------------------------
	
	private class MappedFactory[D, M, R](delegate: DimensionalFromModelFactory[D, M], f: M => R)
		extends DimensionalFromModelFactory[D, R]
	{
		override def newBuilder: DimensionalBuilder[D, R] = delegate.newBuilder.mapResult(f)
		
		override def apply(values: IndexedSeq[D]): R = f(delegate(values))
		override def apply(values: Map[Axis, D]): R = f(delegate(values))
		
		override def from(values: IterableOnce[D]): R = f(delegate.from(values))
		
		override def apply(model: HasProperties): Try[R] = delegate(model).map(f)
	}
}

/**
 * A trait that combines both [[DimensionalFactory]] and [[FromModelFactory]].
 * @author Mikko Hilpinen
 * @since 24.05.2026, v1.8.2
 */
trait DimensionalFromModelFactory[-D, +R] extends DimensionalFactory[D, R] with FromModelFactory[R]
{
	override def mapResult[B](f: R => B): DimensionalFromModelFactory[D, B] = new MappedFactory[D, R, B](this, f)
}
