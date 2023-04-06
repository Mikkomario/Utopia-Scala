package utopia.vault.coder.model.scala.code

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.{Combinable, MaybeEmpty}
import utopia.flow.parse.string.Regex
import utopia.flow.collection.CollectionExtensions._
import utopia.vault.coder.model.scala.code.CodeLine.{maxLineLength, oneTimeRegexes, repeatableRegexes, tabWidth}

object CodeLine
{
	// ATTRIBUTES   ---------------------------------
	
	/**
	  * Recommended maximum line length
	  */
	val maxLineLength = 110
	/**
	  * How many characters (spaces) a single tab represents
	  */
	val tabWidth = 4
	
	private lazy val repeatableRegexes = Vector(
		Regex("with ") -> false,
		(Regex.escape(',') + Regex.whiteSpace.noneOrOnce).withinParenthesis.ignoringQuotations -> true,
		Regex("s").noneOrOnce + Regex.escape('\"') + (!Regex.escape('\"')).anyTimes +
			Regex.escape('\"') -> false,
		(Regex.anyOf("+-*/").oneOrMoreTimes + Regex.whiteSpace) -> true,
		(Regex.whiteSpace + Regex.word + Regex.whiteSpace) +
			!(Regex.escape('=') + Regex.escape('>')).withinParenthesis -> false
	)
	private lazy val oneTimeRegexes = Vector(
		Regex.escape('.') + Regex.alpha.oneOrMoreTimes + Regex.whiteSpace.noneOrOnce + Regex.escape('{'),
		Regex.escape('.') + Regex.alpha.oneOrMoreTimes
	)
	
	/**
	  * An empty code line
	  */
	val empty = indentedEmpty(0)
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param indentation Indentation level
	  * @return An empty line with that indentation
	  */
	def indentedEmpty(indentation: Int) = apply(indentation, "")
	
	/**
	  * @param code A line of code
	  * @return That line as a code line (without indentation)
	  */
	def apply(code: String): CodeLine = CodeLine(0, code)
}

/**
  * Represents a line of code. Handles indentation and supports splitting.
  * @author Mikko Hilpinen
  * @since 27.9.2021, v1.1
  */
case class CodeLine(indentation: Int, code: String) extends Combinable[String, CodeLine] with MaybeEmpty[CodeLine]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * The length of this line in characters
	  */
	lazy val length = indentation * tabWidth + code.length
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Whether this code line exceeds the maximum code line length
	  */
	def isTooLong = length > maxLineLength
	
	/**
	  * @return A copy of this line indented one time
	  */
	def indented = copy(indentation = indentation + 1)
	
	/**
	  * @return This code line split into possibly multiple lines (attempting to keep within maximum length)
	  */
	def split =
	{
		// Case: Already of the correct length
		if (length <= maxLineLength)
			Vector(this)
		// Case: Would benefit from splitting
		else
		{
			// Attempts to use repeatable splitters first
			repeatableRegexes.find { _._1.existsIn(code) } match {
				// Case: Repeatable splitter found => uses that
				case Some((regex, splitAfter)) => splitWith(regex, splitAfter)
				// Case: No repeatable splitter found => uses a one-time splitter
				case None =>
					oneTimeRegexes.findMap { _.startIndexIteratorIn(code).nextOption() } match {
						// Case: Splitter found => splits with that one
						case Some(splitIndex) =>
							Vector(
								copy(code = code.substring(0, splitIndex)),
								CodeLine(indentation + 1, code.substring(splitIndex))
							)
						// Case: No splitter found => Keeps as is
						case None => Vector(this)
					}
			}
		}
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	/**
	  * @return Whether this code line is empty
	  */
	override def isEmpty = code.isEmpty
	
	override def +(other: String) = copy(code = s"$code$other")
	
	override def toString = s"${ "\t" * indentation }$code"
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param amount Amount of indentation (count) to apply
	  * @return An indented copy of this code line
	  */
	def multiIndented(amount: Int) = copy(indentation = indentation + amount)
	
	/**
	  * @param prefix A prefix to add to the beginning of this line
	  * @return A copy of this line with the prefix added
	  */
	def prepend(prefix: String) = copy(code = s"$prefix$code")
	
	/**
	  * @param code New code to assign
	  * @return A copy of this line with the specified code
	  */
	def withCode(code: String) = copy(code = code)
	/**
	  * @param f A mapping function for the code part
	  * @return A mapped version of this code line (indentation is kept as is)
	  */
	def mapCode(f: String => String) = copy(code = f(code))
	
	// Split after regex determines whether the splitting part should be included on the original line
	private def splitWith(regex: Regex, splitAfterRegex: Boolean = false) =
	{
		// Finds the possible split locations
		val possibleSplitIndices = regex.rangesFrom(code).map { range =>
			if (splitAfterRegex)
				range.start + range.length
			else
				range.start
		}
		// Calculates where the first split should occur
		val firstLineTabWidth = indentation * tabWidth
		val firstLineSplitIndexIndex =
			possibleSplitIndices.lastIndexWhereOption { firstLineTabWidth + _ <= maxLineLength }.getOrElse(0)
		val firstLineSplitIndex = possibleSplitIndices(firstLineSplitIndexIndex)
		// Finds the remaining splits, taking into account the increased indentation (recursive)
		val nextLineSplitIndices = _split(possibleSplitIndices.drop(firstLineSplitIndexIndex + 1),
			firstLineSplitIndex, maxLineLength - firstLineTabWidth - tabWidth, code.length)
		// Combines the split lines
		val firstLine = code.substring(0, firstLineSplitIndex)
		val moreLines = (firstLineSplitIndex +: nextLineSplitIndices).paired
			.map { case Pair(start, end) => code.substring(start, end) }
		copy(code = firstLine) +: moreLines.map { code => CodeLine(indentation + 1, code) }
	}
	
	private def _split(remainingSplitIndices: Vector[Int], lastSplitIndex: Int, maxLineLength: Int,
	                   totalLength: Int): Vector[Int] =
	{
		// Checks whether should terminate
		if (remainingSplitIndices.isEmpty || totalLength - lastSplitIndex <= maxLineLength)
			Vector(totalLength)
		else
		{
			// Finds the last split index which fits to the line
			val nextSplitIndexIndex = remainingSplitIndices
				.lastIndexWhereOption { index => index - lastSplitIndex <= maxLineLength }.getOrElse(0)
			// Splits the remaining portion also
			val nextSplitIndex = remainingSplitIndices(nextSplitIndexIndex)
			nextSplitIndex +:
				_split(remainingSplitIndices.drop(nextSplitIndexIndex + 1), nextSplitIndex, maxLineLength, totalLength)
		}
	}
}
