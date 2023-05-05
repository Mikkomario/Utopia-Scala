package utopia.paradigm.transform

import utopia.flow.operator.{Scalable, Sign}

/**
  * Used for representing the impact of an individual individual adjustment,
  * where 0 is no adjustment and 1 is 100% adjustment.
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.1
  * @param decrease The amount of decrease applied upon a single step / level of impact.
  *                 E.g. if 0.2, the affected items will be 20% per each level of impact.
  */
case class Adjustment(decrease: Double) extends Scalable[Double, Double]
{
	// ATTRIBUTES   --------------------------
	
	/**
	  * The amount of increase applied upon a single step / level of impact.
	  */
	val increase = (1.0 / (1.0 - decrease)) - 1.0
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param impactLevel The level of impact to apply, where 1 equals one step towards larger
	  *                    and -1 equals one step towards smaller.
	  * @return A scaling modifier that yields the specified impact
	  */
	def apply(impactLevel: Double) = {
		// Case: No impact => Identity
		if (impactLevel == 0)
			1.0
		// Case: Positive impact => Scales
		else if (impactLevel > 0)
			math.pow(1.0 + increase, impactLevel)
		// Case: Negative impact => Shrinks
		else
			math.pow(1.0 - decrease, -impactLevel)
	}
	/**
	  * @param direction The direction of impact, where positive makes larger and negative shrinks
	  * @param impact The level of impact applied, where 1 equals one step if change and 0 equals no change
	  * @return A scaling modifier that yields the specified impact
	  */
	def apply(direction: Sign, impact: Double): Double = apply(impact * direction.modifier)
	
	
	// IMPLEMENTED  --------------------
	
	override def *(mod: Double): Double = apply(mod)
}
