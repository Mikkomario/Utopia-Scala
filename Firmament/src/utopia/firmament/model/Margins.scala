package utopia.firmament.model

import utopia.firmament.model.stack.StackLength
import utopia.flow.operator.LinearScalable

/**
  * An object that provides access to simple length values
  * @author Mikko Hilpinen
  * @since 17.11.2019, Reflection v1
  * @param medium The standard margin
  */
case class Margins(medium: Double, diffMod: Double = 0.382) extends LinearScalable[Margins]
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * @return A smaller version of margin
	  */
	val small = (medium * diffMod).round.toDouble
	/**
	  * @return A very small version of margin
	  */
	val verySmall = (medium * math.pow(diffMod, 2)).round.toDouble
	/**
	  * @return A large version of margin
	  */
	val large = (medium / diffMod).round.toDouble
	
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
}
