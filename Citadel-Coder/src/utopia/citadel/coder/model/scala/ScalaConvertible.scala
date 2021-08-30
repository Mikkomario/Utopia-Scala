package utopia.citadel.coder.model.scala

/**
  * Common trait for items which may be converted to scala code
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait ScalaConvertible
{
	/**
	  * @return A scala string based on this item
	  */
	def toScala: String
}
