package utopia.vault.coder.model.scala.code

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.operator.Combinable
import utopia.flow.parse.Regex
import utopia.flow.util.CollectionExtensions._
import utopia.vault.coder.model.scala.code.CodeLine.{commaRegex, functionalSplitRegex, maxLineLength, tabWidth, withRegex}

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
	/**
	  * A regular expression that searches for ".xxx {" function calls like .map { and so on.
	  */
	val functionalSplitRegex = Regex.escape('.') + Regex.alpha.oneOrMoreTimes +
		Regex.whiteSpace.noneOrOnce + Regex.escape('{')
	/**
	  * A regular expression that searches for commas
	  */
	val commaRegex = Regex.escape(',') + Regex.whiteSpace.noneOrOnce
	/**
	  * A regular expression for searching "with " -text
	  */
	val withRegex = Regex("with ")
	
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
case class CodeLine(indentation: Int, code: String) extends Combinable[CodeLine, String]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * The length of this line in characters
	  */
	lazy val length = indentation * tabWidth + code.length
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return Whether this code line is empty
	  */
	def isEmpty = code.isEmpty
	/**
	  * @return Whether this code line is not empty
	  */
	def nonEmpty = !isEmpty
	
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
		else if (code.contains("with "))
			splitWith(withRegex)
		// Case: Should be split => attempts comma -based splitting
		// and if that doesn't work, function based splitting (once)
		else if (code.contains(','))
			splitWith(commaRegex, splitAfterRegex = true)
		else
			functionalSplitRegex.startIndexIteratorIn(code).nextOption() match
			{
				case Some(splitIndex) =>
					Vector(
						copy(code = code.substring(0, splitIndex)),
						CodeLine(indentation + 1, code.substring(splitIndex))
					)
				case None => Vector(this)
			}
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def +(other: String) = copy(code = code + other)
	
	override def toString = ("\t" * indentation) + code
	
	
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
	def prepend(prefix: String) = copy(code = prefix + code)
	
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
