package utopia.firmament.model

import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Large, Small, VerySmall}
import utopia.firmament.model.stack.StackLength
import utopia.flow.operator.LinearScalable
import utopia.paradigm.transform.Adjustment

object Margins
{
	/**
	  * Creates a new set of margins using implicitly available adjustments
	  * @param medium The medium margin length
	  * @param adj Implicit adjustments to use
	  * @return A new set of margins
	  */
	def implicitly(medium: Double)(implicit adj: Adjustment): Margins = apply(medium, adj)
}

/**
  * An object that provides access to simple length values
  * @author Mikko Hilpinen
  * @since 17.11.2019, Reflection v1
  * @param medium The standard margin
  * @param adjustment A modifier that represents the difference between two states,
  *                   such as the medium and the large margin.
  *                   Default = Decrease by 38.2%, increase by 61.8%
  */
case class Margins(medium: Double, adjustment: Adjustment = Adjustment(0.382)) extends LinearScalable[Margins]
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * @return A smaller version of margin
	  */
	val small = apply(Small)
	/**
	  * @return A very small version of margin
	  */
	val verySmall = apply(VerySmall)
	/**
	  * @return A large version of margin
	  */
	val large = apply(Large)
	
	lazy val smallOrSmaller = StackLength(0, small, small)
	lazy val verySmallOrNone = StackLength(0, verySmall, verySmall)
	lazy val mediumOrLarger = StackLength(medium, medium)
	lazy val largeOrLarger = StackLength(large, large)
	
	/**
	  * Small to medium (preferred) to large
	  */
	lazy val aroundMedium = StackLength(small, medium, large)
	/**
	  * Very small to small (preferred) to medium
	  */
	lazy val aroundSmall = StackLength(verySmall, small, medium)
	/**
	  * Zero to very small (preferred) to small
	  */
	lazy val aroundVerySmall = StackLength(0, verySmall, small)
	
	/**
	  * Anything between very small and large
	  */
	lazy val any = StackLength(verySmall, medium, large)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self: Margins = this
	
	override def *(mod: Double): Margins = copy(medium = (medium * mod).round.toDouble)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param size Targeted margin size level
	  * @return Margin of that level
	  */
	def apply(size: SizeCategory) = (medium * adjustment(size.impact)).round.toDouble
	/**
	  * @param optimal The optimal margin level
	  * @return Margin of that level, or smaller
	  */
	def smallerThan(optimal: SizeCategory) = {
		val opt = apply(optimal)
		StackLength(0, opt, opt)
	}
	/**
	  * @param optimal The optimal margin level
	  * @return Margin of that level, or larger
	  */
	def largerThan(optimal: SizeCategory) = {
		val opt = apply(optimal)
		StackLength(opt, opt)
	}
	
	/**
	  * @param optimal The optimal margin length
	  * @param variance Amount of variance allowed in levels of impact, where 0 is no variance,
	  *                 1 is one step of variance (e.g. from medium to large)
	  *                 and 2 is two steps of variance (e.g. from very small to medium)
	  * @return A stack length that prefers the specified margin size but allows for some variance
	  */
	def around(optimal: SizeCategory, variance: Double = 1.0) = {
		val opt = apply(optimal)
		StackLength(opt * adjustment(-variance), opt, opt * adjustment(variance))
	}
}
