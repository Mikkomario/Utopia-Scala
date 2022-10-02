package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.view.mutable.caching.ResettableLazy

/**
 * Contains some utility extensions that extend the capabilities of standard strings
 * @author Mikko Hilpinen
 * @since 1.11.2019, v1.6.1+
 */
object StringExtensions
{
	private val space = ' '
	
	/**
	 * Extends standard scala string
	 * @param s String to extend
	 */
	implicit class ExtendedString(val s: String) extends AnyVal
	{
		/**
		  * @return A copy of this string with all control characters (\t, \n, \r and so forth) removed
		  */
		def stripControlCharacters = s.filter { _ >= space }
		
		/**
		 * @return Words that belong to this string. <b>This includes all non-whitespace characters but not newline characters</b>
		 */
		def words = s.linesIterator.toVector.flatMap { _.split(" ").toVector
			.map { _.trim }.filter { _.nonEmpty } }
		
		/**
		 * @return The first word in this string (may include any characters except whitespace)
		 */
		def firstWord = untilFirst(" ")
		/**
		 * @return The last word in this string (may include any characters except whitespace)
		 */
		def lastWord = afterLast(" ")
		
		/**
		 * @return A non-empty copy of this string or None
		 */
		def notEmpty = if (s.isEmpty) None else Some(s)
		
		/**
		 * @return A copy of this string without any non-letter characters
		 */
		def letters = s.filter { _.isLetter }
		/**
		 * @return A copy of this string without any non-digit characters
		 */
		def digits = s.filter { _.isDigit }
		
		/**
		  * @return A copy of this string surrounded with quotation marks (")
		  */
		def quoted = "\"" + s + "\""
		/**
		  * @return A copy of this string where the first character is in lower case
		  */
		def uncapitalize = if (s.isEmpty) s else s"${s.head.toLower}${s.drop(1)}"
		
		/**
		  * @param default The string returned if this string is empty
		  * @return This string, if not empty, otherwise the 'default' string
		  */
		def nonEmptyOrElse(default: => String) = if (s.isEmpty) default else s
		/**
		  * @param f A mapping function
		  * @return This string if empty, otherwise a mapped copy of this string
		  */
		def mapIfNotEmpty(f: String => String) = if (s.isEmpty) s else f(s)
		
		/**
		  * @param range A range
		  * @return The portion of this string which falls into the specified range
		  */
		def slice(range: Range) =
		{
			if (range.isEmpty)
				""
			else
			{
				val first = (range.start min range.last) max 0
				val last = (range.start max range.last) min (s.length - 1)
				s.substring(first, last + 1)
			}
		}
		
		/**
		  * Cuts a range out of this string
		  * @param range Range of characters to cut
		  * @return The cut away part of this string, then the remaining part of this string
		  */
		def cut(range: Range) =
		{
			if (range.isEmpty)
				"" -> s
			else
			{
				val first = (range.start min range.last) max 0
				val last = (range.start max range.last) min (s.length - 1)
				val cutText = s.substring(first, last + 1)
				val remaining = s.take(first) ++ s.drop(last + 1)
				cutText -> remaining
			}
		}
		
		/**
		 * @param other Another string
		 * @return Whether this string contains specified substring (case-insensitive)
		 */
		def containsIgnoreCase(other: String) = s.toLowerCase.contains(other.toLowerCase)
		
		/**
		 * @param strings A number of strings
		 * @return Whether this string contains all of the provided sub-strings (case-sensitive)
		 */
		def containsAll(strings: IterableOnce[String]) = strings.iterator.forall(s.contains)
		/**
		 * @param first A string
		 * @param second Another string
		 * @param more More strings
		 * @return Whether this string contains all of the provided sub-strings (case-sensitive)
		 */
		def containsAll(first: String, second: String, more: String*): Boolean =
			containsAll(Vector(first, second) ++ more)
		
		/**
		 * @param strings A number of strings
		 * @return Whether this string contains all of the provided sub-strings (case-insensitive)
		 */
		def containsAllIgnoreCase(strings: IterableOnce[String]) =
		{
			val lower = s.toLowerCase
			strings.iterator.forall { searched => lower.contains(searched.toLowerCase) }
		}
		/**
		 * @param first A string
		 * @param second Another string
		 * @param more More strings
		 * @return Whether this string contains all of the provided sub-strings (case-insensitive)
		 */
		def containsAllIgnoreCase(first: String, second: String, more: String*): Boolean = containsAllIgnoreCase(
			Vector(first, second) ++ more)
		
		/**
		  * Checks whether multiple instances of the searched string can be found from this string
		  * @param searched A searched string
		  * @param minimumOccurrences Minimum number of required occurrences (default = 2)
		  * @return Whether this string contains at least 'minimumOccurrences' number of 'searched' sub-strings
		  */
		def containsMany(searched: String, minimumOccurrences: Int = 2) =
			indexOfIterator(searched).existsCount(minimumOccurrences) { _ => true }
		
		/**
		 * @param prefix A prefix
		 * @return Whether this string starts with specified prefix (case-insensitive)
		 */
		def startsWithIgnoreCase(prefix: String) = s.toLowerCase.startsWith(prefix.toLowerCase)
		
		/**
		 * @param suffix A suffix
		 * @return Whether this string ends with specified suffix (case-insensitive)
		 */
		def endsWithIgnoreCase(suffix: String) = s.toLowerCase.endsWith(suffix.toLowerCase)
		
		/**
		 * @param str A searched string
		 * @return Index of the beginning of specified string in this string. None if specified string isn't a
		 *         substring of this string
		 */
		def optionIndexOf(str: String) =
		{
			val raw = s.indexOf(str)
			if (raw < 0) None else Some(raw)
		}
		
		/**
		 * @param str A searched string
		 * @return Last index of the beginning of specified string in this string. None if specified string isn't a
		 *         substring of this string
		 */
		def optionLastIndexOf(str: String) =
		{
			val raw = s.lastIndexOf(str)
			if (raw < 0) None else Some(raw)
		}
		
		/**
		  * @param str Searched string
		  * @return An iterator that returns the next index of the searched string
		  */
		def indexOfIterator(str: String): Iterator[Int] = new StringIndexOfIterator(s, str)
		
		/**
		 * @param str A string
		 * @return A portion of this string that comes after the first occurrence of specified string (empty string if
		 *         specified string is not a substring of this string), (case-sensitive)
		 */
		def afterFirst(str: String) = optionIndexOf(str).map { i => s.drop(i + str.length) }.getOrElse("")
		/**
		  * @param regex A regular expression
		  * @return A portion of this string that appears after the first match of the specified regular expression.
		  *         Empty string if no matches were found.
		  */
		def afterFirstMatch(regex: Regex) = regex.endIndexIteratorIn(s).nextOption() match {
			case Some(end) => s.drop(end)
			case None => ""
		}
		/**
		 * @param str A string
		 * @return A portion of this string that comes after the last occurrence of specified string (empty string if
		 *         specified string is not a substring of this string), (case-sensitive)
		 */
		def afterLast(str: String) = optionLastIndexOf(str).map { i => s.drop(i + str.length) }.getOrElse("")
		
		/**
		 * @param str A string
		 * @return A portion of this string that comes after the first occurrence of specified string,
		 *         including the searched string (returns an empty string if
		 *         specified string is not a substring of this string), (case-sensitive)
		 */
		def dropUntil(str: String) = optionIndexOf(str).map(s.drop).getOrElse("")
		/**
		 * @param str A string
		 * @return A portion of this string that comes after the last occurrence of specified string,
		 *         including the searched string (returns an empty string if
		 *         specified string is not a substring of this string), (case-sensitive)
		 */
		def dropUntilLast(str: String) = optionLastIndexOf(str).map(s.drop).getOrElse("")
		
		/**
		 * @param str A string
		 * @return A portion of this string that comes before the first occurrence of specified string
		 *         (returns this string if specified string is not a substring of this string), (case-sensitive)
		 */
		def untilFirst(str: String) = optionIndexOf(str).map(s.take).getOrElse(s)
		/**
		  * @param regex A regular expression
		  * @return A portion of this string that appears before the first match of the specified regular expression.
		  *         If no matches were found, returns this string.
		  */
		def untilFirstMatch(regex: Regex) = regex.startIndexIteratorIn(s).nextOption() match {
			case Some(start) => s.take(start)
			case None => s
		}
		/**
		 * @param str A string
		 * @return A portion of this string that comes before the last occurrence of specified string
		 *         (returns this string if specified string is not a substring of this string), (case-sensitive)
		 */
		def untilLast(str: String) = optionLastIndexOf(str).map(s.take).getOrElse(s)
		
		/*
		 * Splits this string into two at specified index. Eg. "apple".splitAt(2) = "ap" -> "ple"
		 * @param index Index where this string will be split
		 * @return Part of this string until specified index -> part of this string starting from specified index
		 */
		/*
		def splitAt(index: Int) =
		{
			if (index <= 0)
				"" -> s
			else if (index >= s.length)
				s -> ""
			else
				s.take(index) -> s.drop(index)
		}*/
		
		/**
		  * @param regex A regular expression to split with
		  * @return Parts of this string that didn't match the regular expression
		  */
		def split(regex: Regex) = regex.split(s)
		/**
		  * @param regex A regular expression to split with
		  * @return An iterator that returns parts of this string that didn't match the regular expression.
		  *         Empty parts are not included.
		  */
		def splitIterator(regex: Regex) = regex.splitIteratorIn(s)
		
		/**
		 * Splits this string into two at the first occurrence of specified substring. Eg. "apple".splitAtFirst("p") = "a" -> "ple"
		 * @param str A separator string where this string will be split
		 * @return Part of this string until specified string -> part of this string after specified string (empty if string was not found)
		 */
		def splitAtFirst(str: String) = optionIndexOf(str) match
		{
			case Some(index) => s.take(index) -> s.drop(index + str.length)
			case None => s -> ""
		}
		/**
		  * Splits this string into two at the first regular expression match.
		  * @param regex A regular expression used to separate this string
		  * @return Part of this string until the regular expression match ->
		  *         part of this string after the regular expression match. If no match was made,
		  *         the first part is this whole string and the second part is empty
		  */
		def splitAtFirstMatch(regex: Regex) = regex.firstRangeFrom(s) match {
			case Some(range) => s.take(range.start) -> s.drop(range.end)
			case None => s -> ""
		}
		/**
		 * Splits this string into two at the last occurrence of specified substring. Eg. "apple".splitAtLast("p") = "ap" -> "le"
		 * @param str A separator string where this string will be split
		 * @return Part of this string until specified string -> part of this string after specified string (empty if string was not found)
		 */
		def splitAtLast(str: String) = optionLastIndexOf(str) match
		{
			case Some(index) => s.take(index) -> s.drop(index + str.length)
			case None => s -> ""
		}
		
		/**
		 * Splits this string based on the specified regular expression, but ignores expressions which are surrounded
		 * by quotes
		 * @param regex A regular expression
		 * @return Each part of this string which was separated with such an expression
		 */
		@deprecated("Please use .split(Regex), combining it with .ignoringQuotations (in Regex)", "v1.15")
		def splitIgnoringQuotations(regex: String) =
			s.split(regex + "(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)")
		
		/**
		 * Divides this string based on the specified divider / separator string. Works much like split,
		 * except that the divider / separator is included in the resulting strings. Also, this method
		 * doesn't support regular expressions like split does.
		 * @param divider A divider that separates different parts
		 * @return Divided parts of this string with the dividers included
		 */
		def divideWith(divider: String) =
		{
			val dividerIndices = indexOfIterator(divider).toVector
			// Case: No dividers => returns the string as is
			if (dividerIndices.isEmpty)
				Vector(s)
			else
			{
				val divLength = divider.length
				// Collects the strings between dividers. Includes the dividers themselves.
				val (finalStart, firstParts) = dividerIndices
					.foldLeft((0, Vector[String]())) { case ((start, collected), next) =>
						val nextStart = next + divLength
						val part = s.substring(start, nextStart)
						nextStart -> (collected :+ part)
					}
				// Case: String continues after the last divider
				if (finalStart < s.length)
					firstParts :+ s.substring(finalStart)
				// Case: String ends with a divider
				else
					firstParts
			}
		}
		
		/**
		  * @param regex A regular expression to filter with
		  * @return A copy of this string that only contains segments accepted by that regular expression
		  */
		def filterWith(regex: Regex) = regex.filter(s)
		/**
		  * @param regex A regular expression to filter with
		  * @return A copy of this string without any segments accepted by the specified regular expression
		  */
		def filterNotWith(regex: Regex) = regex.filterNot(s)
		
		/**
		  * Replaces all matches of the specified regular expression with the specified string
		  * @param regex A regular expression to search for
		  * @param replacement Replacement string
		  * @return A copy of this string where the replacements have been made
		  */
		def replaceAll(regex: Regex, replacement: => String) = regex.replaceAll(s, replacement)
		/**
		  * An alias for replaceAll
		  * (used to distinguish between the Java implementation and this extension implementation,
		  * which use the same name)
		  */
		def replaceEachMatchOf(regex: Regex, replacement: => String) = replaceAll(regex, replacement)
	}
	
	private class StringIndexOfIterator(val string: String, val searched: String) extends Iterator[Int]
	{
		// ATTRIBUTES	------------------
		
		private var lastIndex: Option[Int] = None
		private val nextIndex = ResettableLazy[Option[Int]] {
			val next = lastIndex match
			{
				case Some(last) =>
					val result = string.indexOf(searched, last + searched.length)
					if (result < 0) None else Some(result)
				case None => string.optionIndexOf(searched)
			}
			lastIndex = next
			next
		}
		
		
		// IMPLEMENTED	------------------
		
		override def hasNext = nextIndex.value.isDefined
		
		override def next() = nextIndex.pop() match
		{
			case Some(index) => index
			case None => throw new NoSuchElementException("Iterator.next() called after running out of items")
		}
	}
}