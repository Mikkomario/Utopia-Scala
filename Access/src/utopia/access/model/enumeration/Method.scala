package utopia.access.model.enumeration

import utopia.flow.collection.immutable.Single
import utopia.flow.operator.equality.{EqualsBy, EqualsFunction}
import utopia.flow.util.{OpenEnumeration, OpenEnumerationValue}

/**
  * Each http method represents a different function a client wants to perform on server side
  * @author Mikko Hilpinen
  * @since 24.8.2017
  * @see https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
  */
trait Method extends OpenEnumerationValue[String] with EqualsBy
{
	// ABSTRACT --------------------
	
	/**
	  * @return Name of this method. In upper-case letters.
	  */
	def name: String
	
	
	// IMPLEMENTED  ---------------
	
	override def toString = name
	
	override def identifier: String = name
	override protected def equalsProperties: Seq[Any] = Single(identifier)
}

object Method extends OpenEnumeration[Method, String](identifiersMatch = EqualsFunction.stringCaseInsensitive)
{
	// VALUES    -----------------
	
	/**
	  * The GET method is used for retrieving data from the server
	  */
	case object Get extends Method
	{
		override val name: String = "GET"
	}
	/**
	  * The POST method is used for storing / pushing new data to the server
	  */
	case object Post extends Method
	{
		override val name: String = "POST"
	}
	/**
	  * The PUT method is used for updating / overwriting existing data on the server
	  */
	case object Put extends Method
	{
		override val name: String = "PUT"
	}
	/**
	  * The DELETE method is used for deleting data on the server
	  */
	case object Delete extends Method
	{
		override val name: String = "DELETE"
	}
	/**
	  * PATCH method is used for updating parts of existing data on the server
	  */
	case object Patch extends Method
	{
		override val name: String = "PATCH"
	}
	
	
	// INITIAL CODE -------------
	
	introduce(Get, Post, Put, Delete, Patch)
	
	
	// OTHER METHODS    ---------
	
	/**
	  * @param method A string representing a method
	  * @return A method wrapping the specified string.
	  */
	def apply(method: String) = findFor(method).getOrElse(new PlaceholderMethod(method))
	
	/**
	  * Parses a string into a method, if it matches one (case-insensitive)
	  */
	@deprecated("Please use .findFor(String) or .apply(String) instead", "v1.6")
	def parse(methodString: String): Option[Method] = {
		val trimmedName = methodString.toUpperCase().trim()
		values.find { _.name == trimmedName }
	}
	
	
	// NESTED   ------------------
	
	private class PlaceholderMethod(override val name: String) extends Method
}