package utopia.paradigm.measurement

import utopia.flow.operator.HasExtremes
import utopia.flow.operator.enumeration.Extreme

/**
  * Used for constructing / selecting metric units
  * @author Mikko Hilpinen
  * @since 8.12.2023, v1.5
  * @tparam U Type of units generated
  */
trait MetricUnitFactory[+U] extends HasExtremes[U]
{
	// ABSTRACT --------------------
	
	/**
	  * @param scale Targeted scale
	  * @return A unit that matches that scale
	  */
	def apply(scale: MetricScale): U
	
	
	// IMPLEMENTED  ----------------
	
	override def apply(extreme: Extreme): U = apply(MetricScale(extreme))
	
	
	// OTHER    -------------------
	
	/**
	  * @param value A value
	  * @return The unit that is readable when used with that value
	  */
	def appropriateFor(value: Double) = apply(MetricScale.appropriateFor(value))
}
