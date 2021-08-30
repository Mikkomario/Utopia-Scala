package utopia.flow.parse

import java.util.regex.Pattern
import scala.collection.immutable.VectorBuilder
import scala.language.implicitConversions

object Regex
{
	implicit def stringToRegex(s: String): Regex = Regex(s)
	
	val anySingle = Regex(".")
	val any = anySingle.zeroOrMoreTimes
	val digit = Regex("\\d")
	val nonDigit = Regex("\\D")
	val whiteSpace = Regex("\\s")
	val nonWhiteSpace = Regex("\\S")
	/**
	  * Contains alpha-numeric (ASCII) words with underscores
	  */
	val word = Regex("\\w")
	val wordBoundary = Regex("\\b")
	val newLine = Regex("\\n")
	
	val lowerCaseLetter = Regex("[a-zåäö]")
	val upperCaseLetter = Regex("[A-ZÅÄÖ]")
	val alpha = Regex("[a-zA-ZåäöÅÄÖ]")
	val numericPositive = digit.oneOrMoreTimes
	val numeric = Regex("\\-").noneOrOnce + numericPositive
	val alphaNumeric = alpha || digit
	val decimalPositive = digit.oneOrMoreTimes + (Regex("[.,]") + digit.oneOrMoreTimes).withinParenthesis.noneOrOnce
	val decimal = Regex("\\-").noneOrOnce + decimalPositive
	
	val decimalParts = Regex("[-\\d,\\.]")
	val decimalPositiveParts = Regex("[\\d,\\.]")
	val numericParts = Regex("[-\\d]")
	
	/**
	 * A regular expression that finds parenthesis ( ) content
	 */
	val parenthesis = escape('(').followedBy(any).followedBy(escape(')'))
	
	/**
	  * Creates a regex that accepts any of the specified characters. You don't need to worry about regular expressions
	  * inside the string, since they are all escaped
	  * @param chars A group of escaped characters
	  * @return A new regex
	  */
	def anyOf(chars: String) = Regex(s"[\\Q$chars\\E]")
	
	/**
	  * @param char Character in regular expression
	  * @return A regular expression where that character has been "escaped" to make sure it is
	  *         treated as a character and not an expression
	  */
	def escape(char: Char) = Regex(s"\\$char")
}

/**
  * This is a simple wrapper for a regular expression with some utility methods
  * @author Mikko Hilpinen
  * @since 1.5.2019, v1+
  * @param string A regular expression
  */
