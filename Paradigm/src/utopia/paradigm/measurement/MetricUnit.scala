package utopia.paradigm.measurement

import utopia.flow.operator.Steppable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.sign.Sign

/**
  * Common trait for metric units (i.e. units that follow the metric scale)
  * @author Mikko Hilpinen
  * @since 8.12.2023, v1.5
  */
trait MetricUnit[+Repr <: Steppable[Repr]] extends Steppable[Repr]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return Scale used in this unit
	  */
	def scale: MetricScale
	
	/**
	  * @return A factory used for constructing these units
	  */
	protected def factory: MetricUnitFactory[Repr]
	
	
	// IMPLEMENTED  ------------------
	
	override def next(direction: Sign): Repr = factory(scale.next(direction))
	override def is(extreme: Extreme): Boolean = scale.is(extreme)
}
