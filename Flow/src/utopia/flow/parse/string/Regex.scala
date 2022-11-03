package utopia.flow.parse.string

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.caching.ResettableLazy

import java.util.regex.{Matcher, Pattern}
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
	  * Accepts alpha-numeric (ASCII) characters and underscores
	  */
	val wordCharacter = Regex("\\w")
	/**
	  * Contains alpha-numeric (ASCII) words with underscores
	  */
	val word = wordCharacter.oneOrMoreTimes
	val wordBoundary = Regex("\\b")
	val newLine = Regex("\\R")
	
	val lowerCaseLetter = Regex("[a-zåäö]")
	val upperCaseLetter = Regex("[A-ZÅÄÖ]")
	val alpha = Regex("[a-zA-ZåäöÅÄÖ]")
	val numericPositive = digit.oneOrMoreTimes
	val numeric = Regex("\\-").noneOrOnce + numericPositive
	val alphaNumeric = (alpha || digit).withinParenthesis
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
	  * @param chars A group fo characters that are not allowed
	  * @return A regular expression that accepts all except the specified characters
	  */
	def noneOf(chars: String) = !anyOf(chars)
	
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
	
	/**
	  * @return A copy of this regular expression that ignores results within quotations
	  */
	def ignoringQuotations = this + "(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)"
	/**
	  * @return A copy of this regular expression that ignores results within parentheses - Doesn't handle nested
	  *         parentheses properly, however
	  */
	def ignoringParentheses = ignoringWithin('(', ')')
	
	
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
		else if (hasBrackets && more.hasBrackets)
			(withoutBrackets + more.withoutBrackets).withBraces
		else
			Regex(string + "|" + more.string)
	}
	
	/**
	  * @param multiplier A multiplier
	  * @return This regular expression repeated that many times
	  */
	def *(multiplier: Int) = times(multiplier)
	/**
	  * @param range Range that determines how many times this regular expression may repeat
	  * @return This regular expression repeated 'a' to 'b' times, where 'a' and 'b' are range ends.
	  */
	def *(range: Range) = times(range)
	
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
	@throws[IllegalArgumentException]("If the specified range is empty")
	def times(range: Range): Regex = {
		if (range.nonEmpty) {
			if (range.size == 1)
				times(range.head)
			else
				Regex(string + s"{${range.min},${range.max}}")
		}
		else
			throw new IllegalArgumentException("Empty range")
	}
	
	/**
	  * Creates a regular expression that ignores results between the specified start and end characters
	  * @param start Starting character
	  * @param end Ending character
	  * @return A regular expression that ignores results between those characters
	  *         (doesn't work properly for nested structures)
	  */
	def ignoringWithin(start: Char, end: Char) =
	{
		val startRegex = Regex.escape(start)
		val endRegex = Regex.escape(end)
		val notStart = !startRegex
		val notEnd = !endRegex
		
		this + "(?=" + (notStart.zeroOrMoreTimes + startRegex + notEnd.zeroOrMoreTimes + endRegex).withinParenthesis
			.zeroOrMoreTimes + Regex.noneOf(start.toString + end).zeroOrMoreTimes + "$)"
	}
	
	/**
	  * @param str A string
	  * @return Whether the string matches this regex
	  */
	def apply(str: String) = str.matches(string)
	
	/**
	  * Replaces all occurrences of this regular expression with a string
	  * @param str String to modify
	  * @param replacement A string that this expression is replaced with (call-by-name)
	  * @return Modified string
	  */
	def replaceAll(str: String, replacement: => String): String = replaceAll(str, Lazy { replacement })
	/**
	  * Replaces all occurrences of this regular expression within a string
	  * @param str String to modify
	  * @param replacement A string to replace the possible occurrences of this regular expression (lazy)
	  * @return A modified string
	  */
	// Implementation is based on Matcher.replaceAll(String)
	// The main point of this function is to allow for the lazy initiation of the replacement
	def replaceAll(str: String, replacement: View[String]) = {
		val matcher = pattern.matcher(str)
		val findResultsIterator = Iterator.continually { matcher.find() }.takeWhile { r => r }
		if (findResultsIterator.hasNext) {
			val resultBuffer = new StringBuffer()
			findResultsIterator.foreach { _ => matcher.appendReplacement(resultBuffer, replacement.value) }
			matcher.appendTail(resultBuffer)
			resultBuffer.toString
		}
		else
			str
	}
	/**
	  * @param str A string
	  * @return A version of the string that only contains items NOT accepted by this regex
	  */
	def filterNot(str: String) = replaceAll(str, "")
	/**
	  * @param str A string
	  * @return A version of the string that only contains items accepted by this regex
	  */
	def filter(str: String) = matchesIteratorFrom(str).mkString
	
	/**
	 * Extracts the matches of this regex from the specified string, returning both the extracted results and the
	 * remaining string
	 * @param str A string from which to extract this regex's matches
	 * @return Remaining parts of the string that didn't match this regex
	 *         (each separated part is returned as a separate string) + matches of this regex that were found from
	 *         the string.
	 */
	def extract(str: String) = divide(str).divided
	
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
	def findFirstFrom(str: String) = matchesIteratorFrom(str).nextOption()
	/**
	  * Finds all matches of this regex from a string
	  * @param str A string
	  * @return All matches for this regex
	  */
	def findAllFrom(str: String) = matchesIteratorFrom(str).toVector
	
	/**
	  * @param str A string
	  * @return All character ranges within that string that match this regular expression
	  */
	def rangesFrom(str: String) = rangesIteratorIn(str).toVector
	/**
	  * @param str A string
	  * @return The first range matching this regular expression in that string, if found
	  */
	def firstRangeFrom(str: String) = rangesIteratorIn(str).nextOption()
	
	/**
	 * Splits the specified string using this regex
	 * @param str String to split
	 * @return Target string split by this regex
	 */
	def split(str: String): IndexedSeq[String] =
	{
		val ranges = rangesFrom(str)
		if (ranges.isEmpty)
			Vector(str)
		else
		{
			val firstPart = str.substring(0, ranges.head.start)
			val middleParts = ranges.paired.map { case Pair(prevRange, nextRange) =>
				str.substring(prevRange.exclusiveEnd, nextRange.start)
			}
			val endPart = str.substring(ranges.last.exclusiveEnd)
			firstPart +: middleParts :+ endPart
		}
	}
	
	/**
	  * Splits the specified string using this regular expression, lazily.
	  * @param str String to split
	  * @return A split result iterator based on the matches of this expression within that string.
	  *         NB: Doesn't contain any empty strings.
	  */
	def splitIteratorIn(str: String) =
	{
		// Finds pattern breaks (lazily), adds string start and end
		(breakIndexIteratorIn(str).pairedFrom(0) :+ str.length).zipWithIndex
			// Ignores matches and empty parts (because start and end were added)
			.filter { case (range, index) => index % 2 == 0 && range.first != range.second }
			// Converts ranges to substrings
			.map { case (Pair(first, end), _) => str.substring(first, end) }
	}
	
	/**
	  * Divides the specified string into matches and non-matches. Keeps the natural ordering.
	  * All of the specified string will be covered in the result.<br>
	  * For example, dividing "AxBxC" by "x" would yield [L("A"), R("x"), L("B"), R("x"), L("C")]
	  * @param str A string to divide
	  * @return Parts of that string as a Vector.
	  *         Each part is either: Left: A non-matched string part (outside match results) or Right: A match result
	  */
	def divide(str: String) = {
		val matcher = pattern.matcher(str)
		val builder = new VectorBuilder[Either[String, String]]()
		
		var lastEndIndex = 0
		while (matcher.find())
		{
			val startIndex = matcher.start()
			val endIndex = matcher.end()
			if (startIndex > lastEndIndex)
				builder += Left(str.substring(lastEndIndex, startIndex))
			builder += Right(str.substring(startIndex, endIndex))
			lastEndIndex = endIndex
		}
		if (str.length > lastEndIndex)
			builder += Left(str.substring(lastEndIndex))
		
		builder.result()
	}
	
	/**
	 * Splits the specified string using this regex. Works much like the standard split operation, except that
	 * this variation doesn't remove the splitting string from the results but instead keeps them at the ends of the
	 * resulting strings where applicable. E.g. splitting "AxBxC" by "x" would yield ["Ax", "Bx", "C"]
	 * @param str A string to split
	 * @return Divided parts of the string
	 */
	def separate(str: String) =
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
	
	/**
	  * @param str A string
	  * @return An iterator that returns pattern match results (substrings) from that string
	  */
	def matchesIteratorFrom(str: String): Iterator[String] = MatcherIterator.matches(pattern.matcher(str))
	/**
	  * @param str A string
	  * @return An iterator that returns pattern match ranges within that string
	  */
	def rangesIteratorIn(str: String): Iterator[Range] = MatcherIterator.ranges(pattern.matcher(str))
	/**
	  * @param str A string
	  * @return An iterator that returns match start indices (inclusive) within that string
	  */
	def startIndexIteratorIn(str: String): Iterator[Int] = MatcherIterator.startIndices(pattern.matcher(str))
	/**
	  * @param str A string
	  * @return An iterator that returns match end indices (exclusive) within that string
	  */
	def endIndexIteratorIn(str: String): Iterator[Int] = MatcherIterator.endIndices(pattern.matcher(str))
	/**
	  * @param str A string
	  * @return An iterator that returns match start indices (inclusive)
	  *         and match end indices (exclusive) within that string. I.e. all pattern ends (exclusive)
	  */
	def breakIndexIteratorIn(str: String): Iterator[Int] = MatcherIterator.breaks(pattern.matcher(str))
}

private object MatcherIterator
{
	def apply[A](matcher: Matcher)(f: Matcher => A) = new MatcherIterator[A](matcher)(f)
	
	def startIndices(matcher: Matcher) = apply(matcher) { _.start() }
	def endIndices(matcher: Matcher) = apply(matcher) { _.end() }
	def ranges(matcher: Matcher) = apply(matcher) { m => m.start() until m.end() }
	def matches(matcher: Matcher) = apply(matcher) { _.group() }
	def breaks(matcher: Matcher) = apply(matcher) { m => Pair(m.start(), m.end()) }.flatten
}

private class MatcherIterator[+A](matcher: Matcher)(f: Matcher => A) extends Iterator[A]
{
	// ATTRIBUTES   -------------------
	
	private val findCache = ResettableLazy { matcher.find() }
	
	
	// IMPLEMENTED  ------------------
	
	override def hasNext = findCache.value
	
	override def next() =
	{
		if (findCache.pop())
			f(matcher)
		else
			throw new NoSuchElementException("Calling .next() after the end of this iterator")
	}
}