package utopia.citadel.coder.model.scala

object CodeConvertible
{
	/**
	  * Recommended maximum line length
	  */
	val maxLineLength = 100
}

/**
  * Common trait for items which can be converted to 1 or more full lines of code
  * @author Mikko Hilpinen
  * @since 31.8.2021, v0.1
  */
trait CodeConvertible
{
	/**
	  * @return Code lines based on this item. Expects the topmost line not to be intended but the other
	  *         lines to be intended correctly relative to the topmost line.
	  */
	def toCodeLines: Vector[String]
}
