package utopia.vault.coder.model.enumeration

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.parse.Regex
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._

import scala.collection.immutable.VectorBuilder

/**
  * An enumeration for different styles of naming properties and classes, etc.
  * @author Mikko Hilpinen
  * @since 3.2.2022, v1.4.1
  */
sealed trait NamingConvention
{
	/**
	  * @param name A name / string
	  * @return Whether that name conforms to this naming convention as-is
	  */
	def accepts(name: String): Boolean
	
	/**
	  * Converts a string from one naming convention to this one
	  * @param string A string to convert
	  * @param originalStyle Naming convention of the original string
	  * @return Version of the string applicable to this naming convention
	  */
	def convert(string: String, originalStyle: NamingConvention): String
	
	/**
	  * Combines two strings using this naming convention
	  * @param beginning The beginning part
	  * @param end The end part
	  * @return Combination of these parts under this convention
	  */
	def combine(beginning: String, end: String): String
}

object NamingConvention
{
	// ATTRIBUTES   ------------------------
	
	private lazy val underscoreRegex = Regex.escape('_')
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param string A string
	  * @return A guess of that string's naming convention
	  */
	def of(string: String) = {
		if (string.contains(' ')) {
			if (string.view.drop(1).exists { _.isUpper })
				Text.allCapitalized
			else if (string.head.isUpper)
				Text.firstCapitalized
			else
				Text.lower
		}
		else if (string.contains('_'))
			UnderScore
		else if (string.headOption.exists { _.isUpper })
			CamelCase.capitalized
		else
			CamelCase.lower
	}
	/**
	  * @param string A string
	  * @param expected Expected naming convention
	  * @return That string's naming convention
	  */
	def of(string: String, expected: NamingConvention): NamingConvention = {
		if (expected.accepts(string))
			expected
		else
			of(string)
	}
	
	/**
	  * @param styleName A string representing a naming convention name
	  * @return A naming convention that matches the specified name, if one is found
	  */
	def forName(styleName: String) = {
		val lower = styleName.toLowerCase
		lazy val isCapitalized = styleName.headOption.exists { _.isUpper }
		lower match {
			case "camel" | "camelcase" => Some(if (isCapitalized) CamelCase.capitalized else CamelCase.lower)
			case "under" | "underscore" => Some(UnderScore)
			case "text" | "doc" => Some(if (isCapitalized) Text.allCapitalized else Text.lower)
			case _ => None
		}
	}
	
	// All parts are in lower-case letters
	private def camelParts(camelName: String) = {
		val upperCaseRanges = Regex.upperCaseLetter.oneOrMoreTimes.rangesFrom(camelName)
		// Case: Name consists of lower case characters only => Returns as is
		if (upperCaseRanges.isEmpty)
			Vector(camelName)
		else
		{
			// Ignores the first uppercase letter if it is at the beginning of the string
			val appliedRanges = if (upperCaseRanges.head.start == 0) upperCaseRanges.tail else upperCaseRanges
			// Case: Contains uppercase letters only at the beginning
			if (appliedRanges.isEmpty)
			{
				// Case: Name starts with >1 uppercase letters but is not fully uppercase
				// => separates the two parts
				if (upperCaseRanges.head.size > 1 && upperCaseRanges.head.size < camelName.length)
					Vector(camelName.slice(upperCaseRanges.head).toLowerCase, camelName.drop(upperCaseRanges.head.size))
				// Case: 1 Uppercase character or all-caps => lower-cases the name
				else
					Vector(camelName.toLowerCase)
			}
			else
			{
				// Separates before uppercase letters. For multiple sequential uppercase letters,
				// separates at the end as well (unless at the end of the string)
				val builder = new VectorBuilder[String]()
				// Adds the portion before the first uppercase letter
				if (appliedRanges.head.start > 0)
					builder += camelName.substring(0, appliedRanges.head.start).toLowerCase
				// Adds the first uppercase sequence
				builder += camelName.slice(appliedRanges.head).toLowerCase
				// Adds the in-between portions and remaining uppercase sequences
				appliedRanges.paired.foreach { case Pair(prevRange, nextRange) =>
					builder += camelName.slice(prevRange.last + 1, nextRange.start)
					builder += camelName.slice(nextRange).toLowerCase
				}
				// Adds the part after the last uppercase sequence
				if (appliedRanges.last.last < camelName.length - 1)
					builder += camelName.drop(appliedRanges.last.last + 1)
				
				builder.result()
			}
		}
	}
	
	
	// NESTED   ----------------------------
	
