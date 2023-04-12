package utopia.firmament.model

import utopia.flow.operator.LinearScalable

/**
  * An object that provides access to simple length values
  * @author Mikko Hilpinen
  * @since 17.11.2019, Reflection v1
  * @param medium The standard margin
  */
case class Margins(medium: Double) extends LinearScalable[Margins]
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * @return A smaller version of margin
	  */
	val small = (medium * 0.382).round.toDouble
	/**
	  * @return A very small version of margin
	  */
	val verySmall = (medium * 0.382 * 0.382).round.toDouble
	/**
	  * @return A large version of margin
	  */
	val large = (medium / 0.382).round.toDouble
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self: Margins = this
	
	override def *(mod: Double): Margins = copy((medium * mod).round.toDouble)
}
