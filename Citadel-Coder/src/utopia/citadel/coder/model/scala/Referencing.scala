package utopia.citadel.coder.model.scala

/**
  * Common trait for codes / pieces of code that (may) refer to external classes
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait Referencing
{
	/**
	  * @return References made by this instance
	  */
	def references: Set[Reference]
}