	object CamelCase
	{
		/**
		  * CamelCase naming convention where first letter is in lower case
		  */
		val lower = apply(false)
		/**
		  * CamelCase naming convention where the first letter is in upper case
		  */
		val capitalized = apply(true)
	}
	/**
	  * CamelCase naming convention, where word separations are indicated by a case change.
	  * E.g. "wordOfLife" or "WordOfLife"
	  */
	case class CamelCase(capitalized: Boolean) extends NamingConvention
	{
		// IMPLEMENTED  ------------------------
		
		override def accepts(name: String) = {
			if (underscoreRegex.existsIn(name) || Regex.whiteSpace.existsIn(name))
				false
			else
				name.headOption.exists { c => !c.isLetter || c.isUpper == capitalized }
		}
		
		override def convert(string: String, originalStyle: NamingConvention) = originalStyle match {
			case UnderScore => fromParts(underscoreRegex.split(string))
			case Text(_, _) => fromParts(Regex.whiteSpace.split(string))
			case CamelCase(wasCapitalized) =>
				if (wasCapitalized == capitalized)
					string
				else if (capitalized)
					string.capitalize
				else
					string.uncapitalize
		}
		
		override def combine(beginning: String, end: String) = beginning + end.capitalize
		
		
		// OTHER    -----------------------------
		
		private def fromParts(parts: Seq[String]) = {
			if (capitalized)
				parts.map { _.capitalize }.mkString
			else if (parts.size <= 1)
				parts.head
			else
				(parts.head +: parts.tail.map { _.capitalize }).mkString
		}
	}
	
	/**
	  * Naming convention where words are separated by a underscore character '_'. E.g. "word_of_life"
	  */
	case object UnderScore extends NamingConvention
	{
		// This naming convention doesn't support uppercase characters, nor whitespaces
		override def accepts(name: String) =
			!Regex.whiteSpace.existsIn(name) && name.forall { c => !c.isLetter || !c.isUpper }
		
		override def convert(string: String, originalStyle: NamingConvention) = originalStyle match {
			case CamelCase(_) => camelParts(string).mkString("_")
			case Text(_, _) => Regex.whiteSpace.split(string).map { _.toLowerCase }.mkString("_")
			case _ => string
		}
		
		override def combine(beginning: String, end: String) = beginning + "_" + end
	}
	
	object Text
	{
		/**
		  * Naming convention that uses only lower case words. E.g. "word of life"
		  */
		val lower = apply(capitalizeFirst = false, capitalizeMore = false)
		/**
		  * Naming convention that capitalizes the first word in a sequence. E.g. "Word of life"
		  */
		val allCapitalized = apply(capitalizeFirst = true, capitalizeMore = true)
		/**
		  * Naming convention that capitalizes all words. E.g. "Word Of Life"
		  */
		val firstCapitalized = apply(capitalizeFirst = true, capitalizeMore = false)
	}
	/**
	  * Naming convention that displays words as they are read within documentation or text. E.g. "word of life" or
	  * "Word Of Life"
	  * @param capitalizeFirst Whether the first word should be capitalized
	  * @param capitalizeMore Whether other words should be capitalized
	  */
	case class Text(capitalizeFirst: Boolean, capitalizeMore: Boolean) extends NamingConvention
	{
		// IMPLEMENTED  --------------------------
		
		override def accepts(name: String) = {
			if (name.isEmpty)
				true
			else if (underscoreRegex.existsIn(name))
				false
			else {
				// Checks capitalization
				if (name.head.isLetter && name.head.isUpper != capitalizeFirst)
					false
				else {
					// Makes sure upper case letters are preceded by either upper case letters or whitespaces
					name.indices.tail.forall { index =>
						val c = name(index)
						if (c.isLower)
							true
						else {
							val prev = name(index - 1)
							if (capitalizeMore)
								prev.isUpper
							else
								prev == ' ' || prev.isUpper
						}
					}
				}
			}
		}
		
		override def convert(string: String, originalStyle: NamingConvention) = originalStyle match {
			case CamelCase(_) => fromParts(camelParts(string))
			case UnderScore => fromParts(underscoreRegex.split(string))
			case Text(first, more) =>
				if (first == capitalizeFirst && more == capitalizeMore)
					string
				else if (more == capitalizeMore) {
					if (capitalizeFirst)
						string.capitalize
					else
						string.uncapitalize
				}
				else
					fromParts(Regex.whiteSpace.split(string))
		}
		
		override def combine(beginning: String, end: String) = {
			val casedEnd = if (capitalizeMore) end.capitalize else end.uncapitalize
			beginning + " " + casedEnd
		}
		
		
		// OTHER    -----------------------
		
		private def fromParts(parts: Seq[String]) = {
			val casedParts = {
				if (capitalizeMore)
					parts.map { _.capitalize }
				else if (capitalizeFirst) {
					if (parts.size > 1)
						parts.head.capitalize +: parts.tail
					else
						parts.map { _.capitalize }
				}
				else
					parts
			}
			casedParts.mkString(" ")
		}
	}
}
