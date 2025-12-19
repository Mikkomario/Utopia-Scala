package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.{HasEnds, HasOrderedEnds, NumericSpan}
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
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
		  * @return Whether this string contains multiple lines
		  */
		def isMultiLine = {
			val iter = s.linesIterator
			if (iter.knownSize >= 2)
				true
			else if (iter.hasNext) {
				iter.next()
				iter.hasNext
			}
			else
				false
		}
		
		/**
		  * @return A copy of this string with all control characters (\t, \n, \r and so forth) removed
		  */
		def stripControlCharacters = s.filter { _ >= space }
		/**
		  * @return A copy of this string where all the backslash (i.e. '\') characters have been escaped as \\ instead.
		  */
		def escapeBackSlashes = s.replace("\\", "\\\\")
		
		/**
		  * @return Iterator returning all words that belong to this string.
		  *         <b>This includes all non-whitespace characters but not newline characters</b>
		  */
		def wordsIterator =
			s.linesIterator.flatMap { _.splitIterator(Regex.whiteSpace) }.map { _.trim }.filter { _.nonEmpty }
		/**
		 * @return Words that belong to this string.
		 *         <b>This includes all non-whitespace characters but not newline characters</b>
		 */
		def words = wordsIterator.toOptimizedSeq
		
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
		def uncapitalize = if (s.isEmpty) s else s"${s.head.toLower}${s.tail}"
		
		/**
		  * @param range A range
		  * @return The portion of this string which falls into the specified range
		  */
		def slice(range: Range): String = slice(HasOrderedEnds.from(range))
		/**
		 * @param range A range
		 * @return The portion of this string which falls into the specified range
		 */
		def slice(range: HasEnds[Int]) = range.inclusiveEndsOption match {
			case Some(ends) =>
				val orderedEnds = ends.sorted
				s.substring(orderedEnds.first max 0, (orderedEnds.second + 1) min s.length)
			case None => ""
		}
		/**
		  * Cuts a range out of this string
		  * @param range Range of characters to cut
		  * @return The cut away part of this string, then the remaining part of this string
		  */
		def cut(range: Range): Pair[String] = cut(HasOrderedEnds.from(range))
		/**
		 * Cuts a range out of this string
		 * @param range Range of characters to cut
		 * @return A pair that contains:
		 *              1. the cut away part of this string
		 *              1. the remaining part of this string
		 */
		def cut(range: HasEnds[Int]) = {
			range.inclusiveEndsOption match {
				case Some(ends) =>
					val orderedEnds = ends.sorted
					val first = orderedEnds.first max 0
					val last = orderedEnds.second min (s.length - 1)
					
					Pair(s.substring(first, last + 1), s.take(first) ++ s.drop(last + 1))
				
				case None => Pair("", s)
			}
		}
		
		/**
		 * @param str A searched string
		 * @return Index of the beginning of specified string in this string. None if specified string isn't a
		 *         substring of this string
		 */
		@deprecated("Renamed to findIndexOf(String)", "v2.7")
		def optionIndexOf(str: String) = findIndexOf(str)
		/**
		 * Finds the location of a substring within this string
		 * @param searched The searched string
		 * @return Index of the specified string within this string.
		 *         None if the specified string didn't appear within this string.
		 */
		def findIndexOf(searched: String): Option[Int] = findIndexOf(searched, 0)
		/**
		 * Finds the location of a substring within this string
		 * @param searched The searched string
		 * @param from The starting index / first checked index. Default = 0.
		 * @return Index of the specified string within this string.
		 *         None if the specified string didn't appear within this string.
		 */
		def findIndexOf(searched: String, from: Int): Option[Int] = Some(s.indexOf(searched, from)).filter { _ >= 0 }
		/**
		 * Finds the location of a substring within this string
		 * @param searched The searched string
		 * @param from The starting index / first checked index. Default = 0.
		 * @param ignoreCase Whether to ignore casing. Default = false.
		 * @return Index of the specified string within this string.
		 *         None if the specified string didn't appear within this string.
		 */
		def findIndexOf(searched: String, from: Int = 0, ignoreCase: Boolean = false): Option[Int] = {
			// Case: Ignoring case => Uses 'regionMatches'
			if (ignoreCase) {
				val myLength = s.length
				// Case: 'from' is out of bounds => No containment
				//       NB: This functionality differs between the standard 'indexOf' implementation,
				//           which yields out-of-bounds values when searching for empty strings
				if (from >= myLength)
					None
				else
					_findIndexOf(searched, from max 0, myLength) { (from, to) => (from to to).iterator }
			}
			// Case: Case-sensitive => Uses the standard 'indexOf' operation
			else
				findIndexOf(searched, from)
		}
		/**
		 * @param str A searched string
		 * @return Last index of the beginning of specified string in this string. None if specified string isn't a
		 *         substring of this string
		 */
		@deprecated("Renamed to findLastIndexOf", "v2.7")
		def optionLastIndexOf(str: String) = findLastIndexOf(str)
		/**
		 * @param searched A searched string
		 * @return Index of the last appearance of the specified string in this string.
		 *         None if this string doesn't contain the specified string.
		 */
		def findLastIndexOf(searched: String): Option[Int] = Some(s.lastIndexOf(searched)).filter { _ >= 0 }
		/**
		 * @param searched A searched string
		 * @param from The first searched index (i.e. the rightmost index).
		 *             Default = End of this string.
		 * @return Index of the last appearance of the specified string in this string.
		 *         None if this string doesn't contain the specified string.
		 */
		def findLastIndexOf(searched: String, from: Int) =
			Some(s.lastIndexOf(searched, from)).filter { _ >= 0 }
		/**
		 * @param searched A searched string
		 * @param from The first searched index (i.e. the rightmost index).
		 *             Default = End of this string.
		 * @param ignoreCase Whether to perform the search in a case-insensitive manner (default = false)
		 * @return Index of the last appearance of the specified string in this string.
		 *         None if this string doesn't contain the specified string.
		 */
		def findLastIndexOf(searched: String, from: Int = s.length - 1, ignoreCase: Boolean = false): Option[Int] = {
			if (ignoreCase) {
				if (from < 0)
					None
				else {
					val myLength = s.length
					_findIndexOf(searched, from min (myLength - 1), myLength) { (from, to) =>
						Iterator.iterate(to) { _ - 1 }.takeWhile { _ >= from }
					}
				}
			}
			else
				findLastIndexOf(searched, from)
		}
		/**
		 * @param str Searched string
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return An iterator that returns the next index of the searched string
		 */
		def indexOfIterator(str: String, ignoreCase: Boolean = false): Iterator[Int] =
			new StringIndexOfIterator(s, str, ignoreCase)
		
		/**
		 * Checks a string appears within this string
		 * @param other Another string
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return Whether this string contains the specified string
		 */
		def containsOther(other: String, ignoreCase: Boolean) = {
			if (ignoreCase)
				other.isEmpty || s.findIndexOf(other, ignoreCase = true).isDefined
			else
				s.contains(other)
		}
		/**
		 * @param other Another string
		 * @return Whether this string contains specified substring (case-insensitive)
		 */
		def containsIgnoreCase(other: String) = containsOther(other, ignoreCase = true)
		
		/**
		 * @param strings Strings to search from this one
		 * @param ignoreCase Whether to ignore case differences
		 * @return Whether this string contains all the specified strings
		 */
		def containsAll(strings: Iterable[String], ignoreCase: Boolean) = {
			// Ignores empty strings
			val nonEmptyStrings = strings.filter { _.nonEmpty }
			
			// Case: Nothing to search => Contains "all" of them
			if (nonEmptyStrings.isEmpty)
				true
			else
				nonEmptyStrings.oneOrMany match {
					// Case: Only one string to search => Proceeds directly to 'contains'
					case Left(only) => containsOther(only, ignoreCase)
					// Case: Multiple strings to search
					//       => Makes sure containment is possible for all of them before looking for it
					case Right(strings) =>
						val myLength = s.length
						if (strings.exists { _.length > myLength })
							false
						else
							strings.forall { containsOther(_, ignoreCase) }
				}
		}
		/**
		 * @param strings A number of strings
		 * @return Whether this string contains all the provided sub-strings (case-sensitive)
		 */
		def containsAll(strings: IterableOnce[String]) = strings.iterator.forall(s.contains)
		/**
		 * @param first A string
		 * @param second Another string
		 * @param more More strings
		 * @return Whether this string contains all the provided sub-strings (case-sensitive)
		 */
		def containsAll(first: String, second: String, more: String*): Boolean =
			containsAll(Pair(first, second) ++ more)
		/**
		 * @param strings A number of strings
		 * @return Whether this string contains all the provided sub-strings (case-insensitive)
		 */
		@deprecated("Please use .containsAll(strings, ignoreCase = true) instead", "v2.7")
		def containsAllIgnoreCase(strings: Iterable[String]) = containsAll(strings, ignoreCase = true)
		/**
		 * @param first A string
		 * @param second Another string
		 * @param more More strings
		 * @return Whether this string contains all the provided sub-strings (case-insensitive)
		 */
		def containsAllIgnoreCase(first: String, second: String, more: String*): Boolean =
			containsAll(Pair(first, second) ++ more, ignoreCase = true)
		/**
		  * Checks whether multiple instances of the searched string can be found from this string
		  * @param searched A searched string
		  * @param minimumOccurrences Minimum number of required occurrences (default = 2)
		  * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return Whether this string contains at least 'minimumOccurrences' number of 'searched' sub-strings
		  */
		def containsMany(searched: String, minimumOccurrences: Int = 2, ignoreCase: Boolean = false) =
			indexOfIterator(searched, ignoreCase).existsCount(minimumOccurrences) { _ => true }
		
		/**
		 * Checks whether this string contains all the specified sub-strings
		 * in the same order in which they're listed.
		 * @return Whether this string contains the specified sub-strings in order.
		 */
		def containsInOrder(first: String, second: String, more: String*): Boolean =
			containsInOrder(Pair(first, second) ++ more)
		/**
		 * Checks whether this string contains all the specified sub-strings
		 * in the same order in which they're listed.
		 * @param searched The sub-strings that are searched from this string
		 * @param ignoreCase Whether to perform the search in a case-insensitive manner (default = false)
		 * @return Whether this string contains the specified sub-strings in order.
		 */
		def containsInOrder(searched: Seq[String], ignoreCase: Boolean = false) = searched
			.foldLeftIterator[Option[Int]](Some(0)) { (from, searched) =>
				from.flatMap { from => findIndexOf(searched, from, ignoreCase).map { _ + searched.length } }
			}
			.forall { _.isDefined }
		
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
		 * Finds the longest overlap between these two strings
		 * @param other Another string
		 * @return The longest overlapping part between these strings.
		 *         If multiple overlaps of the same length are found, returns the first one only.
		 *         If no overlap exists, returns an empty string.
		 */
		def overlapWith(other: String) = {
			if (s.isEmpty || other.isEmpty)
				""
			else {
				// Stores the longest encountered overlap here
				var longestOverlap = ""
				var longestOverlapLength = 0
				
				// Iterates over this string in order to find overlap starting positions
				// Implements early stopping, after the length of this string starts limiting the maximum overlap length
				val myLength = s.length
				val theirLength = other.length
				s.iterator.zipWithIndex.takeWhile { _._2 < myLength - longestOverlapLength }
					// For each character in this string, looks for possible overlaps in the other string
					.foreach { case (my, myIndex) =>
						// Again, implements early stopping
						other.iterator.zipWithIndex.takeWhile { _._2 < theirLength - longestOverlapLength }
							.foreach { case (their, theirIndex) =>
								// Case: Found a part that starts with the same character
								//       => Checks how long of an overlap may be acquired from this matching point
								if (my == their) {
									val additionalOverlapLength =
										s.drop(myIndex + 1).iterator.zip(other.drop(theirIndex + 1))
											.takeWhile { case (my, their) => my == their }.size
									// Case: New longest overlap found => Remembers it
									if (additionalOverlapLength >= longestOverlapLength) {
										longestOverlap = s.slice(myIndex, myIndex + additionalOverlapLength + 1)
										longestOverlapLength = additionalOverlapLength + 1
									}
								}
							}
					}
				longestOverlap
			}
		}
		
		/**
		 * @param prefix A possible prefix
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return Whether this string starts with the specified prefix
		 */
		def startsWith(prefix: String, ignoreCase: Boolean) = {
			if (ignoreCase)
				prefix.isEmpty || s.regionMatches(true, 0, prefix, 0, prefix.length)
			else
				s.startsWith(prefix)
		}
		/**
		 * @param suffix A possible suffix
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return Whether this string ends with the specified suffix
		 */
		def endsWith(suffix: String, ignoreCase: Boolean) = {
			if (ignoreCase) {
				if (suffix.isEmpty)
					true
				else {
					val suffixLength = suffix.length
					s.regionMatches(true, s.length - suffixLength, suffix, 0, suffixLength)
				}
			}
			else
				s.endsWith(suffix)
		}
		/**
		 * @param prefix A prefix
		 * @return Whether this string starts with specified prefix (case-insensitive)
		 */
		def startsWithIgnoreCase(prefix: String) = startsWith(prefix, ignoreCase = true)
		/**
		 * @param suffix A suffix
		 * @return Whether this string ends with specified suffix (case-insensitive)
		 */
		def endsWithIgnoreCase(suffix: String) = endsWith(suffix, ignoreCase = true)
		
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
		  * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return Copy of this string without the specified prefix, if present.
		  *         Otherwise returns this string as is.
		  */
		def notStartingWith(prefix: String, ignoreCase: Boolean = false) =
			if (s.startsWith(prefix, ignoreCase)) s.drop(prefix.length) else s
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
		  * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return Copy of this string without the specified suffix, if present.
		  *         Otherwise returns this string as is.
		  */
		def notEndingWith(suffix: String, ignoreCase: Boolean = false) =
			if (s.endsWith(suffix, ignoreCase)) s.dropRight(suffix.length) else s
		/**
		 * @param edge Edge to place on both sides of this string
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A copy of this string starting and ending with the specified substring.
		 *         If this string already started or ended with that string, or a part of that string,
		 *         won't repeat it.
		 */
		def surroundedWith(edge: String, ignoreCase: Boolean = false) = {
			if (s.isEmpty)
				edge * 2
			else
				startingWith(edge, ignoreCase).endingWith(edge, ignoreCase)
		}
		
		/**
		 * If this string is shorter than the specified length, prepends it with 'elem' until it reaches that length.
		 * E.g. "123".prependTo(4, '0') would yield "01234".
		 * @param length Target (minimum) length
		 * @param elem The character to prepend to this string, if appropriate (call-by-name)
		 * @return This string prepended to at least 'length'
		 */
		def prependTo(length: Int, elem: => Char) = {
			val prependCount = length - s.length
			if (prependCount <= 0)
				s
			else
				s"${ elem.toString * prependCount }$s"
		}
		
		/**
		 * @param string A string that is not allowed
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A copy of this string not containing the specified string even once.
		 *         If this string didn't contain the specified string, yields this.
		 */
		def notContaining(string: String, ignoreCase: Boolean = false) = {
			// Attempts to find the targeted string within this instance
			findIndexOf(string, ignoreCase = ignoreCase) match {
				// Case: At least one match => Looks for others and builds the final result
				case Some(firstMatchIndex) =>
					val builder = new StringBuilder(s.take(firstMatchIndex))
					val step = string.length
					var cursor = firstMatchIndex + step
					
					// Looks for additional matches until no more can be found or the whole string has been exhausted
					while (cursor > 0 && cursor < s.length) {
						findIndexOf(string, cursor, ignoreCase) match {
							case Some(nextIndex) =>
								builder ++= s.slice(cursor, nextIndex)
								cursor = nextIndex + step
							case None =>
								builder ++= s.drop(cursor)
								cursor = -1
						}
					}
					
					builder.result()
				
				// Case: No match => No change
				case None => s
			}
		}
		/**
		 * @param strings Strings that are not allowed
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A copy of this string not containing any of the specified strings even once.
		 *         If this string didn't contain any of the specified strings, yields this.
		 */
		def notContainingAnyOf(strings: IterableOnce[String], ignoreCase: Boolean = false) =
			strings.iterator.foldLeft(s) { _.notContaining(_, ignoreCase) }
		
		/**
		 * @param str A string
		 * @param ignoreCase Whether to ignore case differences (default = false).
		 * @return A portion of this string that comes after the first occurrence of specified string.
		 *         Empty string if specified string doesn't appear within this string.
		 */
		def afterFirst(str: String, ignoreCase: Boolean = false) =
			findIndexOf(str, ignoreCase = ignoreCase) match {
				case Some(i) => s.drop(i + str.length)
				case None => ""
			}
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
		 * @return A portion of this string that comes after the last occurrence of specified string.
		 *         Empty string if specified string doesn't appear within this string.
		 */
		def afterLast(str: String, ignoreCase: Boolean = false) =
			findLastIndexOf(str, ignoreCase = ignoreCase) match {
				case Some(i) => s.drop(i + str.length)
				case None => ""
			}
		
		/**
		 * @param str A string
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A portion of this string that comes before the first occurrence of specified string.
		 *         This string if specified string is not a substring of this string.
		 */
		def untilFirst(str: String, ignoreCase: Boolean = false) =
			findIndexOf(str, ignoreCase = ignoreCase) match {
				case Some(i) => s.take(i)
				case None => s
			}
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
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A portion of this string that comes before the last occurrence of specified string.
		 *         This string if specified string is not a substring of this string.
		 */
		def untilLast(str: String, ignoreCase: Boolean = false) =
			findLastIndexOf(str, ignoreCase = ignoreCase) match {
				case Some(i) => s.take(i)
				case None => s
			}
		
		/**
		 * @param str A string
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A portion of this string that comes after the first occurrence of specified string,
		 *         including the searched string.
		 *         An empty if the specified string doesn't appear within this string.
		 */
		def dropUntil(str: String, ignoreCase: Boolean = false) =
			findIndexOf(str, ignoreCase = ignoreCase) match {
				case Some(i) => s.drop(i)
				case None => ""
			}
		/**
		 * @param str A string
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A portion of this string that comes after the last occurrence of specified string,
		 *         including the searched string.
		 *         An empty string if the specified string doesn't appear within this one.
		 */
		def dropUntilLast(str: String, ignoreCase: Boolean = false) =
			findLastIndexOf(str, ignoreCase = ignoreCase) match {
				case Some(i) => s.drop(i)
				case None => ""
			}
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
		 * Splits this string into two at the first occurrence of specified substring.
		 * E.g. "apple".splitAtFirst("p") = "a" -> "ple"
		 * @param str A separator string where this string will be split
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A pair of strings:
		 *              1. Part of this string until specified string
		 *              1. Part of this string after specified string (empty if string was not found)
		 */
		def splitAtFirst(str: String, ignoreCase: Boolean = false) =
			findIndexOf(str, ignoreCase = ignoreCase) match {
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
		 * Splits this string into two at the last occurrence of specified substring.
		 * E.g. "apple".splitAtFirst("p") = "ap" -> "le"
		 * @param str A separator string where this string will be split
		 * @param ignoreCase Whether to ignore case differences (default = false)
		 * @return A pair of strings:
		 *              1. Part of this string until specified string
		 *              1. Part of this string after specified string (empty if string was not found)
		 */
		def splitAtLast(str: String, ignoreCase: Boolean = false) =
			findLastIndexOf(str, ignoreCase = ignoreCase) match {
				case Some(index) => Pair(s.take(index), s.drop(index + str.length))
				case None => Pair(s, "")
			}
		
		/**
		  * Splits this string based on maximum line length
		  * @param maxCharactersPerLine Maximum amount of characters that may be included in a single line
		  * @param splitRegex Regular expression for identifying a potential line split location.
		 *                   Default = look for whitespaces.
		 * @param splitterOnNewLine Whether the line splitters (e.g. whitespaces)
		 *                       should be placed on the new line instead of being left at the end of the previous line.
		 *                       Default = false.
		 * @return An iterator that returns lines that are <= 'maxCharactersPerLine' long, except in cases where
		  *         individual words exceed this length limit.
		  */
		def splitToLinesIterator(maxCharactersPerLine: Int, splitRegex: Regex = Regex.whiteSpace,
		                         splitterOnNewLine: Boolean = false) =
			s.linesIterator.flatMap { str =>
				val length = str.length
				// Case: This string already fits a single line
				if (length <= maxCharactersPerLine)
					Iterator.single(str)
				// Case: Splitting is necessary
				else {
					// Checks all places where this string may be split
					val possibleSplitIndices = {
						if (splitterOnNewLine)
							splitRegex.startIndexIteratorIn(str).toVector
						else
							splitRegex.endIndexIteratorIn(str).toVector
					}
					// Finds the optimal split intervals
					OptionsIterator
						.iterate(Some(0 -> -1)) { case (lineStartIndex, lastSplitIndex) =>
							val maxIndex = lineStartIndex + maxCharactersPerLine
							// Case: This is the last line => No need for further splitting
							if (maxIndex >= length)
								None
							else
								possibleSplitIndices.view.zipWithIndex.drop(lastSplitIndex + 1)
									.takeWhile { _._1 <= maxIndex }.lastOption
									.orElse {
										Some(lastSplitIndex + 1).filter { _ < possibleSplitIndices.size }
											.map { i => possibleSplitIndices(i) -> i }
									}
						}
						// Converts the selected split intervals to lines
						.map { _._1 }.pairedTo(length).map { range => str.substring(range.first, range.second) }
				}
			}
		
		/**
		 * Divides this string based on the specified divider / separator string. Works much like split,
		 * except that the divider / separator is included in the resulting strings. Also, this method
		 * doesn't support regular expressions like split does.
		 * @param divider A divider that separates different parts
		 * @param ignoreCase Whether to ignore case differences when matching (defualt = false)
		 * @return Divided parts of this string with the dividers included
		 */
		def divideWith(divider: String, ignoreCase: Boolean = false) = {
			val dividerIndices = indexOfIterator(divider, ignoreCase).toOptimizedSeq
			// Case: No dividers => returns the string as is
			if (dividerIndices.isEmpty)
				Single(s)
			else {
				val divLength = divider.length
				// Collects the strings between dividers. Includes the dividers themselves.
				val (finalStart, firstParts) = dividerIndices
					.foldLeft((0, Empty: IndexedSeq[String])) { case ((start, collected), next) =>
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
		
		/**
		 * A direction-insensitive implementation of findIndexOf
		 * @param searched The searched string
		 * @param startIndex The first searched index. Must be within bounds.
		 * @param myLength The length if this string
		 * @param indexIteratorFrom A function that acquires an iterator from an inclusive range (start & end).
		 *                          If searching for the last index, should apply a reverse iterator.
		 * @return Index where the searched string appears within this string
		 */
		private def _findIndexOf(searched: String, startIndex: Int, myLength: Int)
		                        (indexIteratorFrom: (Int, Int) => Iterator[Int]) =
		{
			// Case: The searched string is empty => Always contains it at the first available position
			if (searched.isEmpty)
				Some(startIndex)
			else {
				val searchedLength = searched.length
				// Case: The searched string is too long => No containment
				if (searchedLength > myLength)
					None
				// Case: Containment is possible => Looks for an index where it occurs
				else
					indexIteratorFrom(startIndex, myLength - searchedLength).find { start =>
						s.regionMatches(true, start, searched, 0, searchedLength)
					}
			}
		}
	}
	
	private class StringIndexOfIterator(string: String, searched: String, ignoreCase: Boolean) extends Iterator[Int]
	{
		// ATTRIBUTES	------------------
		
		private var lastIndex: Option[Int] = None
		private val nextIndex = ResettableLazy[Option[Int]] {
			val next = lastIndex match {
				// Case: Previous match was found => Finds the next match
				case Some(last) => string.findIndexOf(searched, last + searched.length, ignoreCase)
				// Case: No searches performed yet => Finds the first match
				case None => string.findIndexOf(searched, ignoreCase = ignoreCase)
			}
			lastIndex = next
			next
		}
		
		
		// IMPLEMENTED	------------------
		
		override def hasNext = nextIndex.value.isDefined
		
		override def next() = nextIndex.pop() match {
			case Some(index) => index
			case None => throw new NoSuchElementException("Iterator.next() called after running out of items")
		}
	}
}