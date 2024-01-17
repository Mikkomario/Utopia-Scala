package utopia.firmament.model.enumeration

import utopia.firmament.model.enumeration.SizeCategory.Custom
import utopia.flow.operator.Steppable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.paradigm.transform.{Adjustment, LinearSizeAdjustable}

/**
  * An enumeration for different levels of sizes, such as large, small and medium
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.1
  */
trait SizeCategory extends LinearSizeAdjustable[SizeCategory] with Steppable[SizeCategory]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return A scaling modifier used for applying this size (compared to default / medium size).
	  *         Expressed as levels of impact (steps of change),
	  *         where 1 equals one adjustment towards larger, 2 equals two adjustments towards larger,
	  *         -1 equals one adjustment towards smaller and 0 equals no adjustment.
	  */
	def impact: Double
	
	
	// COMPUTED -----------------------
	
	/**
	  * @param adj Implicit adjustment scaling
	  * @return A scaling modifier used with this size category
	  */
	def scaling(implicit adj: Adjustment) = adj(impact)
	
	
	// IMPLEMENTED  -------------------
	
	override def self: SizeCategory = this
	
	override def *(mod: Double): SizeCategory = Custom(impact * mod)
	
	override def is(extreme: Extreme): Boolean = false
}

object SizeCategory
{
	/**
	  * A medium size category, matching scaling 1.0
	  */
	case object Medium extends SizeCategory
	{
		override val impact: Double = 0.0
		
		override def next(direction: Sign): SizeCategory = direction match {
			case Positive => Small
			case Negative => Large
		}
	}
	case object Small extends SizeCategory
	{
		override val impact: Double = -1.0
		
		override def next(direction: Sign): SizeCategory = direction match {
			case Positive => Medium
			case Negative => VerySmall
		}
	}
	case object VerySmall extends SizeCategory
	{
		override val impact: Double = -2.0
		
		override def next(direction: Sign): SizeCategory = direction match {
			case Positive => Small
			case Negative => Custom(-3.0)
		}
	}
	case object Large extends SizeCategory
	{
		override val impact: Double = 1.0
		
		override def next(direction: Sign): SizeCategory = direction match {
			case Positive => VeryLarge
			case Negative => Medium
		}
	}
	case object VeryLarge extends SizeCategory
	{
		override val impact: Double = 2.0
		
		override def next(direction: Sign): SizeCategory = direction match {
			case Positive => Custom(3.0)
			case Negative => Large
		}
	}
	
	/**
	  * A custom size category
	  * @param impact Custom impact to apply, where 0 is no impact, 1 is one step larger and -1 is one step smaller.
	  */
	case class Custom(impact: Double) extends SizeCategory
	{
		override def next(direction: Sign): SizeCategory = Custom(impact + direction.modifier)
	}
}
