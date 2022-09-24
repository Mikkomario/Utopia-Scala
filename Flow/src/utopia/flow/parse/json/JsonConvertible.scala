package utopia.flow.parse.json

/**
  * Json Convertible instances can be written as json data
  * @author Mikko Hilpinen
  * @since 23.6.2017
  */
trait JsonConvertible
{
	// ABSTRACT    -----------------------
	
	/**
	  * Appends the json representation of this item to the specified string builder
	  * @param jsonBuilder A string builder that will contain the resulting json string
	  */
	def appendToJson(jsonBuilder: StringBuilder): Unit
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return A json representation fo this instance
	  */
	def toJson: String = {
		val builder = new StringBuilder()
		appendToJson(builder)
		builder.result()
	}
	
	/**
	  * A JSON Representation of this instance
	  */
	@deprecated("Replaced with toJson", "v1.8")
	def toJSON = toJson
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = toJson
}
