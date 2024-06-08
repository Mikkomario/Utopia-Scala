package utopia.flow.parse.string

import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.MaybeEmpty
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
	val any = anySingle.anyTimes
	
	/**
	 * A regular expression that matches the start of a line / input
	 */
	val startOfLine = Regex("^")
	/**
	 * A regular expression that matches the start of the input string
	 */
	val startOfString = Regex("\\A")
	/**
	 * A regular expression that matches the end of input string
	 */
	val endOfString = Regex("\\Z")
	
	/**
	  * Accepts individual digits [0-9]
	  */
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
	/**
	 * Accepts lower- and upper-case letters
	 */
	val letter = Regex("[a-zA-ZåäöÅÄÖ]")
	/**
	  * Accepts lower- and upper-case letters
	  */
	@deprecated("Renamed to .letter", "v2.2")
	def alpha = letter
	/**
	 * Accepts any positive integer
	 */
	val positiveInteger = digit.oneOrMoreTimes
	/**
	  * Accepts any positive integer
	  */
	@deprecated("Renamed to .positiveInteger", "v2.2")
	def numericPositive = positiveInteger
	/**
	 * Accepts any integer (positive or negative)
	 */
	val integer = Regex("\\-").noneOrOnce + positiveInteger
	/**
	  * Accepts any integer (positive or negative)
	  */
	@deprecated("Renamed to .integer", "v2.2")
	def numeric = integer
	/**
	 * Accepts digits and characters
	 */
	val letterOrDigit = (letter || digit).withinParenthesis
	/**
	  * Accepts digits and characters
	  */
	@deprecated("Renamed to .letterOrDigit", "v2.2")
	def alphaNumeric = letterOrDigit
	/**
	 * Accepts positive integers and decimal numbers
	 */
	val positiveNumber = digit.oneOrMoreTimes + (Regex("[.,]") + digit.oneOrMoreTimes).withinParenthesis.noneOrOnce
	/**
	  * Accepts positive integers and decimal numbers
	  */
	@deprecated("Renamed to .positiveNumber", "v2.2")
	def decimalPositive = positiveNumber
	/**
	 * Accepts any integer or decimal number
	 */
	val number = Regex("\\-").noneOrOnce + positiveNumber
	/**
	  * Accepts any integer or decimal number
	  */
	@deprecated("Renamed to .number", "v2.2")
	def decimal = number
	
	/**
	 * Accepts any character that appears in a (decimal) number
	 */
	val numberPart = Regex("[-\\d,\\.]")
	/**
	  * Accepts any character that appears in a decimal number
	  */
	@deprecated("Renamed to .numberPart", "v2.2")
	def decimalParts = numberPart
	/**
	 * Accepts any character that accepts in a positive (decimal) number
	 */
	val positiveNumberPart = Regex("[\\d,\\.]")
	/**
	  * Accepts any character that accepts in a positive decimal number
	  */
	@deprecated("Renamed to positiveNumberPart", "v2.2")
	def decimalPositiveParts = positiveNumberPart
	/**
	 * Accepts any character that appears in an integer (positive or negative)
	 */
	val integerPart = Regex("[-\\d]")
	/**
	  * Accepts any character that appears in an integer (positive or negative)
	  */
	@deprecated("Renamed to .integerPart", "v2.2")
	def numericParts = integerPart
	
	/**
	 * A regular expression that matches a single (non-escaped) backslash
	 */
	val backslash = apply("\\\\")
	
	/**
	 * A regular expression that finds parenthesis ( ) content
	 */
	val parentheses = escape('(').followedBy(any).followedBy(escape(')'))
	@deprecated("Renamed to .parentheses", "v2.3")
	def parenthesis = parentheses
	
	/**
	  * Creates a regex that accepts any of the specified characters. You don't need to worry about regular expressions
	  * inside the string, since they are all escaped
	  * @param chars A group of escaped characters
	  * @return A new regex
	  */
	def anyOf(chars: String) = Regex(s"[\\Q$chars\\E]")
	/**
	  * @param chars A group fo characters that are not allowed.
	 *              All specified characters are escaped,
	 *              so you don't need to worry about accidental regular expressions.
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
case class Regex(string: String) extends MaybeEmpty[Regex]
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
	def unary_! = {
		if (string.startsWith("(?!") && string.endsWith(")"))
			Regex(string.substring(3, string.length - 1))
		else if (string.startsWith("(?!") && string.endsWith("$).*")) {
			val dropCount = if (string.startsWith("(?!(?:")) 6 else 3
			Regex(string.substring(dropCount, string.length - 4))
		}
		else if (hasBrackets) {
			if (string.startsWith(Pattern.quote("[^")))
				Regex(s"[${ string.substring(2) }")
			else
				Regex(s"[^${ string.substring(1) }")
		}
		else if (string.endsWith("}") || string.endsWith("*") || string.endsWith("?") || string.endsWith("+"))
			Regex("(?!" + string + "$).*")
		else
			Regex(s"[^$string]")
		/*
		else
			Regex(s"(?!(?:$string)$$).*")
		 */
	}
	
	/**
	  * @return Whether this regex is defined (non-empty)
	  */
	def isDefined = !isEmpty
	
	/**
	  * @return Whether this regex is inside braces [...]
	  */
	def hasBrackets = {
		if (isDefined && string.startsWith("[") && string.endsWith("]")) {
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
	def withinParenthesis = Regex(s"($string)")
	
	/**
	  * @return This regex in sequence 0-n times
	  */
	def anyTimes = if (isEmpty || string.endsWith("*")) this else Regex(s"$string*")
	/**
	  * @return This regex in sequence one or more times
	  */
	def oneOrMoreTimes = if (isEmpty || string.endsWith("+")) this else Regex(s"$string+")
	/**
	  * @return This regex either 0 or exactly 1 times
	  */
	def noneOrOnce = if (isEmpty || string.endsWith("?")) this else Regex(s"$string?")
	
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
	
	override def self: Regex = this
	
	override def isEmpty = string.isEmpty
	
	override def toString = string
	
	
	// OTHER	----------------
	
	/**
	  * @param another Another regex
	  * @return This regex followed by another regex
	  */
	def +(another: Regex) = Regex(s"$string${ another.string }")
	
	/**
	  * @param more Another regex
	  * @return A regex that accepts this regex or the other regex
	  */
	def ||(more: Regex) = {
		if (more.isEmpty)
			this
		else if (isEmpty)
			more
		else if (hasBrackets && more.hasBrackets)
			(withoutBrackets + more.withoutBrackets).withBraces
		else
			Regex(s"$string|${ more.string }")
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
			if (range hasSize 1)
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
	def ignoringWithin(start: Char, end: Char) = {
		val startRegex = Regex.escape(start)
		val endRegex = Regex.escape(end)
		val notStart = !startRegex
		val notEnd = !endRegex
		
		this + "(?=" + (notStart.anyTimes + startRegex + notEnd.anyTimes + endRegex).withinParenthesis
			.anyTimes + Regex.noneOf(s"${ start.toString }$end").anyTimes + "$)"
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
	 * Replaces all instances within a string where this regular expression doesn't match string contents.
	 * @param str String within which replacement is performed
	 * @param replacement Replacing string (call-by-name, lazily initialized)
	 * @return Copy of the specified string where all non-matching sequences have been replaced
	 *         with the specified string
	 */
	def replaceOthers(str: String, replacement: => String): String = replaceOthers(str, Lazy(replacement))
	/**
	 * Replaces all instances within a string where this regular expression doesn't match string contents.
	 * @param str String within which replacement is performed
	 * @param replacement A view that will yield the replacing string
	 * @return Copy of the specified string where all non-matching sequences have been replaced
	 *         with the specified string
	 */
	def replaceOthers(str: String, replacement: View[String]) = {
		val resultBuilder = new StringBuilder()
		val matcher = pattern.matcher(str)
		var lastMatchEnd = 0
		// Finds all matches
		while (matcher.find()) {
			// If there was non-matching area in between, replaces that
			if (matcher.start() > lastMatchEnd)
				resultBuilder ++= replacement.value
			// Adds the matching range
			resultBuilder ++= matcher.group()
			lastMatchEnd = matcher.end()
		}
		// If the end of the string didn't match, replaces it
		if (str.length > lastMatchEnd)
			resultBuilder ++= replacement.value
			
		resultBuilder.result()
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
	def split(str: String): IndexedSeq[String] = {
		val ranges = rangesFrom(str)
		if (ranges.isEmpty)
			Single(str)
		else {
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
	def splitIteratorIn(str: String) = {
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
	def separate(str: String) = {
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
	
	override def next() = {
		if (findCache.pop())
			f(matcher)
		else
			throw new NoSuchElementException("Calling .next() after the end of this iterator")
	}
}