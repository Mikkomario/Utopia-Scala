package utopia.reflection.localization

/**
  * This trait handles commonalities between different localization strings
  * @author Mikko Hilpinen
  * @since 22.4.2019, v1+
  */
trait LocalStringLike[Repr <: LocalStringLike[Repr]]
{
	// ABSTRACT	--------------
	
	/**
	  * @return This item
	  */
	def repr: Repr
	
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
	  * Adds another string to this string
	  * @param other Another string
	  * @return A combination of these two strings
	  */
	def +(other: Repr): Repr
	
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
	def isEmpty = string.isEmpty
	
	/**
	  * @return Whether this string contains characters
	  */
	def nonEmpty = string.nonEmpty
	
	/**
	  * @return None if this string is empty. This string otherwise.
	  */
	def notEmpty = if (isEmpty) None else Some(repr)
	
	/**
	  * @return This string split on newline characters
	  */
	def lines = split("\r?\n|\r")
	
	
	// IMPLEMENTED	----------
	
	override def toString = string
}
