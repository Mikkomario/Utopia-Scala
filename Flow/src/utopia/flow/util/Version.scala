package utopia.flow.util

import utopia.flow.collection.CollectionExtensions._
import StringExtensions._
import utopia.flow.operator.SelfComparable
import utopia.flow.parse.string.Regex

object Version
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * A regular expression for finding version numbers (including the suffix part)
	 */
	// v-prefix is optional and suffix is optional, but can't contain a whitespace
	val regex = Regex("v").noneOrOnce + Regex.digit.oneOrMoreTimes +
		(Regex.escape('.') + Regex.digit.oneOrMoreTimes).withinParenthesis.anyTimes +
		(Regex.escape('-') + (Regex.alphaNumeric || Regex.escape('-')).withinParenthesis.oneOrMoreTimes)
			.withinParenthesis.noneOrOnce
	
	
	// OTHER    -----------------------------
	
	/**
	 * Creates a version
	 * @param major Major version number
	 * @param minor Minor version number (default = 0)
	 * @param patch Patch version number (default = 0)
	 * @param suffix Version suffix, if any (default = "")
	 * @return A new version
	 */
	def apply(major: Int, minor: Int = 0, patch: Int = 0, suffix: String = "") = new Version(
		Vector(major, minor, patch).dropRightWhile { _ == 0 }, suffix)
	
	/**
	 * Parses a version from string
	 * @param versionString A string representation of version. Eg. "v1.2.3-beta-3" or "2.16"
	 * @return A version parsed from specified string
	 */
	def apply(versionString: String) =
	{
		// Checks whether there should be a suffix at the end
		val (numberPart, suffix) = versionString.splitAtFirst("-")
		val numbers = numberPart.split('.').toVector.map { _.digits }.filter { _.nonEmpty }.map { _.toInt }
			.dropRightWhile { _ == 0 }
		new Version(numbers, suffix.trim)
	}
	
	/**
	 * @param text Text that may contain a version number
	 * @return The first version number found from that text. None if no version number was found.
	 */
	def findFrom(text: String) =
	{
		val matchIter = regex.matchesIteratorFrom(text)
		// If the first result doesn't start with 'v', tries to find one that does
		// If that fails, reverts back to the first result
		matchIter.nextOption().map { firstMatch =>
			if (firstMatch.startsWith("v"))
				firstMatch
			else
				matchIter.find { _.startsWith("v") }.getOrElse(firstMatch)
		}.map(apply)
	}
}

/**
 * Represents a single program version / version number
 * @author Mikko Hilpinen
 * @since 3.10.2021
 * @param numbers Version numbers, from most to least important
 * @param suffix A possible suffix for this version (empty string = no suffix)
 */
case class Version private(numbers: Vector[Int], suffix: String) extends SelfComparable[Version]
{
	// COMPUTED	------------------------
	
	/**
	 * @return Whether this version has a specified suffix
	 */
	def hasSuffix = suffix.nonEmpty
	
	/**
	 * @return Major number of this version
	 */
	def major = apply(0)
	/**
	 * @return Minor number of this version
	 */
	def minor = apply(1)
	/**
	 * @return Patch number of this version
	 */
	def patch = apply(2)
	
	/**
	 * @return Whether this version represents a major update (in relation to a previous version)
	 */
	def isMajorUpdate = numbers hasSize 1
	/**
	 * @return Whether this version represents a standard update (in relation to a previous version)
	 */
	def isStandardUpdate = numbers hasSize 2
	/**
	 * @return Whether this version represents a patch (in relation to a previous version)
	 */
	def isPatch = numbers.hasSize >= 3
	
	/**
	 * @return A copy of this version without suffix (returns self if didn't have a suffix to begin with)
	 */
	def withoutSuffix = if (hasSuffix) copy(suffix = "") else this
	
	/**
	 * @return A copy of this version with a major update. E.g. from v1.2 to v2.0
	 */
	def majorUpdated = bumpedAt(0)
	/**
	 * @return A copy of this version with a standard update. E.g. from v1.2 to v1.3
	 */
	def updated = bumpedAt(1)
	/**
	 * @return A copy of this version with a patch update. E.g. from v1.2 to v1.2.1
	 */
	def patched = bumpedAt(2)
	
	
	// IMPLEMENTED	--------------------
	
	override def self = this
	
	override def toString =
	{
		val numString = numbers.padTo(2, 0).mkString(".")
		s"v${ if (hasSuffix) s"$numString-$suffix" else numString }"
	}
	
	// First compares numbers, then suffixes
	override def compareTo(o: Version) = numbers.zip(o.numbers).findMap { case (a, b) =>
		val numCompare = a.compareTo(b)
		if (numCompare == 0) None else Some(numCompare)
	}.getOrElse {
		val lengthCompare = numbers.size.compareTo(o.numbers.size)
		if (lengthCompare == 0)
		{
			if (hasSuffix)
			{
				if (o.hasSuffix)
					suffix.compareTo(o.suffix)
				else
					1
			}
			else if (o.hasSuffix)
				-1
			else
				0
		}
		else
			lengthCompare
	}
	
	
	// OTHER	----------------------
	
	/**
	 * @param index Index of the targeted digit (starting from 0)
	 * @return This version's number at that index
	 */
	def apply(index: Int) = numbers.getOption(index).getOrElse(0)
	
	/**
	 * Creates a bumped copy of this version
	 * @param index Index at which the number is increased (starting from 0)
	 * @param suffix Suffix applied to the new version (default = "")
	 * @return A bumped version
	 */
	def bumpedAt(index: Int, suffix: String = "") =
		new Version(numbers = numbers.take(index) :+ (apply(index) + 1), suffix)
	
	/**
	 * @param suffix New suffix
	 * @return A copy of this version with specified suffix
	 */
	def withSuffix(suffix: String) = copy(suffix = suffix)
}
