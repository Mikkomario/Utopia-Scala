package utopia.flow.util

import utopia.flow.datastructure.mutable.ResettableLazy
import CollectionExtensions._

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
		def words = s.linesIterator.toVector.flatMap { _.split(" ").toVector.map { _.trim }.filter { _.nonEmpty } }
		
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
		def containsAll(first: String, second: String, more: String*): Boolean = containsAll(Vector(first, second) ++ more)
		
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
		 * @return A portion of this string that comes after the first occurrence of specified string
		 *         (returns this string if specified string is not a substring of this string), (case-sensitive)
		 */
		def untilFirst(str: String) = optionIndexOf(str).map(s.take).getOrElse(s)
		
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
		 * A comparison of two strings in a case-insensitive manner
		 * @param another Another string
		 * @return Whether this string equals the other string when case is ignored
		 */
		def ~==(another: String) = s.equalsIgnoreCase(another)
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