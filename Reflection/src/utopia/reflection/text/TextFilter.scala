package utopia.reflection.text

import scala.language.implicitConversions

object TextFilter
{
	/**
	  * A filter that makes all text uppercase
	  */
	val upperCase = TextFilter(None, isOnlyUpperCase = true)
	
	/**
	  * @param regex A regex
	  * @return A filter based on regex
	  */
	def apply(regex: Regex): TextFilter = TextFilter(Some(regex), isOnlyUpperCase = false)
	
	/**
	  * @param regex A regex
	  * @return A filter based on regex that makes items uppercase
	  */
	def upperCase(regex: Regex) = TextFilter(Some(regex), isOnlyUpperCase = true)
	
	implicit def regexToFilter(regex: Regex): TextFilter = apply(regex)
}

/**
  * These filters alter text characters to fit it into some format
  * @author Mikko Hilpinen
  * @since 1.5.2019, v1+
  */
case class TextFilter(regex: Option[Regex], isOnlyUpperCase: Boolean)
{
	// OPERATORS	--------------------
	
	// Null checks because you never know about swing components
	/**
	  * Checks whether this filter accepts the provided character
	  * @param character A character
	  * @return Whether this filter accepts the provided character
	  */
	def apply(character: String) = if (character == null) false else regex.forall { r => character.matches(r.string) }
	
	/**
	  * Formats a string that it only includes accepted values
	  * @param s A source string
	  * @return Result string
	  */
	def format(s: String) =
	{
		if (s == null || s.isEmpty)
			""
		else
		{
			// Only keeps the sequences included in the regex
			val remaining = regex.map { _.filter(s) } getOrElse s
			
			// May convert all chars to upper case
			if (isOnlyUpperCase) remaining.toUpperCase else remaining
		}
	}
}