case class Regex(string: String)
{
	// ATTRIBUTES	----------------
	
	/**
	 * A matching pattern based on this regular expression
	 */
	lazy val pattern = Pattern.compile(string)
	
	
	// COMPUTED	--------------------
	
	/**
	 * @return Inverted version of this regex
	 */
	def unary_! =
	{
		if (string.startsWith("(?!") && string.endsWith(")"))
			Regex(string.substring(3, string.length - 1))
		else if (string.startsWith("(?!") && string.endsWith("$).*"))
			Regex(string.substring(3, string.length - 4))
		else if (hasBrackets)
		{
			if (string.startsWith(Pattern.quote("[^")))
				Regex("[" + string.substring(2))
			else
				Regex("[^" + string.substring(1))
		}
		else if (string.endsWith("}") || string.endsWith("*") || string.endsWith("?") || string.endsWith("+"))
			Regex("(?!" + string + "$).*")
		else
			Regex("[^" + string + "]")
	}
	
	/**
	  * @return Whether this regex is empty
	  */
	def isEmpty = string.isEmpty
	/**
	  * @return Whether this regex is defined (non-empty)
	  */
	def isDefined = !isEmpty
	
	/**
	  * @return Whether this regex is inside braces [...]
	  */
	def hasBrackets =
	{
		if (isDefined && string.startsWith("[") && string.endsWith("]"))
		{
			// Makes sure the regex is not just a group of braced groups
			val inside = string.substring(1, string.length - 1)
			val firstStart = inside.indexOf('[')
			if (firstStart >= 0)
				firstStart < inside.indexOf(']')
			else
				true
		}
		else
			false
	}
	
	/**
	  * @return A copy of this regex without outside braces
	  */
	def withoutBrackets = if (hasBrackets) Regex(string.substring(1, string.length - 1)) else this
	/**
	  * @return A copy of this regex with outside braces
	  */
	def withBraces = if (hasBrackets) this else Regex(s"[$string]")
	/**
	  * @return A version of this regex wrapped within parenthesis
	  */
	def withinParenthesis = Regex("(" + string + ")")
	
	/**
	  * @return This regex in sequence 0 or more times
	  */
	def zeroOrMoreTimes = if (isEmpty || string.endsWith("*")) this else Regex(string + "*")
	/**
	  * @return This regex in sequence one or more times
	  */
	def oneOrMoreTimes = if (isEmpty || string.endsWith("+")) this else Regex(string + "+")
	/**
	  * @return This regex either 0 or exactly 1 times
	  */
	def noneOrOnce = if (isEmpty || string.endsWith("?")) this else Regex(string + "?")
	
	
	// IMPLEMENTED	----------------
	
	override def toString = string
	
	
	// OTHER	----------------
	
	/**
	  * @param another Another regex
	  * @return This regex followed by another regex
	  */
	def +(another: Regex) = Regex(string + another.string)
	
	/**
	  * @param more Another regex
	  * @return A regex that accepts this regex or the other regex
	  */
	def ||(more: Regex) =
	{
		if (more.isEmpty)
			this
		else if (isEmpty)
			more
		else if (hasBrackets || more.hasBrackets)
			(withoutBrackets + more.withoutBrackets).withBraces
		else
			Regex(string + "|" + more.string)
	}
	
	/**
	 * @param another Another regex
	 * @return This regex followed by another regex
	 */
	def followedBy(another: Regex) = this + another
	
	/**
	 * @param n A number
	 * @return This regex 'n' times in sequence
	 */
	def times(n: Int) = Regex(string + s"{$n}")
	/**
	 * @param range A range
	 * @return This regex 'range' times in sequence
	 */
	def times(range: Range) = Regex(string + s"{${range.start},${range.end}")
	
	/**
	  * @param str A string
	  * @return Whether the string matches this regex
	  */
	def apply(str: String) = str.matches(string)
	
	/**
	  * @param str A string
	  * @return A version of the string that only contains items NOT accepted by this regex
	  */
	def filterNot(str: String) = str.replaceAll(string, "")
	/**
	  * @param str A string
	  * @return A version of the string that only contains items accepted by this regex
	  */
	def filter(str: String) = findAllFrom(str).reduceOption { _ + _ } getOrElse ""
	
	/**
	 * Extracts the matches of this regex from the specified string, returning both the extracted results and the
	 * remaining string
	 * @param str A string from which to extract this regex's matches
	 * @return Remaining parts of the string that didn't match this regex
	 *         (each separated part is returned as a separate string) + matches of this regex that were found from
	 *         the string.
	 */
	def extract(str: String) =
	{
		val matcher = pattern.matcher(str)
		val matchesBuilder = new VectorBuilder[String]()
		val remainingBuilder = new VectorBuilder[String]()
		
		var lastEndIndex = 0
		while (matcher.find())
		{
			val startIndex = matcher.start()
			val endIndex = matcher.end()
			if (startIndex > lastEndIndex)
				remainingBuilder += str.substring(lastEndIndex, startIndex)
			matchesBuilder += str.substring(startIndex, endIndex)
			lastEndIndex = endIndex
		}
		if (str.length > lastEndIndex)
			remainingBuilder += str.substring(lastEndIndex)
		
		remainingBuilder.result() -> matchesBuilder.result()
	}
	
	/**
	 * @param str A target string
	 * @return Whether this regex / pattern can be found from that string
	 */
	def existsIn(str: String) = pattern.matcher(str).find()
	
	/**
	  * Finds the first match for this regex from the specified string
	  * @param str A string
	  * @return The first match from the string
	  */
	def findFirstFrom(str: String) =
	{
		val matcher = pattern.matcher(str)
		if (matcher.find())
			Some(matcher.group())
		else
			None
	}
	/**
	  * Finds all matches of this regex from a string
	  * @param str A string
	  * @return All matches for this regex
	  */
	def findAllFrom(str: String) =
	{
		val matcher = pattern.matcher(str)
		val builder = new VectorBuilder[String]()
		while (matcher.find())
		{
			builder += matcher.group()
		}
		builder.result()
	}
	
	/**
	  * @param str A string
	  * @return All character ranges within that string that match this regular expression
	  */
	def rangesFrom(str: String) =
	{
		val matcher = pattern.matcher(str)
		val builder = new VectorBuilder[Range.Inclusive]()
		while (matcher.find())
		{
			builder += matcher.start() to matcher.end()
		}
		builder.result()
	}
	
	/**
	 * Splits the specified string using this regex
	 * @param str String to split
	 * @return Target string splitted by this regex
	 */
	def split(str: String) = str.split(string)
	/**
	 * Splits the specified string using this regex. Works much like the standard split operation, except that
	 * this variation doesn't remove the splitting string from the results but instead keeps them at the ends of the
	 * resulting strings where applicable. E.g. splitting "AxBxC" by "x" would yield ["Ax", "Bx", "C"]
	 * @param str A string to split
	 * @return Divided parts of the string
	 */
	def divide(str: String) =
	{
		val matcher = pattern.matcher(str)
		val builder = new VectorBuilder[String]()
		var lastEndIndex = 0
		while (matcher.find())
		{
			val endIndex = matcher.end()
			builder += str.substring(lastEndIndex, endIndex)
			lastEndIndex = endIndex
		}
		if (str.length > lastEndIndex)
			builder.result() :+ str.substring(lastEndIndex)
		else
			builder.result()
	}
}
