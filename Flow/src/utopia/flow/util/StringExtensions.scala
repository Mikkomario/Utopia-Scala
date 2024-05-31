package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.equality.EqualsFunction
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
		  * @return A copy of this string where all the backslash (i.e. '\') characters have been escaped as \\ instead.
		  */
		def escapeBackSlashes = s.replace("\\", "\\\\")
		
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
		def quoted = s"\"$s\""
		/**
		  * @return A copy of this string where the first character is in lower case
		  */
		def uncapitalize = if (s.isEmpty) s else s"${s.head.toLower}${s.drop(1)}"
		
		/**
		  * @param range A range
		  * @return The portion of this string which falls into the specified range
		  */
		def slice(range: Range) = {
			if (range.isEmpty)
				""
			else {
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
		def cut(range: Range) = {
			if (range.isEmpty)
				"" -> s
			else {
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
		def containsAllIgnoreCase(strings: IterableOnce[String]) = {
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
		 * Checks whether this string contains all the same characters as the specified string.
		 * This string may be longer, however, and the specified characters may appear in different order.
		 *
		 * In order for the characters to match, they must appear at least as many times as in the specified string.
		 * E.g. "Apple" contains "pAle" but not "palle" or "pale" (if case-sensitive).
		 *
		 * @param string A string that may be contained within this string
		 * @param ignoreCase Whether this checking should be case-insensitive (default = false)
		 * @return Whether all characters of the specified string appear within this string at least that many times.
		 */
		def containsAllCharsFrom(string: IterableOnce[Char], ignoreCase: Boolean = false) = {
			// An empty string may only contain another empty string
			if (s.isEmpty)
				string.iterator.isEmpty
			else {
				// Checks whether the specified string or character set is too large to be contained within this string
				val otherSize = string.knownSize
				if (otherSize < 0 || otherSize <= s.length) {
					val iterableS: Iterable[Char] = s
					val compare = if (ignoreCase) EqualsFunction.charCaseInsensitive else EqualsFunction.default
					string.countAll.forall { case (char, count) => iterableS.existsCount(count) { compare(_, char) } }
				}
				else
					false
			}
		}
		/**
		 * Checks whether all the specified characters appear within this string,
		 * and do so in the correct order.
		 * Case-sensitive.
		 *
		 * @param chars Characters to search from this string
		 * @param ignoreCase Whether this checking should be case-insensitive (default = false)
		 * @return True if this string contains all of the specified characters
		 *         (at least as many times as specified in 'chars') in the same order as they are specified in 'chars'.
		 *
		 *         E.g. "Utopia Flow" contains "tolo" but not "u" (if case-sensitive) nor "loto".
		 */
		def containsCharsInOrder(chars: IterableOnce[Char], ignoreCase: Boolean = false) = {
			val compare = if (ignoreCase) EqualsFunction.charCaseInsensitive else EqualsFunction.default
			var startIndex = 0
			chars.iterator.forall { char =>
				startIndex = s.indexWhere(compare(_, char), startIndex) + 1
				startIndex > 0
			}
		}
		
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
		  * @param prefix A prefix with which the resulting string will start
		  * @param enablePartialReplacement Whether the prefix and this string may partially overlap, causing
		  *                                 only a portion of the prefix to be added.
		  *                                 For example, if true, "banana".startingWith("abba") will yield "abbanana";
		  *                                 If false, this would yield "abbabanana" instead.
		  * @return A copy of this string that starts with the specified string.
		  *         If this string already starts with the specified string, returns this string.
		  */
		def startingWith(prefix: String, enablePartialReplacement: Boolean = false) = {
			// Case: Partial replacement technique used
			if (enablePartialReplacement) {
				// Starts at the rightmost match option (the most rewarding)
				// and moves left towards the more probable cases
				NumericSpan(prefix.length min s.length, 0).findMap { len =>
					if (s.take(len) == prefix.takeRight(len))
						Some(s"${ prefix.dropRight(len) }$s")
					else
						None
				}.getOrElse { s"$prefix$s" }
			}
			// Case: Already starts with the specified string
			else if (s.startsWith(prefix))
				s
			// Case: Prepending required
			else
				s"$prefix$s"
		}
		/**
		  * @param prefix Prefix that is not allowed
		  * @return Copy of this string without the specified prefix, if present.
		  *         Otherwise returns this string as is.
		  */
		def notStartingWith(prefix: String) = if (s.startsWith(prefix)) s.drop(prefix.length) else s
		/**
		  * @param suffix                   A suffix with which the resulting string will end
		  * @param enablePartialReplacement Whether the suffix and this string may partially overlap, causing
		  *                                 only a portion of the suffix to be appended.
		  *                                 For example, if true, "banana".endingWith("apple") will yield "bananapple";
		  *                                 If false, this would yield "bananaapple" (2 a's) instead.
		  * @return A copy of this string that ends with the specified string.
		  *         If this string already ends with the specified string, returns this string.
		  */
		def endingWith(suffix: String, enablePartialReplacement: Boolean = false) = {
			// Case: Partial replacement technique used
			if (enablePartialReplacement) {
				// Starts at the leftmost match option (the most rewarding)
				// and moves right towards the more probable cases
				NumericSpan(suffix.length min s.length, 0).findMap { len =>
					if (s.takeRight(len) == suffix.take(len))
						Some(s"$s${ suffix.drop(len) }")
					else
						None
				}.getOrElse { s"$s$suffix" }
			}
			// Case: Already ends with the specified string
			else if (s.endsWith(suffix))
				s
			// Case: Appending required
			else
				s"$s$suffix"
		}
		/**
		  * @param suffix A suffix that is not allowed
		  * @return Copy of this string without the specified suffix, if present.
		  *         Otherwise returns this string as is.
		  */
		def notEndingWith(suffix: String) = if (s.endsWith(suffix)) s.dropRight(suffix.length) else s
		
		/**
		 * @param str A searched string
		 * @return Index of the beginning of specified string in this string. None if specified string isn't a
		 *         substring of this string
		 */
		def optionIndexOf(str: String) = {
			val raw = s.indexOf(str)
			if (raw < 0) None else Some(raw)
		}
		/**
		 * @param str A searched string
		 * @return Last index of the beginning of specified string in this string. None if specified string isn't a
		 *         substring of this string
		 */
		def optionLastIndexOf(str: String) = {
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
		  * @param f A function that returns true for the characters to remove from the end of this string.
		  *          Called from right to left, as long as it returns true and there are characters left.
		  * @return Copy of this string without the last 'n' characters where n is the number of
		  *         times the specified function returned true.
		  */
		def dropRightWhile(f: Char => Boolean) = {
			val dropLength = s.reverseIterator.takeWhile(f).size
			if (dropLength == 0) s else s.dropRight(dropLength)
		}
		
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
		def splitAtFirst(str: String) = optionIndexOf(str) match {
			case Some(index) => Pair(s.take(index), s.drop(index + str.length))
			case None => Pair(s, "")
		}
		/**
		  * Splits this string into two at the first regular expression match.
		  * @param regex A regular expression used to separate this string
		  * @return Part of this string until the regular expression match ->
		  *         part of this string after the regular expression match. If no match was made,
		  *         the first part is this whole string and the second part is empty
		  */
		def splitAtFirstMatch(regex: Regex) = regex.firstRangeFrom(s) match {
			case Some(range) => Pair(s.take(range.start), s.drop(range.end))
			case None => Pair(s, "")
		}
		/**
		 * Splits this string into two at the last occurrence of specified substring. Eg. "apple".splitAtLast("p") = "ap" -> "le"
		 * @param str A separator string where this string will be split
		 * @return Part of this string until specified string -> part of this string after specified string (empty if string was not found)
		 */
		def splitAtLast(str: String) = optionLastIndexOf(str) match {
			case Some(index) => Pair(s.take(index), s.drop(index + str.length))
			case None => Pair(s, "")
		}
		
		/**
		 * Divides this string based on the specified divider / separator string. Works much like split,
		 * except that the divider / separator is included in the resulting strings. Also, this method
		 * doesn't support regular expressions like split does.
		 * @param divider A divider that separates different parts
		 * @return Divided parts of this string with the dividers included
		 */
		def divideWith(divider: String) = {
			val dividerIndices = indexOfIterator(divider).toVector
			// Case: No dividers => returns the string as is
			if (dividerIndices.isEmpty)
				Vector(s)
			else {
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
		/**
		 * Replaces all instances within this string where the specified regular expression doesn't match
		 * string contents.
		 * @param regex A regular expression used to match the preserved areas
		 * @param replacement A view that will yield the replacing string
		 * @return Copy of the specified string where all non-matching sequences have been replaced
		 *         with the specified string
		 */
		def replaceAllExcept(regex: Regex, replacement: => String) = regex.replaceOthers(s, replacement)
		
		/**
		  * @param other Another string
		  * @param allowedDifference The maximum number of differences allowed between these strings in order for
		  *                          this function to return true.
		  *
		  *                          Differences come in two forms:
		  *                             1) Character is missing from one of these strings (e.g. "apple" vs. "aple"), or
		  *                             2) Character is swapped (e.g. "tyko" vs, "typo")
		  *
		  *                          Please note that case-difference (e.g. "A" vs "a")
		  *                          does NOT constitute a difference in this function.
		  *
		  * @return Whether these two strings no more than the specified number of differences.
		  */
		def isSimilarTo(other: String, allowedDifference: Int) =
			areSimilar(Pair(s, other), Pair.twice(0), allowedDifference)
		private def areSimilar(strings: Pair[String], indices: Pair[Int], allowedDifference: Int): Boolean = {
			// Used when the end of one string is reached
			def lengthDifferenceResult = {
				val lengthDifference = strings.mergeWith(indices) { (s, i) => (s.length - i) max 0 }.merge { _ - _ }.abs
				if (lengthDifference > allowedDifference)
					false
				else
					true
			}
			
			// Only returns valid index pairs
			val indexIter = Iterator.iterate(indices) { _.map { _ + 1 } }
				.takeWhile { _.forallWith(strings) { _ < _.length } }
			// Case: There are more characters to compare =>
			// Compares until finds a difference or until reaches the end of a string
			if (indexIter.hasNext) {
				indexIter.find { strings.mergeWith(_) { _(_) }.isAsymmetricWith(EqualsFunction.charCaseInsensitive) } match {
					// Case: Difference found => Terminates or splits
					case Some(nonMatchingIndex) =>
						if (allowedDifference > 1) {
							// Splits based on 3 assumptions:
							//      1) An invalid character (swap)
							//      2) A missing character
							//      3) An additional character
							// Are similar if they can be similar enough with any of those assumptions
							areSimilar(strings, nonMatchingIndex.map { _ + 1 }, allowedDifference - 1) ||
								areSimilar(strings, nonMatchingIndex.mapSecond { _ + 1 }, allowedDifference - 1) ||
								areSimilar(strings, nonMatchingIndex.mapFirst { _ + 1 }, allowedDifference - 1)
						}
						else
							false
					// Case: End of a string reached => Checks lengths and returns
					case None => lengthDifferenceResult
				}
			}
			// Case: End of a string reached => Checks lengths and returns
			else
				lengthDifferenceResult
		}
		
		/*
		  * @return Some(this) if not empty. None if empty.
		  */
		def ifNotEmpty = if (s.isEmpty) None else Some(s)
		/**
		  * @param f Function to call for this string if this is not an empty string
		  * @tparam U Arbitrary function result type
		  */
		def forNonEmpty[U](f: String => U): Unit = if (s.nonEmpty) f(s)
		
		// NB: These are copied from MayBeEmpty -
		//  Can't inherit it because implicit casting then fails with StringOps ambiguity
		
		/**
		  * @param default An item to return in case this one is empty (call-by-name)
		  * @tparam B Type of the default result
		  * @return This if not empty, otherwise the default
		  */
		def nonEmptyOrElse[B >: String](default: => B) = if (s.isEmpty) default else s
		/**
		  * @param f A mapping function to apply for non-empty items
		  * @tparam B Type of mapping result
		  * @return A mapped copy of this item, if this item was not empty.
		  *         Otherwise returns this item.
		  */
		def mapIfNotEmpty[B >: String](f: String => B) = if (s.isEmpty) s else f(s)
		
		/**
		  * @param suffix A suffix to append to this string if it is not empty
		  * @return This string if empty, otherwise an appended version
		  */
		def appendIfNotEmpty(suffix: => String) = if (s.isEmpty) s else s"$s$suffix"
		/**
		  * @param prefix A prefix to prepend to this string if it is not empty
		  * @return This string if empty, otherwise a prepended version
		  */
		def prependIfNotEmpty(prefix: => String) = if (s.isEmpty) s else s"$prefix$s"
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