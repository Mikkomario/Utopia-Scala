package utopia.flow.parse.json

import scala.collection.mutable

object JsonConvertible
{
	/**
	 * Maximum number of recursive appendToJson(...) calls that should be allowed before throwing a
	 * CyclicalIterationException.
	 */
	val maxRecursiveCalls = 48
}

/**
  * Json Convertible instances can be written as json data
  * @author Mikko Hilpinen
  * @since 23.6.2017
  */
trait JsonConvertible
{
	// ABSTRACT    -----------------------
	
	/**
	  * Appends the JSON representation of this item to the specified string builder
	  * @param jsonBuilder A string builder that will receive the resulting JSON contents
	 * @param depth A depth value that increases with every recursive .appendToJson call.
	 *              If this reaches [[JsonConvertible.maxRecursiveCalls]],
	 *              a CyclicalIterationException must be thrown.
	  */
	def appendToJson(jsonBuilder: mutable.Growable[Char], depth: Int): Unit
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return A json representation fo this instance
	  */
	def toJson: String = {
		val builder = new StringBuilder()
		appendToJson(builder, 0)
		builder.result()
	}
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = toJson
}
