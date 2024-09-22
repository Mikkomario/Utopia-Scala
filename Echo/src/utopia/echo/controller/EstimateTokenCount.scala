package utopia.echo.controller

import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._

/**
  * A simplistic interface for estimating the number of tokens within strings.
  * This logic assumes that each word matches about 1-3 tokens, where each token is at least 3 characters long.
  * @author Mikko Hilpinen
  * @since 19.09.2024, v1.1
  */
object EstimateTokenCount
{
	// ATTRIBUTES   ------------------------
	
	private val specialCharSeqRegex = (!Regex.letterOrDigit).withinParentheses.oneOrMoreTimes
	
	
	// OTHER    ----------------------------
	
	/**
	  * Estimates the number of tokens within some text
	  * @param text A string
	  * @return Estimated number of tokens within that text
	  */
	// Counts the number of words
	def in(text: String) = text.splitIterator(Regex.whiteSpace)
		.map { word =>
			// Each word may consist of multiple parts (e.g. "it's" or "e-commerce")
			word.trim.splitIterator(specialCharSeqRegex)
				.map { part =>
					// The first 4 letters count as a single token
					// After that, every 3 letters count as a token
					if (part.length >= 9)
						(part.length / 3) min 5
					else if (part.length > 4)
						2
					else
						1
				}
				.sum
		}
		.sum
}
