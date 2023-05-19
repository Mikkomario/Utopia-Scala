package utopia.coder.model.enumeration

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._

import scala.collection.immutable.VectorBuilder

/**
  * An enumeration for different styles of naming properties and classes, etc.
  * @author Mikko Hilpinen
  * @since 3.2.2022, v1.4.1
  */
sealed trait NamingConvention
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A separator placed between different parts of the names in this naming convention
	  */
	def separator: String
	
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
	
	/**
	  * @param string A string to split into parts. Expected to be of this naming convention.
	  * @return Parts of that string, according to this naming convention.
	  */
	def split(string: String): Seq[String]
	
	
	// OTHER    ----------------------------
	
	/**
	  * Converts a string to this naming convention
	  * @param string A string to convert
	  * @return Version of the specified string in this naming convention
	  */
	def convert(string: String): String = convert(string, NamingConvention.of(string, this))
}

object NamingConvention
{
	// ATTRIBUTES   ------------------------
	
	private lazy val upperCaseRangeRegex = Regex.upperCaseLetter.oneOrMoreTimes
	private lazy val digitRangeRegex = Regex.digit.oneOrMoreTimes
	private lazy val underscoreRegex = Regex.escape('_')
	private lazy val hyphenRegex = Regex.escape('-')
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param string A string
	  * @return A guess of that string's naming convention
	  */
	def of(string: String) = {
		lazy val containsUnderScore = underscoreRegex.existsIn(string)
		lazy val containsHyphen = hyphenRegex.existsIn(string)
		
		// Hyphen & underscore are mutually exclusive, resulting in a text type when combined
		if (Regex.whiteSpace.existsIn(string) || (containsHyphen && containsUnderScore)) {
			if (string.view.drop(1).exists { _.isUpper })
				Text.allCapitalized
			else if (string.head.isUpper)
				Text.firstCapitalized
			else
				Text.lower
		}
		else if (containsHyphen)
			Hyphenated
		else if (containsUnderScore)
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
			case "pascal" | "pascalcase" => Some(CamelCase.capitalized)
			case "camel" | "camelcase" => Some(if (isCapitalized) CamelCase.capitalized else CamelCase.lower)
			case "under" | "underscore" | "_" => Some(UnderScore)
			case "hyphen" | "dash" | "hyphenated" | "kebab" | "-" => Some(Hyphenated)
			case "text" | "doc" => Some(if (isCapitalized) Text.allCapitalized else Text.lower)
			case _ => None
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
		
		override def separator = ""
		
		override def accepts(name: String) = {
			if (underscoreRegex.existsIn(name) || Regex.whiteSpace.existsIn(name) || hyphenRegex.existsIn(name))
				false
			else
				name.headOption.exists { c => !c.isLetter || c.isUpper == capitalized }
		}
		
		override def convert(string: String, originalStyle: NamingConvention) =
		{
			lazy val parts = originalStyle.split(string)
			originalStyle match {
				case UnderScore | Hyphenated => fromParts(parts)
				case Text(firstCapitalized, _) =>
					val base = fromParts(parts)
					if (firstCapitalized == capitalized)
						base
					else if (capitalized)
						base.capitalize
					else
						base.uncapitalize
				case CamelCase(wasCapitalized) =>
					if (wasCapitalized == capitalized)
						string
					else if (capitalized)
						string.capitalize
					else
						string.uncapitalize
			}
		}
		
		override def combine(beginning: String, end: String) = beginning + end.capitalize
		
		override def split(camelName: String) =
		{
			// Performs the default splitting based on upper- and lower-case letters
			val baseParts = {
				val upperCaseRanges = upperCaseRangeRegex.rangesFrom(camelName)
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
							Vector(camelName.slice(upperCaseRanges.head), camelName.drop(upperCaseRanges.head.size))
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
						if (appliedRanges.head.start > 0) {
							val firstPortion = camelName.substring(0, appliedRanges.head.start)
							if (firstPortion.length > 1)
								builder += firstPortion
							else
								builder += firstPortion.toLowerCase
						}
						// Adds the parts until the last uppercase sequence
						appliedRanges.paired.foreach { case Pair(upperStart, nextUpper) =>
							// Case: There are multiple upper-case characters in sequence => separates that part
							if (upperStart.size > 1) {
								builder += camelName.slice(upperStart)
								builder += camelName.slice(upperStart.last + 1, nextUpper.start)
							}
							// Case: Only one upper-case character => combines it with the rest of the string
							else
								builder += camelName.slice(upperStart.start, nextUpper.start).uncapitalize
						}
						// Adds the last upper-case sequence and the remaining string
						val lastUpper = appliedRanges.last
						// Case: Last sequence is multiple characters => separate
						if (lastUpper.size > 1) {
							builder += camelName.slice(lastUpper)
							if (lastUpper.last < camelName.length - 1)
								builder += camelName.drop(lastUpper.last + 1)
						}
						// Case: Last sequence is a single character => combines with the rest of the string
						else
							builder += camelName.drop(lastUpper.start).uncapitalize
						
						builder.result()
					}
				}
			}
			// Separates digits from the other parts
			baseParts.flatMap { part =>
				val subParts = digitRangeRegex.divide(part)
				if (subParts.size == 1)
					Vector(part)
				// Exception: If the first part is a single letter, combines it with the digits (E.g. "v23" or "A4")
				else if (subParts.size == 2 && subParts(1).isRight && subParts.head.leftOption.exists { _.length == 1 })
					Vector(part)
				else
					subParts.map {
						case Left(s) => s
						case Right(s) => s
					}
			}
		}
		
		
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
		override def separator = "_"
		
		// This naming convention doesn't support uppercase characters, nor whitespaces
		override def accepts(name: String) =
			!Regex.whiteSpace.existsIn(name) && !hyphenRegex.existsIn(name) &&
				name.forall { c => !c.isLetter || !c.isUpper }
		
		override def convert(string: String, originalStyle: NamingConvention) =
			originalStyle match {
				case UnderScore => string
				case _ => originalStyle.split(string).map { _.toLowerCase }.mkString("_")
			}
		
		override def combine(beginning: String, end: String) = beginning + "_" + end
		
		override def split(string: String) = underscoreRegex.split(string)
	}
	
	case object Hyphenated extends NamingConvention
	{
		override def separator = "-"
		
		override def accepts(name: String) =
			!Regex.whiteSpace.existsIn(name) && !underscoreRegex.existsIn(name) &&
				name.forall { c => !c.isLetter || !c.isUpper }
		
		override def convert(string: String, originalStyle: NamingConvention) = originalStyle match {
			case Hyphenated => string
			case _ => originalStyle.split(string).map { _.toLowerCase }.mkString("-")
		}
		
		override def combine(beginning: String, end: String) = s"$beginning-$end"
		
		override def split(string: String) = hyphenRegex.split(string)
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
		
		override def separator = " "
		
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
		
		override def convert(string: String, originalStyle: NamingConvention) =
		{
			lazy val parts = originalStyle.split(string)
			originalStyle match {
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
						fromParts(parts)
				case _ => fromParts(parts)
			}
		}
		
		override def combine(beginning: String, end: String) = {
			val casedEnd = if (capitalizeMore) end.capitalize else end.uncapitalize
			beginning + " " + casedEnd
		}
		
		override def split(string: String) = Regex.whiteSpace.split(string)
		
		
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
					parts.map { _.toLowerCase }
			}
			casedParts.mkString(" ")
		}
	}
}
