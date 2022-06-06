package utopia.vault.coder.util

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.parse.Regex
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._

/**
  * Provides utility methods related to naming
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
object NamingUtils
{
	/**
	  * Converts an under_score name to a CamelCase name (with the first character capitalized)
	  * @param underscoreName A name in underscore format
	  * @return A name in CamelCase format (capitalized)
	  */
	@deprecated("Please use NamingConvention or Name instead", "v1.4.1")
	def underscoreToCamel(underscoreName: String) = underscoreName.split('_').map { _.capitalize }.mkString
	
	/**
	  * Converts a camelCase name to underscore_style name
	  * @param camelName A cameCase property name
	  * @return and underscore_style property name
	  */
	@deprecated("Please use NamingConvention or Name instead", "v1.4.1")
	def camelToUnderscore(camelName: String) =
	{
		val upperCaseRanges = Regex.upperCaseLetter.oneOrMoreTimes.rangesFrom(camelName)
		// Case: Name consists of lower case characters only => Returns as is
		if (upperCaseRanges.isEmpty)
			camelName
		else
		{
			// Ignores the first uppercase letter if it is at the beginning of the string
			val appliedRanges = if (upperCaseRanges.head.start == 0) upperCaseRanges.tail else upperCaseRanges
			// Case: Contains uppercase letters only at the beginning
			if (appliedRanges.isEmpty)
			{
				// Case: Name starts with >1 uppercase letters but is not fully uppercase
				// => adds an underscore in between
				if (upperCaseRanges.head.size > 1 && upperCaseRanges.head.size < camelName.length)
					camelName.slice(upperCaseRanges.head).toLowerCase + "_" + camelName.drop(upperCaseRanges.head.size)
				// Case: 1 Uppercase character or all-caps => lower-cases the name
				else
					camelName.toLowerCase
			}
			else
			{
				// Adds an underscore before uppercase letters. For multiple sequential uppercase letters, adds an
				// underscore to the end as well (unless at the end of the string)
				val lastIndex = camelName.length - 1
				val builder = new StringBuilder
				// Adds the portion before the first uppercase letter
				builder ++= camelName.substring(0, appliedRanges.head.start).toLowerCase
				// Adds the first uppercase sequence
				builder += '_'
				builder ++= camelName.slice(appliedRanges.head).toLowerCase
				if (appliedRanges.head.size > 1 && appliedRanges.head.last < lastIndex)
					builder += '_'
				// Adds the in-between portions and remaining uppercase sequences
				appliedRanges.paired.foreach { case Pair(prevRange, nextRange) =>
					builder ++= camelName.slice(prevRange.last + 1, nextRange.start)
					builder += '_'
					builder ++= camelName.slice(nextRange).toLowerCase
					if (nextRange.size > 1 && nextRange.last < lastIndex)
						builder += '_'
				}
				// Adds the part after the last uppercase sequence
				builder ++= camelName.drop(appliedRanges.last.last + 1)
				
				builder.result()
			}
		}
	}
}
