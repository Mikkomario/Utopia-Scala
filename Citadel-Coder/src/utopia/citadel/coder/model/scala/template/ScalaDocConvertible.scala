package utopia.citadel.coder.model.scala.template

import utopia.citadel.coder.model.scala.ScalaDocPart

/**
  * Common trait for instances that can be converted to scaladoc lines
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
trait ScalaDocConvertible
{
	/**
	  * @return A scaladoc part based on this item
	  */
	def toScalaDocPart: ScalaDocPart
}
