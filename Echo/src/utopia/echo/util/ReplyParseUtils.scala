package utopia.echo.util

/**
  * Provides utility functions for parsing reply messages
  * @author Mikko Hilpinen
  * @since 15.07.2025, v1.3.1
  */
object ReplyParseUtils
{
	// ATTRIBUTES   ------------------
	
	private lazy val thinkBlockStart = "<think>"
	private lazy val thinkBlockEnd = "</think>"
	
	
	// OTHER    ----------------------
	
	/**
	  * Separates the contents of the `<think>` block from the rest of the text, if present
	  * @param fullText Full text content
	  * @return Text content without the think element, and the contents of the think element, which may be empty.
	  */
	// TODO: This method is likely no longer needed, now that Ollama changed its interface
	def separateThinkFrom(fullText: String) = {
		val startIndex = fullText.indexOf(thinkBlockStart)
		// Case: No (complete) <think> block present => Checks whether a partial block may be found
		if (startIndex < 0) {
			val endIndex = fullText.indexOf(thinkBlockEnd)
			if (endIndex < 0)
				fullText -> ""
			else {
				val think = fullText.take(endIndex).trim
				val text = fullText.drop(endIndex + thinkBlockEnd.length).trim
				text -> think
			}
		}
		// Case: <think> block found => Separates its contents from the rest of the answer
		else {
			val firstThinkIndex = startIndex + thinkBlockStart.length
			val endIndex = fullText.indexOf(thinkBlockEnd, firstThinkIndex)
			
			val text = fullText.drop(endIndex + thinkBlockEnd.length).trim
			val thoughts = fullText.slice(firstThinkIndex, endIndex).trim
			
			// Case: Non-empty <think> block => Returns its contents, also
			if (thoughts.exists { _.isLetterOrDigit })
				text -> thoughts
			// Case: <think> was practically empty => Won't include its contents
			else
				text -> ""
		}
	}
}
