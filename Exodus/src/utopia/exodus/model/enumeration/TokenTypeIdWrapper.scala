package utopia.exodus.model.enumeration

/**
  * A common trait for token type enumeration values
  * @author Mikko Hilpinen
  * @since 18.2.2022, v4.0
  */
trait TokenTypeIdWrapper
{
	/**
	  * @return The wrapped token type id
	  */
	def id: Int
}
