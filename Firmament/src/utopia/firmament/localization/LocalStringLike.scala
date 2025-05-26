package utopia.firmament.localization

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.parse.string.Regex
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._

/**
  * Common trait for copyable strings with language information
  * @author Mikko Hilpinen
  * @since 22.4.2019, Reflection v1+
  */
trait LocalStringLike[+Repr <: LocalString]
	extends Combinable[LocalString, Repr] with MaybeEmpty[Repr] with EqualsBy
{
	// ABSTRACT --------------------------
	
	/**
	  * @return The wrapped string, as should be displayed
	  */
	def wrapped: String
	/**
	  * @return Wrapped (raw) string passed to [[factory]] when this string is copied.
	  *         Does not (typically) apply any interpolation parameters.
	  */
	def raw: String
	/**
	  * @return Language in which this string is represented
	  */
	def language: Language
	
	/**
	  * @return Factory used for constructing modified copies of this string
	  */
	def factory: LocalStringFactory[Repr]
	
	/**
	  * @return Whether this string has been localized for the user's environment
	  */
	def isLocalized: Boolean
	/**
	  * @return A copy of this string marked as localized,
	  *         but without applying any (additional) localization.
	  */
	def skipLocalization: LocalizedString
	/**
	  * @param localizer Applied localizer (implicit)
	  * @return A localized copy of this string
	  */
	def localized(implicit localizer: Localizer): LocalizedString
	
	
	// COMPUTED	--------------
	
	/**
	  * @return Length of this string
	  */
	def length = wrapped.length
	
	/**
	  * @return This string split on newline characters
	  */
	def lines = split(Regex.newLine)
	
	/**
	  * @return A copy of this string without any control characters in it
	  */
	def stripControlCharacters = map { _.stripControlCharacters }
	
	@deprecated("Please use .wrapped instead", "v1.5")
	def string = wrapped
	@deprecated("Deprecated for removal. Please use .language instead", "v1.5")
	def languageCode = language.code.ifNotEmpty
	
	@deprecated("Please use .skipLocalization instead", "v1.5")
	def localizationSkipped = skipLocalization
	
	
	// IMPLEMENTED	----------
	
	/**
	  * @return Whether this string is empty
	  */
	override def isEmpty = wrapped.isEmpty
	
	override protected def equalsProperties: Seq[Any] = Pair(language, wrapped)
	
	override def toString = wrapped
	
	override def +(other: LocalString): Repr = append(other)
	
	
	// OTHER    --------------
	
	/**
	  * @param f A string modification function
	  * @return A modified copy of this string
	  */
	def map(f: Mutate[String]): Repr = factory(f(raw))
	
	/**
	  * @param start String start index (inclusive)
	  * @param end String end index (exclusive, default = end of string)
	  * @return A subsection of this string
	  * @throws IndexOutOfBoundsException If start < 0 or end > length of this string
	  */
	@throws[IndexOutOfBoundsException]("If start < 0 or end > length of this string")
	def subString(start: Int, end: Int = raw.length) = map { _.substring(start, end) }
	
	/**
	  * Splits this string using the specified regular expression
	  * @param separator A regular expression that identifies the strings that separate parts of this string
	  * @return An iterator that yields split parts of this string
	  */
	def splitIterator(separator: Regex): Iterator[Repr] = separator.splitIteratorIn(raw).map(factory.apply)
	/**
	  * Splits this string using the specified regular expression
	  * @param separator A regular expression that identifies the strings that separate parts of this string
	  * @return Split parts of this string
	  */
	def split(separator: Regex) = splitIterator(separator).toOptimizedSeq
	
	/**
	  * Creates an interpolated version of this string where segments marked with %s, %S, %i or %d are replaced with
	  * the specified arguments parsed into the correct format.
	  *     - %s means raw string format
	  *     - %S means uppercase string format
	  *     - %i means integer format
	  *     - %d means decimal format
	  * @return A version of this string with parameter segments replaced with the specified values
	  */
	def interpolate(first: Any, more: Any*) = interpolateAll(Single(first) ++ more)
	/**
	  * Creates an interpolated version of this string where segments marked with %s, %S, %i or %d are replaced with
	  * the specified arguments parsed into the correct format.
	  *     - %s means raw string format
	  *     - %S means uppercase string format
	  *     - %i means integer format
	  *     - %d means decimal format
	  * @param args The parsed arguments
	  * @return A version of this string with parameter segments replaced with the specified values
	  */
	def interpolateAll(args: Seq[Any]): Repr = factory.interpolate(raw, args)
	/**
	  * Creates an interpolated version of this string where each ${key} is replaced with a matching value from
	  * the specified map
	  * @param args Interpolation arguments (a map)
	  * @return An interpolated version of this string
	  */
	def interpolateNamed(args: Map[String, Any]): Repr = factory.interpolate(raw, args)
	/**
	  * Creates an interpolated version of this string where each ${key} is replaced with a matching value from
	  * the specified map
	  * @return An interpolated version of this string
	  */
	def interpolateNamed(first: (String, Any), more: (String, Any)*): Repr =
		interpolateNamed((Single(first) ++ more).toMap)
	@deprecated("Please use .interpolateAll(Seq) instead", "v1.5")
	def interpolated(args: Seq[Any]): Repr = interpolateAll(args)
	@deprecated("Please use .interpolateNamed(Map) instead", "v1.5")
	def interpolated(args: Map[String, Any]): Repr = factory.interpolate(raw, args)
	
	/**
	  * @param str An appendix string, without language information
	  * @return A copy of this string where the specified string has been appended to this one
	  */
	def +(str: String): Repr = this + LocalString.noLanguage(str)
	def +:(other: LocalString) = prepend(other)
	def +:(other: String) = prepend(LocalString.noLanguage(other))
	/**
	  * Appends another string to this one. Won't combine empty strings.
	  * @param other Another string, including language information
	  * @param separator A separator placed between these strings
	  * @return A combined copy of these strings, where this string appears first
	  */
	def append(other: LocalString, separator: String = "") = {
		if (other.isEmpty)
			self
		else if (isEmpty)
			factory.from(other)
		else
			join(Pair(raw, other.wrapped), separator, other.language)
	}
	/**
	  * Prepends another string to this one. Won't combine empty strings.
	  * @param other Another string, including language information
	  * @param separator A separator placed between these strings
	  * @return A combined copy of these strings, where this string appears second
	  */
	def prepend(other: LocalString, separator: String = "") = {
		if (other.isEmpty)
			self
		else if (isEmpty)
			factory.from(other)
		else
			join(Pair(other.wrapped, raw), separator, other.language)
	}
	
	@deprecated("Please use .map(...) instead", "v1.5")
	def modify(f: Mutate[String]) = map(f)
	
	private def join(parts: Iterable[String], separator: String, otherLanguage: => Language) =
		factory(parts.mkString(separator), language.nonEmptyOrElse(otherLanguage))
}
