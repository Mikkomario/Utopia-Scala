package utopia.firmament.localization

import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.combine.Combinable
import utopia.flow.util.StringExtensions._

/**
  * This trait handles commonalities between different localization strings
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
trait LocalStringLike[Repr] extends Combinable[Repr, Repr] with MaybeEmpty[Repr]
{
	// ABSTRACT	--------------
	
	/**
	  * @return A string representation of this string-like instance
	  */
	def string: String
	
	/**
	  * @return The ISO code of this string representation's language. None if this string isn't language specific
	  *         (like a number or a whitespace, etc.)
	  */
	def languageCode: Option[String]
	
	/**
	  * @param f A string modification function
	  * @return A modified copy of this string
	  */
	def modify(f: String => String): Repr
	
	/**
	  * Splits this string based on provided regex
	  * @param regex The part which splits this string
	  * @return Split parts
	  */
	def split(regex: String): Vector[Repr]
	
	/**
	  * Creates an interpolated version of this string where segments marked with %s, %S, %i or %d are replaced with
	  * provided arguments parsed into correct format. %s means raw string format. %S means uppercase string format.
	  * %i means integer format. %d means decimal format.
	  * @param args The parsed arguments
	  * @return A version of this string with parameter segments replaced with provided values
	  */
	def interpolated(args: Seq[Any]): Repr
	
	/**
	  * Creates an interpolated version of this string where each ${key} is replaced with a matching value from
	  * the specified map
	  * @param args Interpolation arguments (key value pairs)
	  * @return An interpolated version of this string
	  */
	def interpolated(args: Map[String, Any]): Repr
	
	
	// COMPUTED	--------------
	
	/**
	  * @return Whether this string is empty
	  */
	override def isEmpty = string.isEmpty
	
	/**
	  * @return This string split on newline characters
	  */
	def lines = split("\r?\n|\r")
	
	/**
	  * @return A copy of this string without any control characters in it
	  */
	def stripControlCharacters = modify { _.stripControlCharacters }
	
	
	// IMPLEMENTED	----------
	
	override def toString = string
}
